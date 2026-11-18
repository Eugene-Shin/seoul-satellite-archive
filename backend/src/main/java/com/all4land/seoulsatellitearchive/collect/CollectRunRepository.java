package com.all4land.seoulsatellitearchive.collect;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface CollectRunRepository extends JpaRepository<CollectRun, Long> {

    // 최근 등록이 위로. 커서보다 앞선 실행만 limit개. targetId가 null이면 모든 대상
    @Query("""
            select r from CollectRun r
            join fetch r.target t
            join fetch t.collection
            where (:targetId is null or t.id = :targetId)
              and (r.createdAt < :cursorCreatedAt or (r.createdAt = :cursorCreatedAt and r.id < :cursorRunId))
            order by r.createdAt desc, r.id desc
            """)
    List<CollectRun> findPage(Long targetId, Instant cursorCreatedAt, long cursorRunId, Limit limit);

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

    // 워커가 동시에 가져간 경우 0건이 바뀐다. 일괄 UPDATE는 영속성 컨텍스트를 거치지 않아서 뒤에 다시 읽도록 비운다
    @Modifying(clearAutomatically = true)
    @Query("""
            update CollectRun r
            set r.status = com.all4land.seoulsatellitearchive.collect.CollectRunStatus.CANCELED, r.finishedAt = :now
            where r.id = :runId
              and r.status = com.all4land.seoulsatellitearchive.collect.CollectRunStatus.QUEUED
            """)
    int cancelQueued(Long runId, Instant now);

    @Modifying(clearAutomatically = true)
    @Query("""
            update CollectRun r
            set r.cancelRequestedAt = :now
            where r.id = :runId
              and r.status = com.all4land.seoulsatellitearchive.collect.CollectRunStatus.RUNNING
              and r.cancelRequestedAt is null
            """)
    int requestCancelRunning(Long runId, Instant now);

    // 실행 중 확인 지점마다 부른다. 하트비트는 beatBefore보다 오래됐을 때만 갱신해서 쓰기를 줄이고, 취소 요청 여부를 함께 돌려준다
    @Transactional
    @Query(value = """
            WITH beat AS (
                UPDATE collect_run SET heartbeat_at = :now
                WHERE run_id = :runId AND heartbeat_at < :beatBefore
            )
            SELECT cancel_requested_at IS NOT NULL FROM collect_run WHERE run_id = :runId
            """, nativeQuery = true)
    boolean recordHeartbeatAndCheckCancel(Long runId, Instant now, Instant beatBefore);

    @Query("""
            select max(r.createdAt) from CollectRun r
            where r.target.id = :targetId
              and r.runType = com.all4land.seoulsatellitearchive.collect.CollectRunType.SCHEDULED
            """)
    Optional<Instant> findLastScheduledCreatedAt(Long targetId);

    @Query("""
            select count(r) > 0 from CollectRun r
            where r.target.id = :targetId
              and r.status in (com.all4land.seoulsatellitearchive.collect.CollectRunStatus.QUEUED,
                               com.all4land.seoulsatellitearchive.collect.CollectRunStatus.RUNNING)
            """)
    boolean existsActive(Long targetId);

    // 서버가 죽어 하트비트가 멈춘 실행을 정리해야 같은 대상이 다시 실행될 수 있다
    @Modifying(clearAutomatically = true)
    @Query("""
            update CollectRun r
            set r.status = com.all4land.seoulsatellitearchive.collect.CollectRunStatus.FAILED,
                r.failureReason = :reason,
                r.finishedAt = :now
            where r.status = com.all4land.seoulsatellitearchive.collect.CollectRunStatus.RUNNING
              and r.heartbeatAt < :staleBefore
            """)
    int failStale(Instant staleBefore, Instant now, String reason);
}
