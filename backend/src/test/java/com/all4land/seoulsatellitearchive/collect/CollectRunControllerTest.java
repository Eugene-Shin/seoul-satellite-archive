package com.all4land.seoulsatellitearchive.collect;

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

    @BeforeEach
    void setUp() {
        targetId = insertTarget("sentinel-2-c1-l2a");
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
