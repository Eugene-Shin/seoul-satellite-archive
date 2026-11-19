package com.all4land.seoulsatellitearchive.collect;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

// Earth Search, USGS. POST /search에 영역(intersects)과 촬영 일시(datetime)로 찾고 rel=next 링크를 따라간다
@Component
public class StacProviderClient implements ProviderClient {

    private static final int PAGE_SIZE = 100;

    private final RestClient restClient;

    @Autowired
    public StacProviderClient() {
        this(RestClient.builder().requestFactory(ProviderRequests.timeoutRequestFactory()));
    }

    StacProviderClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    @Override
    public boolean search(
            String searchUrl, String collection, JsonNode aoi, Instant from, Instant to, PageHandler pageHandler) {
        ObjectNode body = JsonNodeFactory.instance.objectNode();
        body.putArray("collections").add(collection);
        body.set("intersects", aoi);
        body.put("datetime", from + "/" + to);
        body.put("limit", PAGE_SIZE);

        HttpMethod method = HttpMethod.POST;
        String uri = searchUrl + "/search";
        while (true) {
            JsonNode page = exchange(method, uri, body);
            List<JsonNode> items = new ArrayList<>();
            page.path("features").forEach(items::add);
            boolean proceed = pageHandler.handle(items);

            JsonNode next = nextLink(page);
            if (next == null) {
                return true;
            }
            if (!proceed) {
                return false;
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
        return ProviderRequests.withRetry(() -> {
            RestClient.RequestBodySpec request = restClient.method(method).uri(URI.create(uri));
            if (body != null) {
                request.contentType(MediaType.APPLICATION_JSON).body(body);
            }
            return request.retrieve().body(JsonNode.class);
        });
    }
}
