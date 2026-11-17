package com.all4land.seoulsatellitearchive.collect;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface CollectRunRepository extends JpaRepository<CollectRun, Long> {

    @Query("""
            select r from CollectRun r
            where r.target.id = :targetId
              and r.runType = com.all4land.seoulsatellitearchive.collect.CollectRunType.SCHEDULED
              and r.status = com.all4land.seoulsatellitearchive.collect.CollectRunStatus.SUCCEEDED
            order by r.windowTo desc
            limit 1
            """)
    Optional<CollectRun> findLastScheduledSuccess(Long targetId);

    // 서버가 여러 대여도 한 대만 가져간다. 다른 서버가 잡은 행은 기다리지 않고 건너뛴다
    @Query(value = """
            SELECT * FROM collect_run
            WHERE status = 'QUEUED'
            ORDER BY created_at
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<CollectRun> lockNextQueued();

    // 워커가 동시에 가져간 경우 0건이 바뀐다
    @Modifying
    @Query("""
            update CollectRun r
            set r.status = com.all4land.seoulsatellitearchive.collect.CollectRunStatus.CANCELED, r.finishedAt = :now
            where r.id = :runId
              and r.status = com.all4land.seoulsatellitearchive.collect.CollectRunStatus.QUEUED
            """)
    int cancelQueued(Long runId, Instant now);

    @Modifying
    @Query("""
            update CollectRun r
            set r.cancelRequestedAt = :now
            where r.id = :runId
              and r.status = com.all4land.seoulsatellitearchive.collect.CollectRunStatus.RUNNING
              and r.cancelRequestedAt is null
            """)
    int requestCancelRunning(Long runId, Instant now);

    @Query("select count(r) > 0 from CollectRun r where r.id = :runId and r.cancelRequestedAt is not null")
    boolean isCancelRequested(Long runId);
}
