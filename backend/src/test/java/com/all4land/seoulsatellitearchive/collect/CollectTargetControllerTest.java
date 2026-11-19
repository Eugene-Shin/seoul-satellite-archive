package com.all4land.seoulsatellitearchive.collect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.all4land.seoulsatellitearchive.global.config.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
@Sql("/fixtures/uncollectable-collection.sql")
class CollectTargetControllerTest {

    private static final String BASE = "/api/collect-targets";
    private static final String AOI =
            "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[126.9,37.5],[127.1,37.5],[127.1,37.6],[126.9,37.5]]]]}";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    private long c1CollectionId;
    private long uncollectableId;

    @BeforeEach
    void setUp() {
        c1CollectionId = collectionId("sentinel-2-c1-l2a");
        uncollectableId = collectionId("test-uncollectable");
    }

    // seed로 서울 c1 수집 대상이 매일 03:00으로 들어 있다
    @Test
    void listIncludesSeededSeoulTarget() throws Exception {
        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].collection.code").value("sentinel-2-c1-l2a"))
                .andExpect(jsonPath("$.data[0].collection.collectable").value(true))
                .andExpect(jsonPath("$.data[0].aoi.type").value("MultiPolygon"))
                .andExpect(jsonPath("$.data[0].scheduleCron").value("0 0 3 * * *"))
                .andExpect(jsonPath("$.data[0].lookbackDays").value(7))
                .andExpect(jsonPath("$.data[0].enabled").value(true))
                .andExpect(jsonPath("$.data[0].nextRunAt").value(containsString("T18:00:00Z")));
    }

    @Test
    void defaultAoiIsSeoulBoundaryWithSource() throws Exception {
        mockMvc.perform(get(BASE + "/default-aoi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("seoul"))
                .andExpect(jsonPath("$.data.name").value("서울특별시"))
                .andExpect(jsonPath("$.data.aoi.type").value("MultiPolygon"))
                .andExpect(jsonPath("$.data.source").value("행정구역 경계: 국토교통부(브이월드)"));
    }

    @Test
    void createUpdateAndDelete() throws Exception {
        removeSeededTarget();
        String created = mockMvc.perform(json(post(BASE), body(c1CollectionId, "0 30 4 * * *", 3, true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.aoi.coordinates[0][0][0][0]").value(126.9))
                .andExpect(jsonPath("$.data.lookbackDays").value(3))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long targetId = ((Number) JsonPath.read(created, "$.data.targetId")).longValue();

        mockMvc.perform(json(put(BASE + "/" + targetId), body(c1CollectionId, "0 0 5 * * MON", 10, false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scheduleCron").value("0 0 5 * * MON"))
                .andExpect(jsonPath("$.data.lookbackDays").value(10))
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.nextRunAt").doesNotExist());

        mockMvc.perform(delete(BASE + "/" + targetId)).andExpect(status().isOk());
        mockMvc.perform(get(BASE)).andExpect(jsonPath("$.data[*].targetId", not(hasItem((int) targetId))));
    }

    @Test
    void rejectsInvalidValues() throws Exception {
        removeSeededTarget();
        mockMvc.perform(json(post(BASE), body(uncollectableId, "0 0 3 * * *", 7, true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorDetail[0]").value(containsString("수집할 수 없는 컬렉션")));
        mockMvc.perform(json(post(BASE), body(c1CollectionId, "0 3 * * *", 7, true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorDetail[0]").value(containsString("scheduleCron")));
        mockMvc.perform(json(post(BASE), body(c1CollectionId, "0 0 3 * * *", 32, true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorDetail[0]").value("lookbackDays: 1~31일이어야 합니다."));
        mockMvc.perform(json(post(BASE), body(c1CollectionId, "0 0 3 * * *", 0, true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorDetail[0]").value("lookbackDays: 1~31일이어야 합니다."));
        mockMvc.perform(json(post(BASE), body(c1CollectionId, "0 0 3 * * *", 31, true)))
                .andExpect(status().isCreated());
        mockMvc.perform(json(
                        post(BASE), body(c1CollectionId, "0 0 3 * * *", 7, true).replace("MultiPolygon", "Polygon")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorDetail[0]").value(containsString("aoi")));
    }

    // 컬렉션마다 수집 대상은 하나다. 형식 오류는 중복보다 먼저 알린다
    @Test
    void secondTargetOfSameCollectionConflicts() throws Exception {
        mockMvc.perform(json(post(BASE), body(c1CollectionId, "0 0 3 * * *", 7, true)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorDetail[0]").value(containsString("이 컬렉션의 수집 대상이 이미 있습니다")));
        mockMvc.perform(json(post(BASE), body(c1CollectionId, "0 3 * * *", 7, true)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void collectionCannotBeChanged() throws Exception {
        long seoulTarget = seoulTargetId();

        mockMvc.perform(json(put(BASE + "/" + seoulTarget), body(uncollectableId, "0 0 3 * * *", 7, true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorDetail[0]").value(containsString("컬렉션은 바꿀 수 없습니다")));
    }

    // 실행 이력은 지워지고 수집한 영상은 일반 영상으로 남는다. 대기·실행 중이면 지울 수 없다
    @Test
    void deleteKeepsImagesAndRejectsActiveTarget() throws Exception {
        long seoulTarget = seoulTargetId();
        jdbc.update("""
                INSERT INTO satellite_image (collection_id, source_id, title, satellite, sensor_type, acquired_at,
                                             footprint, url, download_urls, collect_target_id)
                SELECT collection_id, 'collected-scene', 'collected-scene', 'SENTINEL-2C', 'OPTICAL', now(), aoi,
                       'https://example.test/collected-scene', ARRAY['https://example.test/collected-scene.tif'], target_id
                FROM collect_target WHERE target_id = ?
                """, seoulTarget);
        jdbc.update(
                "INSERT INTO collect_run (target_id, run_type, status) VALUES (?, 'ONE_OFF', 'QUEUED')", seoulTarget);

        mockMvc.perform(delete(BASE + "/" + seoulTarget)).andExpect(status().isConflict());

        jdbc.update("UPDATE collect_run SET status = 'CANCELED' WHERE target_id = ?", seoulTarget);
        mockMvc.perform(delete(BASE + "/" + seoulTarget)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM collect_run WHERE target_id = ?", Long.class, seoulTarget))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT collect_target_id IS NULL FROM satellite_image WHERE source_id = 'collected-scene'",
                        Boolean.class))
                .isTrue();
        mockMvc.perform(delete(BASE + "/-1")).andExpect(status().isNotFound());
    }

    private static String body(long collectionId, String cron, int lookbackDays, boolean enabled) {
        return """
                {"collectionId": %d, "aoi": %s, "scheduleCron": "%s", "lookbackDays": %d, "enabled": %s}
                """.formatted(collectionId, AOI, cron, lookbackDays, enabled);
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    // 컬렉션마다 대상이 하나라서, c1 대상을 새로 만드는 테스트는 seed 대상을 먼저 지운다(테스트 트랜잭션이 되돌린다)
    private void removeSeededTarget() {
        jdbc.update("DELETE FROM collect_target");
    }

    private long seoulTargetId() {
        return jdbc.queryForObject("SELECT min(target_id) FROM collect_target", Long.class);
    }

    private long collectionId(String code) {
        return jdbc.queryForObject(
                "SELECT collection_id FROM image_collection WHERE provider = 'earth-search' AND code = ?",
                Long.class,
                code);
    }
}
