package com.all4land.seoulsatellitearchive.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
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
import com.all4land.seoulsatellitearchive.provider.Provider;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

// 로컬 전용 ALL4LAND 시드(provider=all4land)가 있어도 없어도 같은 결과가 나오는 조건으로만 검증한다
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
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
                .set("$.collectionId", collectionId("earth-search", "sentinel-2-l2a"))
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
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.data[?(@.code == 'sentinel-2-l2a')].providerCode", contains("earth-search")));
    }

    // 백필은 출처가 영상에 적어 둔 컬렉션 참조(STAC collection, CMR collection-concept-id)를 따른다
    @Test
    void seedImagesAreBackfilledFromSourceCollectionReference() {
        assertThat(jdbc.queryForList("""
                        select distinct i.provider || '/' || c.code
                        from satellite_image i join image_collection c on c.collection_id = i.collection_id
                        where i.provider <> 'all4land'
                        """, String.class))
                .containsExactlyInAnyOrder(
                        "earth-search/sentinel-2-l2a", "nasa-cmr/C2021957657-LPCLOUD", "nasa-cmr/C1214470488-ASF");
    }

    @Test
    void filterOptions() throws Exception {
        mockMvc.perform(get("/api/images/filter-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.satellites", hasItems("LANDSAT-8", "SENTINEL-1A", "SENTINEL-2A")))
                .andExpect(jsonPath("$.data.instruments", hasItems("C-SAR", "OLI", "MSI")))
                .andExpect(jsonPath("$.data.providers", hasSize(5)))
                .andExpect(jsonPath("$.data.providers[?(@.ownOrganization == true)].code", contains("all4land")))
                .andExpect(jsonPath("$.data.sensorTypes", contains("OPTICAL", "SAR")));
    }

    @Test
    void collectionsMatchAnySelected() throws Exception {
        String s1Slc = String.valueOf(collectionId("nasa-cmr", "C1214470488-ASF"));
        String hls = String.valueOf(collectionId("nasa-cmr", "C2021957657-LPCLOUD"));
        mockMvc.perform(get("/api/images").param("collectionIds", s1Slc))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[*].collection.code", everyItem(is("C1214470488-ASF"))))
                .andExpect(jsonPath("$.data.content[*].collection.productType", everyItem(is("SLC"))));
        mockMvc.perform(get("/api/images").param("collectionIds", s1Slc, hls))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(4)));
    }

    @Test
    void productTypesMatchAnySelected() throws Exception {
        mockMvc.perform(get("/api/images").param("productTypes", "SLC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)));
        mockMvc.perform(get("/api/images").param("productTypes", "SLC", "L30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(4)));
    }

    // 촬영 종료 일시도 출처 값 그대로: STAC은 end_datetime이 없으면 NULL, CMR은 RangeDateTime.EndingDateTime
    @Test
    void seedAcquiredEndAtIsBackfilledFromSource() {
        assertThat(jdbc.queryForObject(
                        "select count(*) from satellite_image where provider = 'earth-search' and acquired_end_at is not null",
                        Long.class))
                .isZero();
        assertThat(jdbc.queryForObject("""
                        select count(*) from satellite_image
                        where provider = 'nasa-cmr' and acquired_end_at = (raw_metadata #>> '{umm,TemporalExtent,RangeDateTime,EndingDateTime}')::timestamptz
                        """, Long.class)).isEqualTo(4);
    }

    // STAC은 에셋마다 한 행, 밴드는 eo:bands. 픽셀 간격은 에셋의 proj:transform
    @Test
    void seedStacAssetsAreBackfilledAsFilesAndBands() {
        assertThat(jdbc.queryForObject("""
                        select count(*) from satellite_image i
                        where i.provider = 'earth-search'
                          and (select count(*) from image_file f where f.image_id = i.image_id)
                              <> (select count(*) from jsonb_object_keys(i.raw_metadata -> 'assets'))
                        """, Long.class)).isZero();
        assertThat(jdbc.queryForList("""
                        select f.asset_key || '/' || f.role || '/' || coalesce(f.pixel_spacing_meters::text, '-')
                               || '/' || coalesce(string_agg(b.band_name || ':' || coalesce(b.common_name, '-')
                                                             || ':' || coalesce(b.resolution_meters::text, '-'),
                                                             ',' order by b.band_index), '-')
                        from image_file f
                                 join satellite_image i on i.image_id = f.image_id
                                 left join image_file_band b on b.file_id = f.file_id
                        where i.source_id = 'S2B_52SCG_20241227_0_L2A'
                          and f.asset_key in ('red', 'coastal', 'visual', 'thumbnail', 'granule_metadata')
                        group by f.asset_key, f.role, f.pixel_spacing_meters
                        """, String.class))
                .containsExactlyInAnyOrder(
                        "red/DATA/10.00/red:red:10.00",
                        "coastal/DATA/60.00/coastal:coastal:60.00",
                        // visual 에셋엔 센서 해상도(gsd)가 없어서 비워 둔다
                        "visual/PREVIEW/10.00/red:red:-,green:green:-,blue:blue:-",
                        "thumbnail/THUMBNAIL/-/-",
                        "granule_metadata/METADATA/-/-");
    }

    // 파일은 역할순, 밴드는 파일 안 순서. 밴드가 없는 파일도 빠지지 않고 빈 배열로 온다
    @Test
    void detailReturnsFilesWithBands() throws Exception {
        long seedId = jdbc.queryForObject(
                "select image_id from satellite_image where provider = 'earth-search' and source_id = ?",
                Long.class,
                "S2B_52SCG_20241227_0_L2A");
        String body = mockMvc.perform(get(BASE + "/" + seedId))
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

    // CMR은 https 링크만. HLS DATA 파일은 그래뉼의 SPATIAL_RESOLUTION, S1은 원본 묶음 하나
    @Test
    void seedCmrRelatedUrlsAreBackfilledAsFiles() {
        assertThat(jdbc.queryForList("""
                        select c.code || '/' || f.role || '/' || coalesce(f.pixel_spacing_meters::text, '-')
                               || '/' || count(*)
                        from image_file f
                                 join satellite_image i on i.image_id = f.image_id
                                 join image_collection c on c.collection_id = i.collection_id
                        where i.provider = 'nasa-cmr'
                        group by i.image_id, c.code, f.role, f.pixel_spacing_meters
                        """, String.class))
                .containsOnly(
                        "C2021957657-LPCLOUD/DATA/30.00/15",
                        "C2021957657-LPCLOUD/METADATA/-/2",
                        "C2021957657-LPCLOUD/THUMBNAIL/-/1",
                        "C1214470488-ASF/ORIGINAL/-/1");
        assertThat(jdbc.queryForObject(
                        "select count(*) from image_file where href not like 'https://%' and image_id in "
                                + "(select image_id from satellite_image where provider = 'nasa-cmr')",
                        Long.class))
                .isZero();
    }

    // ASF CMR은 umm.AdditionalAttributes에 SAR 정보를 준다. 입사각·해상도는 주지 않아서 NULL
    @Test
    void seedSarIsBackfilledFromCmrAttributes() throws Exception {
        assertThat(jdbc.queryForList("""
                        select s.relative_orbit || '/' || s.frame || '/' || s.instrument_mode || '/'
                               || array_to_string(s.polarizations, '+') || '/' || s.look_direction
                               || '/' || coalesce(s.incidence_angle::text, '-')
                        from image_sar s join satellite_image i on i.image_id = s.image_id
                        where i.provider <> 'all4land'
                        """, String.class))
                .containsExactly("127/120/IW/VV+VH/RIGHT/-", "127/120/IW/VV+VH/RIGHT/-");
        mockMvc.perform(get("/api/images").param("productTypes", "SLC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].sar.relativeOrbit", everyItem(is(127))))
                .andExpect(jsonPath("$.data.content[*].sar.polarizations[0]", everyItem(is("VV"))));
        mockMvc.perform(get("/api/images").param("productTypes", "L2A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].sar", nullValue()));
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
        mockMvc.perform(get("/api/images").param("instruments", "OLI", "C-SAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(4)));
    }

    @Test
    void keywordTreatsUnderscoreLiterally() throws Exception {
        mockMvc.perform(get("/api/images").param("keyword", "_").param("providers", "nasa-cmr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].satellite", everyItem(is("SENTINEL-1A"))))
                .andExpect(jsonPath("$.data.content", hasSize(2)));
    }

    @Test
    void acquiredDateRangeCoversWholeKstDay() throws Exception {
        mockMvc.perform(get("/api/images").param("acquiredFrom", "2026-01-08").param("acquiredTo", "2026-01-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)));
    }

    @Test
    void cloudMaxExcludesSar() throws Exception {
        mockMvc.perform(get("/api/images").param("cloudMax", "100").param("sensorTypes", "SAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    @Test
    void bboxFiltersByFootprint() throws Exception {
        mockMvc.perform(get("/api/images").param("bbox", "128.9,35.0,129.2,35.3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
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
        mockMvc.perform(get("/api/images").param("bbox", "129.0,35.1,129.0,35.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)));
    }

    @Test
    void cloudSortPutsNullsLast() throws Exception {
        mockMvc.perform(get("/api/images")
                        .param("providers", "earth-search", "nasa-cmr")
                        .param("sorts", "CLOUD_COVER_DESC")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].cloudCover").value(100.00))
                .andExpect(jsonPath("$.data.content[-1].cloudCover", nullValue()));
    }

    // S2·HLS는 출처 썸네일, S1은 썸네일이 없다
    @Test
    void searchReturnsThumbnailOfSeedImages() throws Exception {
        mockMvc.perform(get("/api/images")
                        .param("providers", "earth-search", "nasa-cmr")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.data.content[?(@.collection.code == 'sentinel-2-l2a')].thumbnailUrl",
                        everyItem(endsWith(".jpg"))))
                .andExpect(jsonPath(
                        "$.data.content[?(@.collection.code == 'C2021957657-LPCLOUD')].thumbnailUrl",
                        everyItem(endsWith(".jpg"))))
                .andExpect(jsonPath(
                        "$.data.content[?(@.collection.code == 'C1214470488-ASF')].thumbnailUrl",
                        everyItem(nullValue())));
    }

    // S2만 겹칠 수 있는 COG(visual)가 있다. HLS는 미리보기 파일이 없고 S1은 묶음 파일 하나다
    @Test
    void searchReturnsOverlayOfSeedImages() throws Exception {
        mockMvc.perform(get("/api/images")
                        .param("providers", "earth-search", "nasa-cmr")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.data.content[?(@.collection.code == 'sentinel-2-l2a')].overlayUrl",
                        everyItem(endsWith("/TCI.tif"))))
                .andExpect(jsonPath(
                        "$.data.content[?(@.collection.code == 'C2021957657-LPCLOUD')].overlayUrl",
                        everyItem(nullValue())))
                .andExpect(jsonPath(
                        "$.data.content[?(@.collection.code == 'C1214470488-ASF')].overlayUrl",
                        everyItem(nullValue())));
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

    // 해상도 범위가 겹치면 포함한다. S2는 파일 픽셀 간격 10~60 m, HLS는 30 m
    @Test
    void resolutionRangeOverlapsImageResolution() throws Exception {
        mockMvc.perform(get("/api/images")
                        .param("providers", "earth-search", "nasa-cmr")
                        .param("resolutionMin", "40")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(12)))
                .andExpect(jsonPath("$.data.content[*].provider.code", everyItem(is("earth-search"))))
                .andExpect(jsonPath("$.data.content[*].resolution.minMeters", everyItem(is(10.00))))
                .andExpect(jsonPath("$.data.content[*].resolution.maxMeters", everyItem(is(60.00))));
        mockMvc.perform(get("/api/images")
                        .param("providers", "earth-search", "nasa-cmr")
                        .param("resolutionMax", "20")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].provider.code", everyItem(is("earth-search"))));
        mockMvc.perform(get("/api/images")
                        .param("providers", "earth-search", "nasa-cmr")
                        .param("resolutionMin", "25")
                        .param("resolutionMax", "35")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].provider.code", hasItems("earth-search", "nasa-cmr")))
                .andExpect(jsonPath(
                        "$.data.content[?(@.collection.code == 'C2021957657-LPCLOUD')].resolution.minMeters",
                        everyItem(is(30.00))));
    }

    @Test
    void cursorContinuesUntilNull() throws Exception {
        String first = search(null, "5", "providers", "earth-search");
        assertThat(ids(first)).hasSize(5);
        String second = search(nextCursor(first), "5", "providers", "earth-search");
        assertThat(ids(second)).hasSize(5);
        String third = search(nextCursor(second), "5", "providers", "earth-search");
        assertThat(ids(third)).hasSize(2);
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
                .andExpect(jsonPath("$.data.providerCode").value("all4land"))
                .andExpect(jsonPath("$.data.collection.providerCode").value("earth-search"))
                .andExpect(jsonPath("$.data.collection.code").value("sentinel-2-l2a"))
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

    @Test
    void duplicateCreateAndUpdateConflictButOwnSourceAndOtherProvidersAreAllowed() throws Exception {
        long id = create(request);
        mockMvc.perform(json(post(BASE), request)).andExpect(status().isConflict());
        mockMvc.perform(json(put(BASE + "/" + id), request)).andExpect(status().isOk());
        long second = create(changed("sourceId", "other-source"));
        mockMvc.perform(json(put(BASE + "/" + second), request)).andExpect(status().isConflict());
        create(changed("sourceId", "S2B_52SCG_20241227_0_L2A"));
    }

    // 자동으로 수집한 영상의 파일 정보는 수집기가 관리하므로 요청의 다운로드 URL을 무시한다
    @Test
    void updateKeepsProviderAndCollectedFiles() throws Exception {
        long seedId = jdbc.queryForObject(
                "select image_id from satellite_image where provider = 'earth-search' and source_id = ?",
                Long.class,
                "S2B_52SCG_20241227_0_L2A");
        String filesQuery = "select string_agg(href, ',' order by file_id) from image_file where image_id = ?";
        String downloadUrlsQuery = "select array_to_string(download_urls, ',') from satellite_image where image_id = ?";
        String files = jdbc.queryForObject(filesQuery, String.class, seedId);
        String downloadUrls = jdbc.queryForObject(downloadUrlsQuery, String.class, seedId);

        mockMvc.perform(json(put(BASE + "/" + seedId), request)).andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/" + seedId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.providerCode").value("earth-search"));
        assertThat(jdbc.queryForObject(filesQuery, String.class, seedId)).isEqualTo(files);
        assertThat(jdbc.queryForObject(downloadUrlsQuery, String.class, seedId)).isEqualTo(downloadUrls);
    }

    // 그대로인 URL의 행은 남기고, 빠진 URL은 지우고, 새 URL은 확장자로 역할을 정해 한 번만 추가한다
    @Test
    void ownOrganizationFilesFollowDownloadUrls() throws Exception {
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

    // 올포랜드 영상(등록)은 다른 출처의 컬렉션을 가질 수 있지만, 다른 출처의 영상은 그 출처의 컬렉션만 가진다
    @Test
    void onlyOwnOrganizationImagesTakeOtherProvidersCollections() throws Exception {
        String otherProviders = changed("collectionId", collectionId("nasa-cmr", "C1214470488-ASF"));
        create(otherProviders);
        long seedId = jdbc.queryForObject(
                "select image_id from satellite_image where provider = 'earth-search' and source_id = ?",
                Long.class,
                "S2B_52SCG_20241227_0_L2A");
        mockMvc.perform(json(put(BASE + "/" + seedId), otherProviders))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorDetail[0]").value("collectionId: 이 제공 기관의 컬렉션이 아닙니다."));
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
        String sourceId = JsonPath.read(body, "$.sourceId");
        return jdbc.queryForObject(
                "select image_id from satellite_image where provider = ? and source_id = ?",
                Long.class,
                Provider.OWN_ORGANIZATION_CODE,
                sourceId);
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
