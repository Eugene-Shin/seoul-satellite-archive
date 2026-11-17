package com.all4land.seoulsatellitearchive.collect;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

// 1분마다 실행할 시각이 된 대상의 정기 실행을 대기열에 넣고, 하트비트가 끊긴 실행을 정리한다.
// 테스트는 app.collect.scheduler.cron을 "-"로 두어 끄고 메서드를 직접 부른다
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectScheduler {

    static final Duration STALE_AFTER = Duration.ofMinutes(10);
    static final String STALE_REASON = "실행 중 응답이 끊겨 중단된 것으로 처리했습니다.";

    private final CollectTargetRepository collectTargetRepository;
    private final CollectRunRepository collectRunRepository;
    private final CollectService collectService;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    @Scheduled(cron = "${app.collect.scheduler.cron:0 * * * * *}", zone = "Asia/Seoul")
    public void tick() {
        Instant now = clock.instant();
        failStaleRuns(now);
        enqueueDueRuns(now);
    }

    void failStaleRuns(Instant now) {
        int failed = transactionTemplate.execute(
                status -> collectRunRepository.failStale(now.minus(STALE_AFTER), now, STALE_REASON));
        if (failed > 0) {
            log.warn("하트비트가 끊긴 수집 실행 {}건을 실패로 정리했습니다.", failed);
        }
    }

    // 대상마다 트랜잭션을 나눠서 한 대상의 오류가 다른 대상의 등록을 되돌리지 않게 한다
    void enqueueDueRuns(Instant now) {
        for (Long targetId : collectTargetRepository.findEnabledIds()) {
            try {
                transactionTemplate.executeWithoutResult(status -> enqueueIfDue(targetId, now));
            } catch (RuntimeException e) {
                log.warn("정기 수집을 등록하지 못했습니다 targetId={}", targetId, e);
            }
        }
    }

    // 마지막 정기 등록 다음의 cron 시각이 지났으면 한 번 등록한다. 놓친 시각이 여러 번이어도 한 번이면
    // 조회 구간이 마지막 성공에서 이어져 빠진 기간을 메운다. 대기·실행 중이면 끝난 뒤 다음 분에 등록한다
    private void enqueueIfDue(Long targetId, Instant now) {
        if (collectTargetRepository.lockEnabled(targetId).isEmpty()) {
            return;
        }
        CollectTarget target = collectTargetRepository.findById(targetId).orElseThrow();
        Instant next = target.nextRunAt(
                collectRunRepository.findLastScheduledCreatedAt(targetId).orElse(null));
        if (next == null || next.isAfter(now) || collectRunRepository.existsActive(targetId)) {
            return;
        }
        collectService.enqueue(targetId, CollectRunType.SCHEDULED, null, null, now);
    }
}
