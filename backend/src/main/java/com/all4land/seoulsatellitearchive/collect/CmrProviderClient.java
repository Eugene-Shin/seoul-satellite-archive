package com.all4land.seoulsatellitearchive.collect;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

// NASA CMR 그래뉼 검색(UMM-G). 서울 경계처럼 점이 많은 영역은 주소에 넣기 길어서 조건을 POST 본문(form)으로 보낸다.
// 다음 페이지는 응답 헤더 CMR-Search-After를 다음 요청 헤더로 넘겨 받는다
@Component
public class CmrProviderClient implements ProviderClient {

    static final int PAGE_SIZE = 100;
    private static final String SEARCH_AFTER = "CMR-Search-After";

    private final RestClient restClient;

    @Autowired
    public CmrProviderClient() {
        this(RestClient.builder().requestFactory(ProviderRequests.timeoutRequestFactory()));
    }

    CmrProviderClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    @Override
    public boolean search(
            String searchUrl, String collection, JsonNode aoi, Instant from, Instant to, PageHandler pageHandler) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("collection_concept_id", collection);
        for (JsonNode polygon : aoi.path("coordinates")) {
            form.add("polygon[]", counterClockwise(polygon.path(0)));
        }
        form.add("options[polygon][or]", "true");
        form.add("temporal", from + "," + to);
        form.add("page_size", String.valueOf(PAGE_SIZE));

        String searchAfter = null;
        while (true) {
            ResponseEntity<JsonNode> response = exchange(searchUrl + "/granules.umm_json", form, searchAfter);
            List<JsonNode> items = new ArrayList<>();
            response.getBody().path("items").forEach(items::add);
            boolean proceed = pageHandler.handle(items);

            searchAfter = response.getHeaders().getFirst(SEARCH_AFTER);
            if (items.size() < PAGE_SIZE || searchAfter == null) {
                return true;
            }
            if (!proceed) {
                return false;
            }
        }
    }

    // CMR은 바깥 경계를 반시계 방향 "경도,위도,..."로 받는다. 구멍은 보내지 않는다(영역이 조금 넓어질 뿐이다)
    static String counterClockwise(JsonNode ring) {
        List<JsonNode> points = new ArrayList<>();
        ring.forEach(points::add);
        double twiceArea = 0;
        for (int i = 0; i + 1 < points.size(); i++) {
            twiceArea += points.get(i).get(0).asDouble()
                            * points.get(i + 1).get(1).asDouble()
                    - points.get(i + 1).get(0).asDouble() * points.get(i).get(1).asDouble();
        }
        if (twiceArea < 0) {
            points = points.reversed();
        }
        StringJoiner joiner = new StringJoiner(",");
        for (JsonNode point : points) {
            joiner.add(point.get(0).asString()).add(point.get(1).asString());
        }
        return joiner.toString();
    }

    private ResponseEntity<JsonNode> exchange(String uri, MultiValueMap<String, String> form, String searchAfter) {
        return ProviderRequests.withRetry(() -> restClient
                .post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .headers(headers -> {
                    if (searchAfter != null) {
                        headers.set(SEARCH_AFTER, searchAfter);
                    }
                })
                .body(form)
                .retrieve()
                .toEntity(JsonNode.class));
    }
}
