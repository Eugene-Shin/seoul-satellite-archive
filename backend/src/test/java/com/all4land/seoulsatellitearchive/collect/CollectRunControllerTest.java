package com.all4land.seoulsatellitearchive.collect;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// 워커는 꺼져 있어서(application-test.yml) 등록한 실행은 대기 상태로 남는다
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class CollectRunControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    private long targetId;

    // 컬렉션마다 대상이 하나라서 c1은 seed 서울 대상을 쓴다
    @BeforeEach
    void setUp() {
        targetId = jdbc.queryForObject("""
                SELECT t.target_id FROM collect_target t
                JOIN image_collection c ON c.collection_id = t.collection_id
                WHERE c.code = 'sentinel-2-c1-l2a'
                """, Long.class);
    }

    @Test
    void enqueueReturnsAcceptedQueuedOneOffRun() throws Exception {
        mockMvc.perform(post(runsOf(targetId)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value("COMMON_202_001"))
                .andExpect(jsonPath("$.data.runId").isNumber())
                .andExpect(jsonPath("$.data.targetId").value(targetId))
                .andExpect(jsonPath("$.data.runType").value("ONE_OFF"))
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andExpect(jsonPath("$.data.windowFrom").doesNotExist())
                .andExpect(jsonPath("$.data.failureReason").isEmpty());
    }

    @Test
    void enqueueKeepsGivenWindow() throws Exception {
        mockMvc.perform(post(runsOf(targetId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"windowFrom": "2025-01-01T09:00:00+09:00", "windowTo": "2025-02-01T09:00:00+09:00"}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.windowFrom").value("2025-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.data.windowTo").value("2025-02-01T00:00:00Z"));
    }

    @Test
    void enqueueRejectsBadWindow() throws Exception {
        mockMvc.perform(post(runsOf(targetId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"windowFrom\": \"2025-01-01T00:00:00Z\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(runsOf(targetId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"windowFrom": "2025-02-01T00:00:00Z", "windowTo": "2025-01-01T00:00:00Z"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void enqueueRejectsUnknownTargetUnsupportedCollectionAndActiveRun() throws Exception {
        mockMvc.perform(post(runsOf(-1))).andExpect(status().isNotFound());
        mockMvc.perform(post(runsOf(insertTarget("sentinel-2-l2a")))).andExpect(status().isBadRequest());

        mockMvc.perform(post(runsOf(targetId))).andExpect(status().isAccepted());
        mockMvc.perform(post(runsOf(targetId))).andExpect(status().isConflict());
    }

    @Test
    void getRunReturnsStatus() throws Exception {
        long runId = enqueue();

        mockMvc.perform(get("/api/collect-runs/" + runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.runId").value(runId))
                .andExpect(jsonPath("$.data.status").value("QUEUED"));
        mockMvc.perform(get("/api/collect-runs/-1")).andExpect(status().isNotFound());
    }

    @Test
    void cancelQueuedRunThenCancelAgainConflicts() throws Exception {
        long runId = enqueue();

        mockMvc.perform(post("/api/collect-runs/" + runId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELED"))
                .andExpect(jsonPath("$.data.finishedAt").isNotEmpty());
        mockMvc.perform(post("/api/collect-runs/" + runId + "/cancel")).andExpect(status().isConflict());
        mockMvc.perform(post("/api/collect-runs/-1/cancel")).andExpect(status().isNotFound());
    }

    // 최근 등록이 위로. 커서로 이어 받고, targetId를 주면 그 대상만
    @Test
    void listsRunsNewestFirstWithCursorAndTargetFilter() throws Exception {
        long legacyTarget = insertTarget("sentinel-2-l2a");
        long oldest = insertFinishedRun(targetId, "2026-11-01T00:00:00Z");
        long middle = insertFinishedRun(legacyTarget, "2026-11-02T00:00:00Z");
        long newest = insertFinishedRun(targetId, "2026-11-03T00:00:00Z");

        String first = mockMvc.perform(get("/api/collect-runs").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].runId", contains((int) newest, (int) middle)))
                .andExpect(jsonPath("$.data.content[0].collectionTitle").value("Sentinel-2 Collection 1 Level-2A"))
                .andExpect(jsonPath("$.data.nextCursor").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String cursor = JsonPath.read(first, "$.data.nextCursor");

        mockMvc.perform(get("/api/collect-runs").param("size", "2").param("cursor", cursor))
                .andExpect(jsonPath("$.data.content[*].runId", contains((int) oldest)))
                .andExpect(jsonPath("$.data.nextCursor").doesNotExist());
        mockMvc.perform(get("/api/collect-runs").param("targetId", String.valueOf(legacyTarget)))
                .andExpect(jsonPath("$.data.content[*].runId", contains((int) middle)));
        mockMvc.perform(get("/api/collect-runs").param("cursor", "not-a-cursor"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsSkipsOfRun() throws Exception {
        long runId = insertFinishedRun(targetId, "2026-11-01T00:00:00Z");
        jdbc.update(
                "INSERT INTO collect_run_skip (run_id, source_id, reason) VALUES (?, 'item-1', 'footprint 없음')", runId);

        mockMvc.perform(get("/api/collect-runs/" + runId + "/skips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sourceId").value("item-1"))
                .andExpect(jsonPath("$.data[0].reason").value("footprint 없음"));
        mockMvc.perform(get("/api/collect-runs/-1/skips")).andExpect(status().isNotFound());
    }

    private long insertFinishedRun(long target, String createdAt) {
        return jdbc.queryForObject("""
                INSERT INTO collect_run (target_id, run_type, status, window_from, window_to, created_at, started_at, finished_at)
                VALUES (?, 'ONE_OFF', 'SUCCEEDED', now() - INTERVAL '7 days', now(), ?::timestamptz, now(), now())
                RETURNING run_id
                """, Long.class, target, createdAt);
    }

    private long enqueue() throws Exception {
        String body = mockMvc.perform(post(runsOf(targetId)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.runId")).longValue();
    }

    private static String runsOf(long targetId) {
        return "/api/collect-targets/" + targetId + "/runs";
    }

    private long insertTarget(String collectionCode) {
        return jdbc.queryForObject("""
                INSERT INTO collect_target (collection_id, aoi, schedule_cron)
                SELECT collection_id,
                       ST_GeomFromText('MULTIPOLYGON(((126.9 37.5, 127.1 37.5, 127.1 37.6, 126.9 37.5)))', 4326),
                       '0 0 3 * * *'
                FROM image_collection
                WHERE provider = 'earth-search' AND code = ?
                RETURNING target_id
                """, Long.class, collectionCode);
    }
}
