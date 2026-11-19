package com.all4land.seoulsatellitearchive.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.all4land.seoulsatellitearchive.global.config.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
// 검색·표시 테스트는 이 fixture 영상(제목이 fx로 시작)만 keyword=fx로 골라 확인한다. 테스트가 끝나면 롤백된다
@Sql("/fixtures/search-images.sql")
class ImageControllerTest {

    private static final String BASE = "/api/images";
    // collectionId는 DB가 만드는 값이라 테스트마다 시드 컬렉션의 id로 채운다
    private static final String REQUEST_TEMPLATE = """
            {
              "collectionId": null, "sourceId": "admin-crud-test", "title": "관리자 CRUD 테스트",
              "satellite": " sentinel-2a ", "instruments": [" msi "], "sensorType": "OPTICAL",
              "acquiredAt": "2026-10-19T10:00:00+09:00", "acquiredEndAt": "2026-10-19T10:00:27+09:00",
              "cloudCover": 5.25,
              "orbitDirection": "ASCENDING",
              "footprint": {"type":"MultiPolygon","coordinates":[[[[126.9,37.5],[127,37.5],[127,37.6],[126.9,37.5]]]]},
              "url": "archive/metadata.json", "downloadUrls": ["s3://archive/image.tif", "https://example.test/image.tif"],
              "sourceUpdatedAt": "2026-10-19T01:00:00Z", "rawMetadata": {"nested":{"value":1},"tags":["a"]}
            }
            """;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    SatelliteImageRepository repository;

    @Autowired
    EntityManager entityManager;

    private String request;

    @BeforeEach
    void fillCollectionId() {
        request = JsonPath.parse(REQUEST_TEMPLATE)
                .set("$.collectionId", collectionId("earth-search", "sentinel-2-c1-l2a"))
                .jsonString();
    }

    private long collectionId(String provider, String code) {
        return jdbc.queryForObject(
                "select collection_id from image_collection where provider = ? and code = ?",
                Long.class,
                provider,
                code);
    }

    @Test
    void collections() throws Exception {
        mockMvc.perform(get("/api/collections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(11)))
                .andExpect(jsonPath("$.data[?(@.code == 'sentinel-2-c1-l2a')].providerCode", contains("earth-search")))
                .andExpect(jsonPath("$.data[?(@.code == 'landsat-c2l2-st')].providerCode", contains("usgs")))
                .andExpect(jsonPath("$.data[?(@.collectable == true)].code")
                        .value(containsInAnyOrder(
                                "sentinel-2-c1-l2a",
                                "landsat-c2l2-sr",
                                "landsat-c2l2-st",
                                "C2021957657-LPCLOUD",
                                "C2021957295-LPCLOUD",
                                "C3998139651-LPCLOUD",
                                "C1748058432-LPCLOUD",
                                "C1748046084-LPCLOUD",
                                "C1214470488-ASF",
                                "C3470873558-ASF",
                                "C4175278193-ASF")));
    }

    @Test
    void filterOptions() throws Exception {
        mockMvc.perform(get("/api/images/filter-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.satellites", hasItems("LANDSAT-8", "SENTINEL-1A", "SENTINEL-2A")))
                .andExpect(jsonPath("$.data.instruments", hasItems("C-SAR", "OLI", "MSI")))
                .andExpect(jsonPath("$.data.providers[*].code", containsInAnyOrder("earth-search", "nasa-cmr", "usgs")))
                .andExpect(jsonPath("$.data.sensorTypes", contains("OPTICAL", "SAR")));
    }

    @Test
    void collectionsMatchAnySelected() throws Exception {
        String s1Slc = String.valueOf(collectionId("nasa-cmr", "C1214470488-ASF"));
        String hls = String.valueOf(collectionId("nasa-cmr", "C2021957657-LPCLOUD"));
        mockMvc.perform(fixtures().param("collectionIds", s1Slc))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].title", contains("fx_s1")))
                .andExpect(jsonPath("$.data.content[*].collection.productType", everyItem(is("SLC"))));
        mockMvc.perform(fixtures().param("collectionIds", s1Slc, hls))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].title", containsInAnyOrder("fx_s1", "fx-hls")));
    }

    @Test
    void productTypesMatchAnySelected() throws Exception {
        mockMvc.perform(fixtures().param("productTypes", "SLC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].title", contains("fx_s1")));
        mockMvc.perform(fixtures().param("productTypes", "SLC", "L30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].title", containsInAnyOrder("fx_s1", "fx-hls")));
    }

    // 파일은 역할순, 밴드는 파일 안 순서. 밴드가 없는 파일도 빠지지 않고 빈 배열로 온다
    @Test
    void detailReturnsFilesWithBands() throws Exception {
        long fixtureId = imageId("fx-s2-low");
        String body = mockMvc.perform(get(BASE + "/" + fixtureId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.files[?(@.assetKey == 'visual')].role", contains("PREVIEW")))
                .andExpect(jsonPath("$.data.files[?(@.assetKey == 'visual')].pixelSpacingMeters", contains(10.00)))
                .andExpect(jsonPath("$.data.files[?(@.assetKey == 'visual')].bands[*].bandIndex", contains(1, 2, 3)))
                .andExpect(jsonPath(
                        "$.data.files[?(@.assetKey == 'visual')].bands[*].bandName", contains("red", "green", "blue")))
                .andExpect(jsonPath("$.data.files[?(@.assetKey == 'red')].bands[0].commonName", contains("red")))
                .andExpect(
                        jsonPath("$.data.files[?(@.assetKey == 'red')].bands[0].centerWavelengthUm", contains(0.665)))
                .andExpect(jsonPath("$.data.files[?(@.assetKey == 'red')].bands[0].bandwidthUm", contains(0.038)))
                .andExpect(jsonPath("$.data.files[?(@.assetKey == 'red')].bands[0].resolutionMeters", contains(10.00)))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        List<String> roles = JsonPath.read(body, "$.data.files[*].role");
        assertThat(roles).isSorted();
        List<List<?>> thumbnailBands = JsonPath.read(body, "$.data.files[?(@.assetKey == 'thumbnail')].bands");
        assertThat(thumbnailBands).containsExactly(List.of());
    }

    // 수집기가 만든 영상은 출처 값이 원본이라 수정을 막고, 삭제는 허용한다
    @Test
    void collectedImageCannotBeUpdatedButCanBeDeleted() throws Exception {
        // 컬렉션마다 대상이 하나라서 seed로 들어 있는 c1 서울 대상에 붙인다
        long collectedId = imageId("fx-s2-low");
        jdbc.update("""
                UPDATE satellite_image AS i
                SET collect_target_id = t.target_id
                FROM collect_target AS t
                WHERE t.collection_id = i.collection_id AND i.image_id = ?
                """, collectedId);

        mockMvc.perform(fixtures())
                .andExpect(jsonPath("$.data.content[?(@.imageId == " + collectedId + ")].collected", contains(true)))
                .andExpect(jsonPath(
                        "$.data.content[?(@.imageId != " + collectedId + ")].collected", everyItem(is(false))));
        mockMvc.perform(get(BASE + "/" + collectedId))
                .andExpect(jsonPath("$.data.collected").value(true));
        mockMvc.perform(json(put(BASE + "/" + collectedId), request)).andExpect(status().isConflict());
        // 요청마다 영속성 컨텍스트가 따로인 실제 서버처럼, 상세 조회가 올려 둔 파일 엔티티를 비운다
        entityManager.clear();
        mockMvc.perform(delete(BASE + "/" + collectedId)).andExpect(status().isOk());
    }

    @Test
    void detailReturnsFilesOfRegisteredImage() throws Exception {
        long id = create(request);
        entityManager.clear();
        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.data.files[*].href",
                        containsInAnyOrder("s3://archive/image.tif", "https://example.test/image.tif")))
                .andExpect(jsonPath("$.data.files[*].assetKey", everyItem(is("image.tif"))))
                .andExpect(jsonPath("$.data.files[*].role", everyItem(is("DATA"))))
                .andExpect(jsonPath("$.data.files[*].mediaType", everyItem(nullValue())))
                .andExpect(jsonPath("$.data.files[*].pixelSpacingMeters", everyItem(nullValue())))
                .andExpect(jsonPath("$.data.files[*].bands[*]", hasSize(0)));
    }

    // 검색 결과에서 SAR 영상만 촬영 기하(sar)를 갖고, 광학 영상은 null이다
    @Test
    void searchReturnsSarOnlyForSarImages() throws Exception {
        mockMvc.perform(fixtures())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.title == 'fx_s1')].sar.relativeOrbit", contains(127)))
                .andExpect(jsonPath("$.data.content[?(@.title == 'fx_s1')].sar.polarizations[0]", contains("VV")))
                .andExpect(jsonPath("$.data.content[?(@.title == 'fx-s2-low')].sar", contains(nullValue())));
    }

    @Test
    void sarIsCreatedReplacedAndDeletedWithRequest() throws Exception {
        long id = create(sarRequest());
        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sar.relativeOrbit").value(127))
                .andExpect(jsonPath("$.data.sar.frame").value(120))
                .andExpect(jsonPath("$.data.sar.instrumentMode").value("IW"))
                .andExpect(jsonPath("$.data.sar.polarizations", contains("VV", "VH")))
                .andExpect(jsonPath("$.data.sar.lookDirection").value("RIGHT"))
                .andExpect(jsonPath("$.data.sar.incidenceAngle").value(38.5))
                .andExpect(jsonPath("$.data.sar.resolutionRangeMeters").value(2.64))
                .andExpect(jsonPath("$.data.sar.resolutionAzimuthMeters").value(3.3))
                // 파일에 픽셀 간격이 없어서 거리·방위 해상도로 대체한다
                .andExpect(jsonPath("$.data.resolution.minMeters").value(2.64))
                .andExpect(jsonPath("$.data.resolution.maxMeters").value(3.3));

        String replaced = JsonPath.parse(sarRequest())
                .set("$.sar.relativeOrbit", 50)
                .delete("$.sar.frame")
                .jsonString();
        mockMvc.perform(json(put(BASE + "/" + id), replaced)).andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sar.relativeOrbit").value(50))
                .andExpect(jsonPath("$.data.sar.frame", nullValue()));

        String withoutSar = JsonPath.parse(sarRequest()).delete("$.sar").jsonString();
        mockMvc.perform(json(put(BASE + "/" + id), withoutSar)).andExpect(status().isOk());
        // 뷰 엔티티는 앞선 조회의 값이 영속성 컨텍스트에 남아 있다. 실제 요청처럼 비우고 조회한다
        entityManager.clear();
        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sar", nullValue()))
                .andExpect(jsonPath("$.data.resolution", nullValue()));
        assertThat(jdbc.queryForObject("select count(*) from image_sar where image_id = ?", Long.class, id))
                .isZero();
    }

    @Test
    void opticalImagesCannotHaveSar() throws Exception {
        String optical =
                JsonPath.parse(sarRequest()).set("$.sensorType", "OPTICAL").jsonString();
        mockMvc.perform(json(post(BASE), optical))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorDetail[0]").value("sarValid: SAR 영상만 sar를 가질 수 있습니다."));
    }

    @ParameterizedTest
    @MethodSource("invalidSarFields")
    void invalidSarReturns400WithoutWriting(String field, Object value) throws Exception {
        long count = jdbc.queryForObject("select count(*) from image_sar", Long.class);
        mockMvc.perform(json(
                        post(BASE),
                        JsonPath.parse(sarRequest())
                                .set("$.sar." + field, value)
                                .jsonString()))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select count(*) from image_sar", Long.class))
                .isEqualTo(count);
    }

    static Stream<Arguments> invalidSarFields() {
        return Stream.of(
                Arguments.of("relativeOrbit", 0),
                Arguments.of("frame", -1),
                Arguments.of("instrumentMode", "M".repeat(11)),
                Arguments.of("polarizations", List.of()),
                Arguments.of("polarizations", List.of("XX")),
                Arguments.of("polarizations", java.util.Arrays.asList("VV", null)),
                Arguments.of("lookDirection", "UP"),
                Arguments.of("incidenceAngle", 90.01),
                Arguments.of("incidenceAngle", -1),
                Arguments.of("resolutionRangeMeters", 0),
                Arguments.of("resolutionAzimuthMeters", 0.001));
    }

    private String sarRequest() {
        return JsonPath.parse(request)
                .set("$.sensorType", "SAR")
                .set("$.cloudCover", null)
                // set은 없는 키를 만들지 못한다
                .put(
                        "$",
                        "sar",
                        Map.of(
                                "relativeOrbit",
                                127,
                                "frame",
                                120,
                                "instrumentMode",
                                " iw ",
                                "polarizations",
                                List.of("VV", "VH"),
                                "lookDirection",
                                "RIGHT",
                                "incidenceAngle",
                                38.5,
                                "resolutionRangeMeters",
                                2.64,
                                "resolutionAzimuthMeters",
                                3.3))
                .jsonString();
    }

    @Test
    void instrumentsMatchAnySelected() throws Exception {
        mockMvc.perform(fixtures().param("instruments", "OLI", "C-SAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].title", containsInAnyOrder("fx-hls", "fx_s1")));
    }

    // '_'를 아무 글자로 해석하면 fx-로 시작하는 영상도 걸린다
    @Test
    void keywordTreatsUnderscoreLiterally() throws Exception {
        mockMvc.perform(get("/api/images").param("keyword", "fx_"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].title", contains("fx_s1")));
    }

    // 01-08(KST)은 00:30 KST(전날 15:30 UTC)부터 포함하고, 01-09 00:30 KST는 뺀다
    @Test
    void acquiredDateRangeCoversWholeKstDay() throws Exception {
        mockMvc.perform(fixtures().param("acquiredFrom", "2026-01-08").param("acquiredTo", "2026-01-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].title", containsInAnyOrder("fx-s2-low", "fx-s2-cloudy")));
    }

    @Test
    void cloudMaxExcludesSar() throws Exception {
        mockMvc.perform(fixtures().param("sensorTypes", "SAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].title", contains("fx_s1")));
        mockMvc.perform(fixtures().param("cloudMax", "100").param("sensorTypes", "SAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    @Test
    void bboxFiltersByFootprint() throws Exception {
        mockMvc.perform(fixtures().param("bbox", "128.9,35.0,129.2,35.3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].title", contains("fx-s2-busan")))
                .andExpect(jsonPath("$.data.content[0].footprint.type").value("MultiPolygon"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"128.9,35.0,129.2", "128.9,35.0,129.2,35.3,1"})
    void wrongBboxCountReturnsValidationError(String bbox) throws Exception {
        mockMvc.perform(get("/api/images").param("bbox", bbox))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorDetail", hasSize(1)))
                .andExpect(jsonPath("$.errorDetail[0]").value("bbox: minLon,minLat,maxLon,maxLat 4개 값이어야 합니다."));
    }

    @Test
    void emptyBboxValueReturnsValidationError() throws Exception {
        mockMvc.perform(get("/api/images").param("bbox", "128.9,,129.2,35.3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorDetail", hasSize(1)))
                .andExpect(jsonPath("$.errorDetail[0]").value("bbox: 빈 값이 있으면 안 됩니다."));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "NaN,35.0,129.2,35.3",
                "Infinity,35.0,129.2,35.3",
                "-181,35.0,129.2,35.3",
                "128.9,-91,129.2,35.3",
                "129.2,35.0,128.9,35.3",
                "128.9,35.3,129.2,35.0"
            })
    void invalidBboxRangeOrOrderReturnsValidationError(String bbox) throws Exception {
        mockMvc.perform(get("/api/images").param("bbox", bbox))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorDetail", hasSize(1)))
                .andExpect(jsonPath("$.errorDetail[0]").value("bbox: 경도는 -180~180, 위도는 -90~90, 최소값은 최대값 이하여야 합니다."));
    }

    @Test
    void pointBboxFindsCoveringImages() throws Exception {
        mockMvc.perform(fixtures().param("bbox", "129.0,35.1,129.0,35.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].title", contains("fx-s2-busan")));
    }

    @Test
    void cloudSortPutsNullsLast() throws Exception {
        mockMvc.perform(fixtures().param("sorts", "CLOUD_COVER_DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.data.content[*].title",
                        contains("fx-s2-cloudy", "fx-s2-busan", "fx-hls", "fx-s2-low", "fx_s1")));
    }

    // 출처 썸네일이 있으면 그 링크, 없으면(S1) null
    @Test
    void searchReturnsThumbnail() throws Exception {
        mockMvc.perform(fixtures())
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.data.content[?(@.title == 'fx-s2-low')].thumbnailUrl",
                        contains("https://fx.test/s2/preview.jpg")))
                .andExpect(jsonPath(
                        "$.data.content[?(@.title == 'fx-hls')].thumbnailUrl",
                        contains("https://fx.test/hls/fx-hls.jpg")))
                .andExpect(jsonPath("$.data.content[?(@.title == 'fx_s1')].thumbnailUrl", contains(nullValue())));
    }

    // 출처 표기 의무가 없는 컬렉션은 creditText가 없다
    @Test
    void collectionsReturnLicenseAndCredit() throws Exception {
        mockMvc.perform(get("/api/collections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code == 'sentinel-2-c1-l2a')].licenseName", contains("proprietary")))
                .andExpect(jsonPath(
                        "$.data[?(@.code == 'sentinel-2-c1-l2a')].licenseUrl",
                        contains("https://sentinels.copernicus.eu/documents/247904/690755/Sentinel_Data_Legal_Notice")))
                .andExpect(jsonPath(
                        "$.data[?(@.code == 'sentinel-2-c1-l2a')].creditText",
                        contains("Contains modified Copernicus Sentinel data")))
                .andExpect(jsonPath("$.data[?(@.code == 'C2021957657-LPCLOUD')].creditText", contains(nullValue())));
    }

    // 겹치기는 미리보기 COG(visual)가 있는 영상만. 미리보기 파일이 없거나(HLS) 묶음 파일 하나뿐이면(S1) null
    @Test
    void searchReturnsOverlay() throws Exception {
        mockMvc.perform(fixtures())
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.data.content[?(@.title == 'fx-s2-low')].overlayUrl", contains("https://fx.test/s2/TCI.tif")))
                .andExpect(jsonPath("$.data.content[?(@.title == 'fx-hls')].overlayUrl", contains(nullValue())))
                .andExpect(jsonPath("$.data.content[?(@.title == 'fx_s1')].overlayUrl", contains(nullValue())));
    }

    // 여러 개면 https 링크 중 가장 앞서는 것 하나
    @Test
    void searchPicksOneHttpsThumbnail() throws Exception {
        long id = create(request);
        for (String href : List.of(
                "https://example.test/b.jpg", "https://example.test/a.jpg", "http://example.test/insecure.jpg")) {
            jdbc.update(
                    "insert into image_file (image_id, asset_key, role, href) values (?, 'thumbnail', 'THUMBNAIL', ?)",
                    id,
                    href);
        }
        mockMvc.perform(get("/api/images").param("keyword", "관리자 CRUD 테스트"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].imageId").value(id))
                .andExpect(jsonPath("$.data.content[0].thumbnailUrl").value("https://example.test/a.jpg"));
    }

    // 해상도 범위가 겹치면 포함한다. fx-s2-low는 파일 픽셀 간격 10~60 m, fx-hls는 30 m. 해상도가 없는 영상은 빠진다
    @Test
    void resolutionRangeOverlapsImageResolution() throws Exception {
        mockMvc.perform(fixtures().param("resolutionMin", "40"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].title", contains("fx-s2-low")))
                .andExpect(jsonPath("$.data.content[0].resolution.minMeters").value(10.00))
                .andExpect(jsonPath("$.data.content[0].resolution.maxMeters").value(60.00));
        mockMvc.perform(fixtures().param("resolutionMax", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].title", contains("fx-s2-low")));
        mockMvc.perform(fixtures().param("resolutionMin", "25").param("resolutionMax", "35"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].title", containsInAnyOrder("fx-s2-low", "fx-hls")))
                .andExpect(jsonPath("$.data.content[?(@.title == 'fx-hls')].resolution.minMeters", contains(30.00)));
    }

    @Test
    void cursorContinuesUntilNull() throws Exception {
        String first = search(null, "2", "keyword", "fx");
        assertThat(ids(first)).hasSize(2);
        String second = search(nextCursor(first), "2", "keyword", "fx");
        assertThat(ids(second)).hasSize(2);
        String third = search(nextCursor(second), "2", "keyword", "fx");
        assertThat(ids(third)).hasSize(1);
        assertThat(nextCursor(third)).isNull();
    }

    // 작은 크기로 끝까지 이어 받은 결과가 한 번에 받은 결과와 순서까지 같아야 한다 (동점·NULL·방향 혼합 포함)
    @ParameterizedTest
    @ValueSource(
            strings = {
                "ACQUIRED_AT_DESC",
                "ACQUIRED_AT_ASC",
                "CLOUD_COVER_ASC",
                "CLOUD_COVER_ASC,ACQUIRED_AT_DESC",
                "CLOUD_COVER_DESC,ACQUIRED_AT_ASC"
            })
    void cursorWalkMatchesSingleRequest(String sorts) throws Exception {
        String all = search(null, "100", "sorts", sorts);
        assertThat(nextCursor(all)).isNull();

        List<Integer> walked = new ArrayList<>();
        String cursor = null;
        do {
            String page = search(cursor, "3", "sorts", sorts);
            walked.addAll(ids(page));
            cursor = nextCursor(page);
        } while (cursor != null);

        assertThat(walked).isEqualTo(ids(all));
    }

    @Test
    void invalidCursorReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/images").param("cursor", "not-a-cursor"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_001"));

        String cursorByDate = nextCursor(search(null, "1", "sorts", "ACQUIRED_AT_DESC"));
        mockMvc.perform(get("/api/images").param("cursor", cursorByDate).param("sorts", "CLOUD_COVER_ASC"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorDetail[0]").value("커서가 올바르지 않습니다."));
    }

    private String search(String cursor, String size, String name, String value) throws Exception {
        MockHttpServletRequestBuilder request =
                get("/api/images").param("size", size).param(name, value.split(","));
        if (cursor != null) {
            request.param("cursor", cursor);
        }
        return mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private static List<Integer> ids(String body) {
        return JsonPath.read(body, "$.data.content[*].imageId");
    }

    private static String nextCursor(String body) {
        return JsonPath.read(body, "$.data.nextCursor");
    }

    @Test
    void invalidParamsReturnValidationError() throws Exception {
        mockMvc.perform(get("/api/images").param("bbox", "126.9,37.5,127.1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_002"));
        mockMvc.perform(get("/api/images").param("sorts", "FOO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorDetail[0]").value("sorts: 타입이 올바르지 않습니다."));
        mockMvc.perform(get("/api/images").param("sorts", "ACQUIRED_AT_ASC", "CLOUD_COVER_ASC", "ACQUIRED_AT_DESC"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorDetail[0]").value("sorts: 같은 정렬 기준은 한 번만 지정할 수 있습니다."));
    }

    @Test
    void crudKeepsSearchFootprintAndAuditingAndCascadesOnlyDatabaseRows() throws Exception {
        long uploadsBefore = jdbc.queryForObject("select count(*) from image_upload", Long.class);
        long id = create(request);
        assertThat(jdbc.queryForObject("select count(*) from image_upload", Long.class))
                .isEqualTo(uploadsBefore);
        assertThat(jdbc.queryForObject(
                        "select ST_SRID(footprint) from satellite_image where image_id = ?", Integer.class, id))
                .isEqualTo(4326);

        String initial = mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imageId").value(id))
                .andExpect(jsonPath("$.data.providerCode").value("earth-search"))
                .andExpect(jsonPath("$.data.collection.providerCode").value("earth-search"))
                .andExpect(jsonPath("$.data.collection.code").value("sentinel-2-c1-l2a"))
                .andExpect(jsonPath("$.data.sourceId").value("admin-crud-test"))
                .andExpect(jsonPath("$.data.satellite").value("SENTINEL-2A"))
                .andExpect(jsonPath("$.data.instruments[0]").value("MSI"))
                .andExpect(jsonPath("$.data.sensorType").value("OPTICAL"))
                .andExpect(jsonPath("$.data.acquiredAt").value("2026-10-19T01:00:00Z"))
                .andExpect(jsonPath("$.data.acquiredEndAt").value("2026-10-19T01:00:27Z"))
                .andExpect(jsonPath("$.data.resolution", nullValue()))
                .andExpect(jsonPath("$.data.cloudCover").value(5.25))
                .andExpect(jsonPath("$.data.orbitDirection").value("ASCENDING"))
                .andExpect(jsonPath("$.data.footprint.type").value("MultiPolygon"))
                .andExpect(jsonPath("$.data.url").value("archive/metadata.json"))
                .andExpect(jsonPath("$.data.downloadUrls", hasSize(2)))
                .andExpect(jsonPath("$.data.sourceUpdatedAt").value("2026-10-19T01:00:00Z"))
                .andExpect(jsonPath("$.data.rawMetadata.nested.value").value(1))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        searchFootprint(126.9);
        var document = JsonPath.parse(request)
                .set("$.title", "수정된 영상")
                .set("$.sensorType", "SAR")
                .set(
                        "$.footprint.coordinates",
                        List.of(List.of(
                                List.of(List.of(128, 35), List.of(129, 35), List.of(129, 36), List.of(128, 35)))));
        for (String field : List.of(
                "instruments", "acquiredEndAt", "cloudCover", "orbitDirection", "sourceUpdatedAt", "rawMetadata")) {
            document.delete("$." + field);
        }
        mockMvc.perform(json(put(BASE + "/" + id), document.jsonString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        String updated = mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("수정된 영상"))
                .andExpect(jsonPath("$.data.instruments", nullValue()))
                .andExpect(jsonPath("$.data.cloudCover", nullValue()))
                .andExpect(jsonPath("$.data.orbitDirection", nullValue()))
                .andExpect(jsonPath("$.data.sourceUpdatedAt", nullValue()))
                .andExpect(jsonPath("$.data.acquiredEndAt", nullValue()))
                .andExpect(jsonPath("$.data.rawMetadata").isEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        assertThat((String) JsonPath.read(updated, "$.data.createdAt"))
                .isEqualTo(JsonPath.read(initial, "$.data.createdAt"));
        assertThat((String) JsonPath.read(updated, "$.data.updatedAt"))
                .isNotEqualTo(JsonPath.read(initial, "$.data.updatedAt"));
        searchFootprint(128);

        jdbc.update("""
                insert into image_upload (image_id, status, original_file_name)
                values (?, 'UPLOADED', 'admin-crud-test.tar')
                """, id);
        // 테스트는 요청들이 트랜잭션 하나를 같이 쓴다. 실제 삭제 요청처럼 빈 영속성 컨텍스트에서 시작한다
        entityManager.clear();
        mockMvc.perform(delete(BASE + "/" + id)).andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/" + id)).andExpect(status().isNotFound());
        assertThat(repository.existsById(id)).isFalse();
        assertThat(jdbc.queryForObject("select count(*) from image_upload where image_id = ?", Long.class, id))
                .isZero();
        assertThat(jdbc.queryForObject("select count(*) from image_file where image_id = ?", Long.class, id))
                .isZero();
    }

    @Test
    void nullOptionalsAndRawMetadataAreCleared() throws Exception {
        long id = create(request);
        var document = JsonPath.parse(request);
        for (String field : List.of(
                "instruments", "acquiredEndAt", "cloudCover", "orbitDirection", "sourceUpdatedAt", "rawMetadata")) {
            document.set("$." + field, null);
        }
        mockMvc.perform(json(put(BASE + "/" + id), document.jsonString())).andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rawMetadata").isEmpty())
                .andExpect(jsonPath("$.data.instruments", nullValue()))
                .andExpect(jsonPath("$.data.cloudCover", nullValue()))
                .andExpect(jsonPath("$.data.orbitDirection", nullValue()))
                .andExpect(jsonPath("$.data.sourceUpdatedAt", nullValue()))
                .andExpect(jsonPath("$.data.acquiredEndAt", nullValue()));
    }

    // 같은 컬렉션 안에서만 sourceId가 겹치면 안 된다
    @Test
    void duplicateSourceIdConflictsOnlyWithinCollection() throws Exception {
        long id = create(request);
        mockMvc.perform(json(post(BASE), request)).andExpect(status().isConflict());
        mockMvc.perform(json(put(BASE + "/" + id), request)).andExpect(status().isOk());
        long second = create(changed("sourceId", "other-source"));
        mockMvc.perform(json(put(BASE + "/" + second), request)).andExpect(status().isConflict());
        mockMvc.perform(json(post(BASE), changed("sourceId", "fx-s2-low"))).andExpect(status().isConflict());
        create(changed("collectionId", collectionId("nasa-cmr", "C2021957657-LPCLOUD")));
    }

    // 영상의 제공 기관은 컬렉션을 따른다. 컬렉션을 바꾸면 제공 기관도 바뀐다
    @Test
    void providerFollowsCollection() throws Exception {
        long id = create(changed("collectionId", collectionId("nasa-cmr", "C2021957657-LPCLOUD")));
        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.providerCode").value("nasa-cmr"));
        mockMvc.perform(json(put(BASE + "/" + id), request)).andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.providerCode").value("earth-search"));
    }

    // 그대로인 URL의 행은 남기고, 빠진 URL은 지우고, 새 URL은 확장자로 역할을 정해 한 번만 추가한다
    @Test
    void filesFollowDownloadUrls() throws Exception {
        long id = create(request);
        String filesQuery = """
                select file_id || '/' || asset_key || '/' || role || '/' || href
                from image_file where image_id = ? order by file_id
                """;
        List<String> created = jdbc.queryForList(filesQuery, String.class, id);
        assertThat(created).hasSize(2);
        assertThat(created.get(0)).endsWith("/image.tif/DATA/s3://archive/image.tif");
        String kept = created.get(1);
        assertThat(kept).endsWith("/image.tif/DATA/https://example.test/image.tif");

        mockMvc.perform(json(
                        put(BASE + "/" + id),
                        changed(
                                "downloadUrls",
                                List.of(
                                        "https://example.test/image.tif",
                                        "https://example.test/scene.tar.gz",
                                        "https://example.test/meta.XML",
                                        "https://example.test/meta.XML"))))
                .andExpect(status().isOk());

        List<String> updated = jdbc.queryForList(filesQuery, String.class, id);
        assertThat(updated).hasSize(3);
        assertThat(updated.get(0)).isEqualTo(kept);
        assertThat(updated.get(1)).endsWith("/scene.tar.gz/ORIGINAL/https://example.test/scene.tar.gz");
        assertThat(updated.get(2)).endsWith("/meta.XML/METADATA/https://example.test/meta.XML");
    }

    @Test
    void missingIdsReturn404AndPutDoesNotInsert() throws Exception {
        long count = repository.count();
        String path = BASE + "/" + Long.MAX_VALUE;
        mockMvc.perform(get(path)).andExpect(status().isNotFound());
        mockMvc.perform(json(put(path), request)).andExpect(status().isNotFound());
        mockMvc.perform(delete(path)).andExpect(status().isNotFound());
        assertThat(repository.count()).isEqualTo(count);
    }

    @ParameterizedTest
    @MethodSource("invalidFields")
    void invalidInputReturns400WithoutWriting(String field, Object value) throws Exception {
        long count = repository.count();
        mockMvc.perform(json(post(BASE), changed(field, value)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        assertThat(repository.count()).isEqualTo(count);
    }

    static Stream<Arguments> invalidFields() {
        return Stream.of(
                Arguments.of("collectionId", Long.MAX_VALUE),
                Arguments.of("sourceId", "s".repeat(201)),
                Arguments.of("title", "t".repeat(301)),
                Arguments.of("title", "  "),
                Arguments.of("satellite", "s".repeat(51)),
                Arguments.of("satellite", "  "),
                Arguments.of("sensorType", "INVALID"),
                Arguments.of("sensorType", "SAR"),
                Arguments.of("orbitDirection", "SIDEWAYS"),
                Arguments.of("cloudCover", -0.01),
                Arguments.of("cloudCover", 100.01),
                Arguments.of("downloadUrls", List.of()),
                Arguments.of("downloadUrls", List.of(" ")),
                Arguments.of("downloadUrls", java.util.Arrays.asList("ok", null)),
                Arguments.of("instruments", List.of()),
                Arguments.of("instruments", List.of(" ")),
                Arguments.of("instruments", java.util.Arrays.asList("ok", null)),
                Arguments.of("url", " "),
                Arguments.of("acquiredAt", "2026-10-19T10:00:00"),
                Arguments.of("acquiredEndAt", "2026-10-19T09:59:59+09:00"),
                Arguments.of("acquiredEndAt", "bad-date"),
                Arguments.of("sourceUpdatedAt", "bad-date"),
                Arguments.of("rawMetadata", List.of("not-an-object")),
                Arguments.of("footprint", "not-an-object"),
                Arguments.of("footprint", Map.of()),
                Arguments.of("footprint", Map.of("type", Map.of())),
                Arguments.of("footprint", Map.of("type", "Polygon", "coordinates", List.of())),
                Arguments.of("footprint", Map.of("type", "MultiPolygon", "coordinates", List.of())),
                Arguments.of("footprint", Map.of("type", "MultiPolygon", "coordinates", List.of(List.of()))),
                Arguments.of("footprint", Map.of("type", "MultiPolygon")),
                Arguments.of("footprint", geometry("[[[[181,37],[182,37],[182,38],[181,37]]]]")),
                Arguments.of("footprint", geometry("[[[[127,91],[128,91],[128,92],[127,91]]]]")),
                Arguments.of("footprint", geometry("[[[[126,37],[127,38],[127,37],[126,38],[126,37]]]]")),
                Arguments.of("footprint", geometry("[[[[126,37],[127,37],[127,38],[126,38]]]]")),
                Arguments.of("footprint", geometry("[[[[\"126\",37],[127,37],[127,38],[126,37]]]]")),
                Arguments.of("footprint", geometry("[[[[126,37,0],[127,37,0],[127,38,0],[126,37,0]]]]")));
    }

    @ParameterizedTest
    @MethodSource("requiredFields")
    void requiredFieldsCannotBeNullOrOmitted(String field) throws Exception {
        mockMvc.perform(json(post(BASE), changed(field, null))).andExpect(status().isBadRequest());
        mockMvc.perform(json(
                        post(BASE), JsonPath.parse(request).delete("$." + field).jsonString()))
                .andExpect(status().isBadRequest());
    }

    static Stream<String> requiredFields() {
        return Stream.of(
                "collectionId",
                "sourceId",
                "title",
                "satellite",
                "sensorType",
                "acquiredAt",
                "footprint",
                "url",
                "downloadUrls");
    }

    private long create(String body) throws Exception {
        mockMvc.perform(json(post(BASE), body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", nullValue()));
        Number collectionId = JsonPath.read(body, "$.collectionId");
        String sourceId = JsonPath.read(body, "$.sourceId");
        return jdbc.queryForObject(
                "select image_id from satellite_image where collection_id = ? and source_id = ?",
                Long.class,
                collectionId.longValue(),
                sourceId);
    }

    private long imageId(String sourceId) {
        return jdbc.queryForObject("select image_id from satellite_image where source_id = ?", Long.class, sourceId);
    }

    // fixture 영상만 고르는 검색. 정렬이 정해지지 않은 테스트는 촬영 일시 최신순이다
    private static MockHttpServletRequestBuilder fixtures() {
        return get("/api/images").param("keyword", "fx").param("size", "100");
    }

    private void searchFootprint(double longitude) throws Exception {
        // 같은 영속성 컨텍스트에 남아 있는 @Formula도 등록/수정된 좌표를 가져야 한다.
        mockMvc.perform(get("/api/images").param("satellites", "SENTINEL-2A").param("acquiredFrom", "2026-10-19"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].footprint.type").value("MultiPolygon"))
                .andExpect(jsonPath("$.data.content[0].footprint.coordinates[0][0][0][0]")
                        .value(longitude));
    }

    private String changed(String field, Object value) {
        return JsonPath.parse(request).set("$." + field, value).jsonString();
    }

    private static Map<String, Object> geometry(String coordinates) {
        return Map.of(
                "type",
                "MultiPolygon",
                "coordinates",
                JsonPath.parse(coordinates).read("$"));
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
