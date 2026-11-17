package com.all4land.seoulsatellitearchive.collect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.all4land.seoulsatellitearchive.global.config.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
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

    @Autowired
    CollectRunRepository collectRunRepository;

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

    // 하트비트는 1분이 지났을 때만 갱신하고, 취소 요청 여부를 함께 돌려준다
    @Test
    void heartbeatIsRecordedOnlyAfterIntervalAndReportsCancel() {
        long runId = insertRun("RUNNING");
        Instant start = Instant.parse("2026-11-17T01:00:00Z");
        jdbc.update("UPDATE collect_run SET heartbeat_at = ?::timestamptz WHERE run_id = ?", start.toString(), runId);

        Instant later = start.plusSeconds(90);
        assertThat(collectRunRepository.recordHeartbeatAndCheckCancel(runId, later, later.minusSeconds(60)))
                .isFalse();
        assertThat(heartbeatOf(runId)).isEqualTo(later);

        Instant soon = later.plusSeconds(30);
        jdbc.update("UPDATE collect_run SET cancel_requested_at = now() WHERE run_id = ?", runId);
        assertThat(collectRunRepository.recordHeartbeatAndCheckCancel(runId, soon, soon.minusSeconds(60)))
                .isTrue();
        assertThat(heartbeatOf(runId)).isEqualTo(later);
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

    private Instant heartbeatOf(long runId) {
        return jdbc.queryForObject("SELECT heartbeat_at FROM collect_run WHERE run_id = ?", Timestamp.class, runId)
                .toInstant();
    }

    private long insertRun(String status) {
        return jdbc.queryForObject("""
                INSERT INTO collect_run (target_id, run_type, status, window_from, window_to, started_at)
                VALUES (?, 'SCHEDULED', ?, now() - INTERVAL '7 days', now(), now())
                RETURNING run_id
                """, Long.class, targetId, status);
    }
}
