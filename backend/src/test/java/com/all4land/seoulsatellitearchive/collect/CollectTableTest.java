package com.all4land.seoulsatellitearchive.collect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.all4land.seoulsatellitearchive.global.config.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class CollectTableTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EntityManager entityManager;

    private long targetId;

    @BeforeEach
    void setUp() {
        targetId = jdbc.queryForObject("""
                INSERT INTO collect_target (collection_id, aoi, schedule_cron)
                SELECT min(collection_id),
                       ST_GeomFromText('MULTIPOLYGON(((126.9 37.5, 127 37.5, 127 37.6, 126.9 37.5)))', 4326),
                       '0 0 3 * * *'
                FROM image_collection
                RETURNING target_id
                """, Long.class);
    }

    @Test
    void runWithSkipsMapsToEntities() {
        long runId = insertRun("SUCCEEDED");
        jdbc.update(
                "INSERT INTO collect_run_skip (run_id, source_id, reason) VALUES (?, 'item-1', 'footprint 없음')", runId);

        CollectRun run = entityManager.find(CollectRun.class, runId);

        assertThat(run.getStatus()).isEqualTo(CollectRunStatus.SUCCEEDED);
        assertThat(run.getRunType()).isEqualTo(CollectRunType.SCHEDULED);
        assertThat(run.getCreatedAt()).isNotNull();
        assertThat(run.getTarget().getLookback()).isEqualTo(Duration.ofDays(7));
        assertThat(run.getTarget().isEnabled()).isTrue();
        assertThat(run.getTarget().getAoi().getSRID()).isEqualTo(4326);
        assertThat(run.getSkips()).singleElement().satisfies(skip -> assertThat(skip.getSourceId())
                .isEqualTo("item-1"));
    }

    @Test
    void deletingTargetDeletesRunsAndSkips() {
        long runId = insertRun("FAILED");
        jdbc.update("INSERT INTO collect_run_skip (run_id, source_id, reason) VALUES (?, 'item-1', 'x')", runId);

        jdbc.update("DELETE FROM collect_target WHERE target_id = ?", targetId);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM collect_run", Long.class))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM collect_run_skip", Long.class))
                .isZero();
    }

    // 대기 중이거나 실행 중인 실행은 대상마다 하나다
    @Test
    void onlyOneActiveRunPerTarget() {
        insertRun("SUCCEEDED");
        insertRun("RUNNING");

        assertThatThrownBy(() -> insertRun("QUEUED")).isInstanceOf(DataIntegrityViolationException.class);
    }

    // 조회 구간과 시작 시각은 시작하기 전(대기, 시작 전 취소)에만 비어 있을 수 있다
    @Test
    void startedRunNeedsWindowAndStartTime() {
        jdbc.update(
                "INSERT INTO collect_run (target_id, run_type, status) VALUES (?, 'ONE_OFF', 'CANCELED')", targetId);

        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO collect_run (target_id, run_type, status) VALUES (?, 'ONE_OFF', 'RUNNING')",
                        targetId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private long insertRun(String status) {
        return jdbc.queryForObject("""
                INSERT INTO collect_run (target_id, run_type, status, window_from, window_to, started_at)
                VALUES (?, 'SCHEDULED', ?, now() - INTERVAL '7 days', now(), now())
                RETURNING run_id
                """, Long.class, targetId, status);
    }
}
