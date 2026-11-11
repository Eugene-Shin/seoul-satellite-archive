package com.all4land.seoulsatellitearchive.collect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class StacClientTest {

    private static final String SEARCH_URL = "https://stac.test/v1";
    private static final JsonNode AOI = new JsonMapper()
            .readTree("{\"type\":\"Polygon\",\"coordinates\":[[[126.9,37.5],[127,37.5],[127,37.6],[126.9,37.5]]]}");
    private static final Instant FROM = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-10-08T00:00:00Z");

    private MockRestServiceServer server;
    private StacClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new StacClient(builder);
    }

    @Test
    void searchesByCollectionAoiAndAcquisitionWindow() {
        server.expect(requestTo(SEARCH_URL + "/search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"collections":["sentinel-2-c1-l2a"],
                         "intersects":{"type":"Polygon"},
                         "datetime":"2026-10-01T00:00:00Z/2026-10-08T00:00:00Z",
                         "limit":100}
                        """))
                .andRespond(page("a", null));

        assertThat(ids(search())).containsExactly("a");
        server.verify();
    }

    // Earth Search 형식: 다음 요청 본문 전체를 링크에 담아 준다
    @Test
    void followsPostNextLinkWithItsBody() {
        server.expect(requestTo(SEARCH_URL + "/search")).andRespond(page("a", """
                        {"rel":"next","href":"https://stac.test/v1/search","method":"POST","merge":false,
                         "body":{"collections":["sentinel-2-c1-l2a"],"limit":100,
                                 "next":"2026-10-23T02:27:12.176000Z,S2C_T52SCG_20261023T022443_L2A,sentinel-2-c1-l2a"}}
                        """));
        server.expect(requestTo(SEARCH_URL + "/search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"next":"2026-10-23T02:27:12.176000Z,S2C_T52SCG_20261023T022443_L2A,sentinel-2-c1-l2a"}
                        """))
                .andRespond(page("b", null));

        assertThat(ids(search())).containsExactly("a", "b");
        server.verify();
    }

    @Test
    void mergesNextBodyIntoPreviousRequest() {
        server.expect(requestTo(SEARCH_URL + "/search")).andRespond(page("a", """
                        {"rel":"next","href":"https://stac.test/v1/search","method":"POST",
                         "merge":true,"body":{"next":"token-2"}}
                        """));
        server.expect(requestTo(SEARCH_URL + "/search"))
                .andExpect(
                        content().json("{\"collections\":[\"sentinel-2-c1-l2a\"],\"limit\":100,\"next\":\"token-2\"}"))
                .andRespond(page("b", null));

        assertThat(ids(search())).containsExactly("a", "b");
        server.verify();
    }

    @Test
    void followsGetNextLink() {
        server.expect(requestTo(SEARCH_URL + "/search"))
                .andRespond(page("a", "{\"rel\":\"next\",\"href\":\"https://stac.test/v1/search?token=2\"}"));
        server.expect(requestTo(SEARCH_URL + "/search?token=2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(page("b", null));

        assertThat(ids(search())).containsExactly("a", "b");
        server.verify();
    }

    @Test
    void retriesUnavailableServer() {
        server.expect(requestTo(SEARCH_URL + "/search")).andRespond(unavailable());
        server.expect(requestTo(SEARCH_URL + "/search")).andRespond(page("a", null));

        assertThat(ids(search())).containsExactly("a");
        server.verify();
    }

    @Test
    void givesUpAfterRetries() {
        server.expect(times(4), requestTo(SEARCH_URL + "/search")).andRespond(unavailable());

        assertThatThrownBy(this::search).isInstanceOf(HttpServerErrorException.class);
        server.verify();
    }

    @Test
    void doesNotRetryClientError() {
        server.expect(requestTo(SEARCH_URL + "/search")).andRespond(withBadRequest());

        assertThatThrownBy(this::search).isInstanceOf(HttpClientErrorException.class);
        server.verify();
    }

    private List<JsonNode> search() {
        return client.search(SEARCH_URL, "sentinel-2-c1-l2a", AOI, FROM, TO);
    }

    private static List<String> ids(List<JsonNode> items) {
        return items.stream().map(item -> item.path("id").asString()).toList();
    }

    private static ResponseCreator page(String id, String nextLink) {
        String links = nextLink == null ? "" : nextLink;
        return withSuccess("""
                {"type":"FeatureCollection","features":[{"id":"%s"}],"links":[%s]}
                """.formatted(id, links), MediaType.parseMediaType("application/geo+json"));
    }

    // 대기 0초로 재시도해서 테스트가 기다리지 않게 한다
    private static ResponseCreator unavailable() {
        return withStatus(HttpStatus.SERVICE_UNAVAILABLE).header("Retry-After", "0");
    }
}
