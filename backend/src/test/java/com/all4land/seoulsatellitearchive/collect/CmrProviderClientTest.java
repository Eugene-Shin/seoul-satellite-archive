package com.all4land.seoulsatellitearchive.collect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.test.web.client.response.DefaultResponseCreator;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class CmrProviderClientTest {

    private static final String SEARCH_URL = "https://cmr.test/search";
    private static final String GRANULES = SEARCH_URL + "/granules.umm_json";
    private static final String HLS_L30 = "C2021957657-LPCLOUD";
    // 시계 방향 고리. CMR에는 반시계 방향으로 보내야 한다
    private static final JsonNode AOI = new JsonMapper().readTree("""
            {"type":"MultiPolygon","coordinates":[[[[126.9,37.5],[126.9,37.6],[127.1,37.6],[127.1,37.5],[126.9,37.5]]]]}
            """);
    private static final Instant FROM = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-10-08T00:00:00Z");

    private MockRestServiceServer server;
    private CmrProviderClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new CmrProviderClient(builder);
    }

    @Test
    void searchesByFormWithCounterClockwisePolygonAndAcquisitionWindow() {
        server.expect(requestTo(GRANULES))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content()
                        .formDataContains(Map.of(
                                "collection_concept_id", HLS_L30,
                                "polygon[]", "126.9,37.5,127.1,37.5,127.1,37.6,126.9,37.6,126.9,37.5",
                                "options[polygon][or]", "true",
                                "temporal", "2026-10-01T00:00:00Z,2026-10-08T00:00:00Z",
                                "page_size", "100")))
                .andExpect(headerDoesNotExist("CMR-Search-After"))
                .andRespond(page(1, null));

        assertThat(search()).hasSize(1);
        server.verify();
    }

    // 꽉 찬 페이지면 응답 헤더 CMR-Search-After를 다음 요청 헤더로 보낸다
    @Test
    void followsSearchAfterHeader() {
        server.expect(requestTo(GRANULES)).andRespond(page(CmrProviderClient.PAGE_SIZE, "[\"lpcloud\",1,2]"));
        server.expect(requestTo(GRANULES))
                .andExpect(header("CMR-Search-After", "[\"lpcloud\",1,2]"))
                .andRespond(page(3, "[\"lpcloud\",3,4]"));

        assertThat(search()).hasSize(CmrProviderClient.PAGE_SIZE + 3);
        server.verify();
    }

    @Test
    void retriesUnavailableServer() {
        server.expect(requestTo(GRANULES))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).header("Retry-After", "0"));
        server.expect(requestTo(GRANULES)).andRespond(page(1, null));

        assertThat(search()).hasSize(1);
        server.verify();
    }

    @Test
    void stopsRequestingWhenHandlerDeclinesNextPage() {
        server.expect(requestTo(GRANULES)).andRespond(page(CmrProviderClient.PAGE_SIZE, "[\"lpcloud\",1,2]"));

        assertThat(client.search(SEARCH_URL, HLS_L30, AOI, FROM, TO, items -> false))
                .isFalse();
        server.verify();
    }

    private List<JsonNode> search() {
        List<JsonNode> items = new ArrayList<>();
        client.search(SEARCH_URL, HLS_L30, AOI, FROM, TO, page -> {
            items.addAll(page);
            return true;
        });
        return items;
    }

    private static ResponseCreator page(int size, String searchAfter) {
        String items = IntStream.range(0, size)
                .mapToObj(i -> "{\"meta\":{\"concept-id\":\"G" + i + "\"}}")
                .collect(Collectors.joining(","));
        DefaultResponseCreator response =
                withSuccess("{\"hits\":0,\"items\":[" + items + "]}", MediaType.APPLICATION_JSON);
        return searchAfter == null ? response : response.header("CMR-Search-After", searchAfter);
    }
}
