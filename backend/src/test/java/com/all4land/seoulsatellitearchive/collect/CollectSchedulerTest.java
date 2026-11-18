package com.all4land.seoulsatellitearchive.collect;

import static org.assertj.core.api.Assertions.assertThat;

import com.all4land.seoulsatellitearchive.global.config.TestcontainersConfiguration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

// cron은 매일 한국 시간 03:00. 대상은 2026-11-16 12:00(KST)에 만든 것으로 둔다
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class CollectSchedulerTest {

    private static final String C1 = "sentinel-2-c1-l2a";
    private static final String LEGACY_S2 = "sentinel-2-l2a";
    private static final String HLS = "C2021957657-LPCLOUD";

    private static final Instant NOV17_0259_KST = Instant.parse("2026-11-16T17:59:00Z");
    private static final Instant NOV17_0300_KST = Instant.parse("2026-11-16T18:00:30Z");
    private static final Instant NOV17_0301_KST = Instant.parse("2026-11-16T18:01:00Z");

    @Autowired
    CollectScheduler scheduler;

    @Autowired
    JdbcTemplate jdbc;

    private long targetId;

    // seed 대상은 실제 시각 기준으로 만들어져 있어서 지우고, 시각을 정한 대상을 새로 만든다(테스트 트랜잭션이 되돌린다).
    // 컬렉션마다 대상이 하나라서 대상이 더 필요하면 다른 컬렉션으로 만든다
    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM collect_target");
        targetId = insertTarget(C1, "0 0 3 * * *");
    }

    @Test
    void enqueuesScheduledRunOnlyAfterCronTime() {
        scheduler.enqueueDueRuns(NOV17_0259_KST);
        assertThat(scheduledRuns()).isZero();

        scheduler.enqueueDueRuns(NOV17_0300_KST);
        assertThat(scheduledRuns()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM collect_run WHERE target_id = ?", String.class, targetId))
                .isEqualTo("QUEUED");
    }

    // 등록 시각이 기준이 되어 다음 cron 시각(다음 날 03:00) 전에는 다시 등록하지 않는다
    @Test
    void doesNotEnqueueAgainUntilNextCronTime() {
        scheduler.enqueueDueRuns(NOV17_0300_KST);
        finishRuns();

        scheduler.enqueueDueRuns(NOV17_0301_KST);
        scheduler.enqueueDueRuns(Instant.parse("2026-11-17T17:59:00Z"));
        assertThat(scheduledRuns()).isEqualTo(1);

        scheduler.enqueueDueRuns(Instant.parse("2026-11-17T18:00:00Z"));
        assertThat(scheduledRuns()).isEqualTo(2);
    }

    // 놓친 시각이 여러 번이어도 한 번만 등록한다. 조회 구간은 마지막 성공에서 이어진다
    @Test
    void enqueuesOnceForManyMissedCronTimes() {
        scheduler.enqueueDueRuns(Instant.parse("2026-11-20T01:00:00Z"));

        assertThat(scheduledRuns()).isEqualTo(1);
    }

    // 대기·실행 중인 실행이 끝나면 다음 분에 등록한다
    @Test
    void waitsWhileAnotherRunIsActive() {
        jdbc.update("INSERT INTO collect_run (target_id, run_type, status) VALUES (?, 'ONE_OFF', 'QUEUED')", targetId);

        scheduler.enqueueDueRuns(NOV17_0300_KST);
        assertThat(scheduledRuns()).isZero();

        finishRuns();
        scheduler.enqueueDueRuns(NOV17_0301_KST);
        assertThat(scheduledRuns()).isEqualTo(1);
    }

    @Test
    void skipsDisabledTargetAndInvalidCronWithoutStoppingOthers() {
        long disabled = insertTarget(LEGACY_S2, "0 0 3 * * *");
        jdbc.update("UPDATE collect_target SET enabled = false WHERE target_id = ?", disabled);
        long invalidCron = insertTarget(HLS, "not a cron");

        scheduler.enqueueDueRuns(NOV17_0300_KST);

        assertThat(count("SELECT count(*) FROM collect_run WHERE target_id = ?", disabled))
                .isZero();
        assertThat(count("SELECT count(*) FROM collect_run WHERE target_id = ?", invalidCron))
                .isZero();
        assertThat(scheduledRuns()).isEqualTo(1);
    }

    @Test
    void failsOnlyRunsWithStaleHeartbeat() {
        Instant now = Instant.parse("2026-11-17T01:00:00Z");
        long stale =
                insertRunning(targetId, now.minus(CollectScheduler.STALE_AFTER).minusSeconds(1));
        long alive = insertRunning(insertTarget(LEGACY_S2, "0 0 3 * * *"), now.minusSeconds(120));

        scheduler.failStaleRuns(now);

        assertThat(jdbc.queryForObject("SELECT status FROM collect_run WHERE run_id = ?", String.class, stale))
                .isEqualTo("FAILED");
        assertThat(jdbc.queryForObject("SELECT failure_reason FROM collect_run WHERE run_id = ?", String.class, stale))
                .isEqualTo(CollectScheduler.STALE_REASON);
        assertThat(jdbc.queryForObject("SELECT status FROM collect_run WHERE run_id = ?", String.class, alive))
                .isEqualTo("RUNNING");
    }

    private long scheduledRuns() {
        return count("SELECT count(*) FROM collect_run WHERE target_id = ? AND run_type = 'SCHEDULED'", targetId);
    }

    private void finishRuns() {
        jdbc.update("UPDATE collect_run SET status = 'CANCELED' WHERE status IN ('QUEUED', 'RUNNING')");
    }

    private long insertRunning(long target, Instant heartbeatAt) {
        return jdbc.queryForObject("""
                INSERT INTO collect_run (target_id, run_type, status, window_from, window_to, started_at, heartbeat_at)
                VALUES (?, 'SCHEDULED', 'RUNNING', now() - INTERVAL '7 days', now(), now(), ?::timestamptz)
                RETURNING run_id
                """, Long.class, target, heartbeatAt.toString());
    }

    private long count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    private long insertTarget(String collectionCode, String cron) {
        return jdbc.queryForObject("""
                INSERT INTO collect_target (collection_id, aoi, schedule_cron, created_at)
                SELECT collection_id,
                       ST_GeomFromText('MULTIPOLYGON(((126.9 37.5, 127.1 37.5, 127.1 37.6, 126.9 37.5)))', 4326),
                       ?, '2026-11-16T12:00:00+09:00'
                FROM image_collection
                WHERE code = ?
                RETURNING target_id
                """, Long.class, cron, collectionCode);
    }
}
