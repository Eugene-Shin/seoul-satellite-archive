package com.all4land.seoulsatellitearchive.collect;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

@Component
public class StacClient {

    private static final int PAGE_SIZE = 100;
    private static final int MAX_RETRIES = 3;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(60);

    private final RestClient restClient;

    @Autowired
    public StacClient() {
        this(RestClient.builder().requestFactory(timeoutRequestFactory()));
    }

    StacClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    // 촬영 일시가 [from, to]인 영상을 다음 페이지가 없을 때까지 모은다
    public List<JsonNode> search(String searchUrl, String collection, JsonNode aoi, Instant from, Instant to) {
        ObjectNode body = JsonNodeFactory.instance.objectNode();
        body.putArray("collections").add(collection);
        body.set("intersects", aoi);
        body.put("datetime", from + "/" + to);
        body.put("limit", PAGE_SIZE);

        List<JsonNode> items = new ArrayList<>();
        HttpMethod method = HttpMethod.POST;
        String uri = searchUrl + "/search";
        while (true) {
            JsonNode page = exchange(method, uri, body);
            page.path("features").forEach(items::add);

            JsonNode next = nextLink(page);
            if (next == null) {
                return items;
            }
            uri = next.path("href").asString();
            method = HttpMethod.valueOf(next.path("method").asString("GET"));
            body = method == HttpMethod.POST ? nextBody(body, next) : null;
        }
    }

    private static JsonNode nextLink(JsonNode page) {
        for (JsonNode link : page.path("links")) {
            if ("next".equals(link.path("rel").asString())) {
                return link;
            }
        }
        return null;
    }

    // STAC 링크의 body는 그대로 보내거나(merge=false) 이전 요청 본문에 덮어쓴다(merge=true)
    private static ObjectNode nextBody(ObjectNode previous, JsonNode next) {
        if (!(next.get("body") instanceof ObjectNode linkBody)) {
            return previous;
        }
        if (!next.path("merge").asBoolean(false)) {
            return linkBody;
        }
        ObjectNode merged = previous.deepCopy();
        merged.setAll(linkBody);
        return merged;
    }

    private JsonNode exchange(HttpMethod method, String uri, ObjectNode body) {
        for (int attempt = 0; ; attempt++) {
            try {
                RestClient.RequestBodySpec request = restClient.method(method).uri(URI.create(uri));
                if (body != null) {
                    request.contentType(MediaType.APPLICATION_JSON).body(body);
                }
                return request.retrieve().body(JsonNode.class);
            } catch (HttpStatusCodeException e) {
                if (!isRetryable(e.getStatusCode()) || attempt == MAX_RETRIES) {
                    throw e;
                }
                sleep(retryAfter(e).orElse(backoff(attempt)));
            } catch (ResourceAccessException e) {
                if (attempt == MAX_RETRIES) {
                    throw e;
                }
                sleep(backoff(attempt));
            }
        }
    }

    private static boolean isRetryable(HttpStatusCode status) {
        return status.value() == 429 || status.is5xxServerError();
    }

    // 초 단위 값만 따른다. 날짜 형식이면 기본 대기 시간을 쓴다
    private static Optional<Duration> retryAfter(HttpStatusCodeException e) {
        String value =
                e.getResponseHeaders() == null ? null : e.getResponseHeaders().getFirst("Retry-After");
        if (value == null || !value.matches("\\d+")) {
            return Optional.empty();
        }
        return Optional.of(Duration.ofSeconds(Long.parseLong(value)));
    }

    private static Duration backoff(int attempt) {
        return Duration.ofSeconds(1L << attempt);
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("STAC 요청 재시도 대기 중 중단되었습니다.", e);
        }
    }

    private static SimpleClientHttpRequestFactory timeoutRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }
}
