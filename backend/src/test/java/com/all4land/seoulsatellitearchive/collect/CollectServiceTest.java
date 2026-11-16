package com.all4land.seoulsatellitearchive.collect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.all4land.seoulsatellitearchive.global.config.TestcontainersConfiguration;
import com.all4land.seoulsatellitearchive.global.exception.BusinessException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

// 실행 기록과 영상을 각각 커밋하는 동작을 보려고 테스트 트랜잭션을 쓰지 않고 직접 지운다
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class CollectServiceTest {

    @Autowired
    CollectService collectService;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    StacClient stacClient;

    private long targetId;

    @BeforeEach
    void setUp() {
        targetId = insertTarget("sentinel-2-c1-l2a");
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM satellite_image WHERE collect_target_id IS NOT NULL");
        jdbc.update("DELETE FROM collect_target");
    }

    @Test
    void insertsNewImagesWithFiles() throws IOException {
        stubSearch(items());

        CollectRun run = collectService.run(targetId);

        assertThat(run.getStatus()).isEqualTo(CollectRunStatus.SUCCEEDED);
        assertThat(run.getFound()).isEqualTo(2);
        assertThat(run.getInserted()).isEqualTo(2);
        assertThat(run.getWindowTo().minus(Duration.ofDays(7))).isEqualTo(run.getWindowFrom());
        assertThat(count("SELECT count(*) FROM satellite_image WHERE collect_target_id = ?", targetId))
                .isEqualTo(2);
        assertThat(count("""
                        SELECT count(*) FROM image_file f JOIN satellite_image i ON i.image_id = f.image_id
                        WHERE i.source_id = 'S2B_T52SCG_20261028T022157_L2A'
                        """)).isEqualTo(23);
    }

    @Test
    void nextRunContinuesFromLastSuccessAndSkipsUnchanged() throws IOException {
        stubSearch(items());
        CollectRun first = collectService.run(targetId);

        CollectRun second = collectService.run(targetId);

        assertThat(second.getWindowFrom()).isEqualTo(first.getWindowTo().minus(Duration.ofDays(7)));
        assertThat(second.getInserted()).isZero();
        assertThat(second.getUpdated()).isZero();
    }

    @Test
    void updatesOnlyImagesWithNewSourceUpdatedAt() throws IOException {
        stubSearch(items());
        collectService.run(targetId);
        List<JsonNode> changed = items();
        ((ObjectNode) changed.get(0).path("properties")).put("updated", "2026-11-01T00:00:00Z");
        stubSearch(changed);

        CollectRun run = collectService.run(targetId);

        assertThat(run.getUpdated()).isEqualTo(1);
        assertThat(count("SELECT count(*) FROM satellite_image WHERE source_updated_at = '2026-11-01T00:00:00Z'"))
                .isEqualTo(1);
    }

    @Test
    void skipsBrokenItemAndSavesOthers() throws IOException {
        List<JsonNode> items = items();
        ((ObjectNode) items.get(0)).remove("geometry");
        stubSearch(items);

        CollectRun run = collectService.run(targetId);

        assertThat(run.getStatus()).isEqualTo(CollectRunStatus.SUCCEEDED);
        assertThat(run.getInserted()).isEqualTo(1);
        assertThat(run.getSkipped()).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT source_id FROM collect_run_skip WHERE run_id = ?", String.class, run.getId()))
                .isEqualTo("S2B_T52SCG_20261028T022157_L2A");
    }

    @Test
    void failedSearchIsRecordedAndNotCountedAsSuccess() {
        when(stacClient.search(any(), any(), any(), any(), any()))
                .thenThrow(new ResourceAccessException("connection refused"));

        CollectRun failed = collectService.run(targetId);

        assertThat(failed.getStatus()).isEqualTo(CollectRunStatus.FAILED);
        assertThat(failed.getFailureReason()).isEqualTo("connection refused");
        assertThat(failed.getFinishedAt()).isNotNull();
        assertThat(count("SELECT count(*) FROM satellite_image WHERE collect_target_id = ?", targetId))
                .isZero();

        CollectRun next = collectService.run(targetId);
        assertThat(next.getWindowFrom()).isEqualTo(next.getWindowTo().minus(Duration.ofDays(7)));
    }

    @Test
    void rejectsCollectionWithoutMapper() {
        long legacyTarget = insertTarget("sentinel-2-l2a");

        assertThatThrownBy(() -> collectService.run(legacyTarget)).isInstanceOf(BusinessException.class);
        assertThat(count("SELECT count(*) FROM collect_run WHERE target_id = ?", legacyTarget))
                .isZero();
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

    private void stubSearch(List<JsonNode> items) {
        when(stacClient.search(any(), any(), any(), any(), any())).thenReturn(items);
    }

    private long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    private static List<JsonNode> items() throws IOException {
        try (InputStream in = CollectServiceTest.class.getResourceAsStream("/collect/sentinel-2-c1-l2a-search.json")) {
            List<JsonNode> items = new ArrayList<>();
            JsonMapper.shared().readTree(in).path("features").forEach(items::add);
            return items;
        }
    }
}
