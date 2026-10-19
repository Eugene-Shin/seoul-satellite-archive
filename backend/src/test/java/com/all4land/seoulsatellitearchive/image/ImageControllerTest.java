package com.all4land.seoulsatellitearchive.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
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

// 로컬 전용 DX 시드(provider=dx)가 있어도 없어도 같은 결과가 나오는 조건으로만 검증한다
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class ImageControllerTest {

    private static final String BASE = "/api/images";
    private static final String REQUEST = """
            {
              "providerCode": "earth-search", "sourceId": "admin-crud-test", "title": "관리자 CRUD 테스트",
              "satellite": " sentinel-2a ", "instruments": ["msi"], "sensorType": "OPTICAL",
              "acquiredAt": "2026-10-19T10:00:00+09:00", "gsdMeters": 10.25, "cloudCover": 5.25,
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

    @Test
    void filterOptions() throws Exception {
        mockMvc.perform(get("/api/images/filter-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.satellites", hasItems("LANDSAT-8", "SENTINEL-1A", "WV02")))
                .andExpect(jsonPath("$.data.instruments", hasItems("C-SAR", "OLI", "msi")))
                .andExpect(jsonPath("$.data.providers", hasSize(4)))
                .andExpect(jsonPath("$.data.sensorTypes", contains("OPTICAL", "SAR")));
    }

    @Test
    void instrumentsMatchAnySelected() throws Exception {
        mockMvc.perform(get("/api/images").param("instruments", "OLI", "C-SAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(4)));
    }

    @Test
    void keywordTreatsUnderscoreLiterally() throws Exception {
        mockMvc.perform(get("/api/images").param("keyword", "_").param("providers", "maxar", "nasa-cmr"))
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
        long id = create(REQUEST);
        assertThat(jdbc.queryForObject("select count(*) from image_upload", Long.class))
                .isEqualTo(uploadsBefore);
        assertThat(jdbc.queryForObject(
                        "select ST_SRID(footprint) from satellite_image where image_id = ?", Integer.class, id))
                .isEqualTo(4326);

        String initial = mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imageId").value(id))
                .andExpect(jsonPath("$.data.providerCode").value("earth-search"))
                .andExpect(jsonPath("$.data.sourceId").value("admin-crud-test"))
                .andExpect(jsonPath("$.data.satellite").value("SENTINEL-2A"))
                .andExpect(jsonPath("$.data.instruments[0]").value("msi"))
                .andExpect(jsonPath("$.data.sensorType").value("OPTICAL"))
                .andExpect(jsonPath("$.data.acquiredAt").value("2026-10-19T01:00:00Z"))
                .andExpect(jsonPath("$.data.gsdMeters").value(10.25))
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
        var document = JsonPath.parse(REQUEST)
                .set("$.title", "수정된 영상")
                .set("$.sensorType", "SAR")
                .set(
                        "$.footprint.coordinates",
                        List.of(List.of(
                                List.of(List.of(128, 35), List.of(129, 35), List.of(129, 36), List.of(128, 35)))));
        for (String field :
                List.of("instruments", "gsdMeters", "cloudCover", "orbitDirection", "sourceUpdatedAt", "rawMetadata")) {
            document.delete("$." + field);
        }
        mockMvc.perform(json(put(BASE + "/" + id), document.jsonString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        String updated = mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("수정된 영상"))
                .andExpect(jsonPath("$.data.instruments", nullValue()))
                .andExpect(jsonPath("$.data.gsdMeters", nullValue()))
                .andExpect(jsonPath("$.data.cloudCover", nullValue()))
                .andExpect(jsonPath("$.data.orbitDirection", nullValue()))
                .andExpect(jsonPath("$.data.sourceUpdatedAt", nullValue()))
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
        mockMvc.perform(delete(BASE + "/" + id)).andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/" + id)).andExpect(status().isNotFound());
        assertThat(repository.existsById(id)).isFalse();
        assertThat(jdbc.queryForObject("select count(*) from image_upload where image_id = ?", Long.class, id))
                .isZero();
    }

    @Test
    void nullOptionalsAndRawMetadataAreCleared() throws Exception {
        long id = create(REQUEST);
        var document = JsonPath.parse(REQUEST);
        for (String field :
                List.of("instruments", "gsdMeters", "cloudCover", "orbitDirection", "sourceUpdatedAt", "rawMetadata")) {
            document.set("$." + field, null);
        }
        mockMvc.perform(json(put(BASE + "/" + id), document.jsonString())).andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rawMetadata").isEmpty())
                .andExpect(jsonPath("$.data.instruments", nullValue()))
                .andExpect(jsonPath("$.data.gsdMeters", nullValue()))
                .andExpect(jsonPath("$.data.cloudCover", nullValue()))
                .andExpect(jsonPath("$.data.orbitDirection", nullValue()))
                .andExpect(jsonPath("$.data.sourceUpdatedAt", nullValue()));
    }

    @Test
    void duplicateCreateAndUpdateConflictButOwnSourceAndOtherProvidersAreAllowed() throws Exception {
        long id = create(REQUEST);
        mockMvc.perform(json(post(BASE), REQUEST)).andExpect(status().isConflict());
        mockMvc.perform(json(put(BASE + "/" + id), REQUEST)).andExpect(status().isOk());
        long second = create(changed("sourceId", "other-source"));
        mockMvc.perform(json(put(BASE + "/" + second), REQUEST)).andExpect(status().isConflict());
        create(changed("providerCode", "nasa-cmr"));
    }

    @Test
    void missingIdsReturn404AndPutDoesNotInsert() throws Exception {
        long count = repository.count();
        String path = BASE + "/" + Long.MAX_VALUE;
        mockMvc.perform(get(path)).andExpect(status().isNotFound());
        mockMvc.perform(json(put(path), REQUEST)).andExpect(status().isNotFound());
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
                Arguments.of("providerCode", "unknown-provider"),
                Arguments.of("providerCode", "p".repeat(31)),
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
                Arguments.of("gsdMeters", 1000000),
                Arguments.of("gsdMeters", 0.001),
                Arguments.of("downloadUrls", List.of()),
                Arguments.of("downloadUrls", List.of(" ")),
                Arguments.of("downloadUrls", java.util.Arrays.asList("ok", null)),
                Arguments.of("instruments", List.of()),
                Arguments.of("instruments", List.of(" ")),
                Arguments.of("instruments", java.util.Arrays.asList("ok", null)),
                Arguments.of("url", " "),
                Arguments.of("acquiredAt", "2026-10-19T10:00:00"),
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
                        post(BASE), JsonPath.parse(REQUEST).delete("$." + field).jsonString()))
                .andExpect(status().isBadRequest());
    }

    static Stream<String> requiredFields() {
        return Stream.of(
                "providerCode",
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
        String providerCode = JsonPath.read(body, "$.providerCode");
        String sourceId = JsonPath.read(body, "$.sourceId");
        return jdbc.queryForObject(
                "select image_id from satellite_image where provider = ? and source_id = ?",
                Long.class,
                providerCode,
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

    private static String changed(String field, Object value) {
        return JsonPath.parse(REQUEST).set("$." + field, value).jsonString();
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
