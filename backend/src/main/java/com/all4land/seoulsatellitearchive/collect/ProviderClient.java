package com.all4land.seoulsatellitearchive.collect;

import java.time.Instant;
import java.util.List;
import tools.jackson.databind.JsonNode;

// 출처의 메타데이터 검색. 검색 방식(provider.metadata_search_api)마다 구현이 하나다
public interface ProviderClient {

    @FunctionalInterface
    interface PageHandler {
        // 페이지의 장면을 처리하고, 다음 페이지를 계속 받을지 돌려준다
        boolean handle(List<JsonNode> items);
    }

    // 촬영 일시가 [from, to]이고 영역(GeoJSON MultiPolygon)과 겹치는 장면을 페이지마다 넘긴다. 마지막 페이지까지 받았으면 true
    boolean search(
            String searchUrl, String collection, JsonNode aoi, Instant from, Instant to, PageHandler pageHandler);
}
