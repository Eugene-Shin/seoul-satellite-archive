package com.all4land.seoulsatellitearchive.collect;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 테스트는 대기열을 직접 처리하므로 app.collect.worker.enabled=false로 끈다
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.collect.worker.enabled", havingValue = "true", matchIfMissing = true)
public class CollectWorker {

    private final CollectService collectService;

    @Scheduled(fixedDelayString = "${app.collect.worker.poll-interval:PT5S}")
    public void poll() {
        while (collectService.processNext()) {
            // 대기열이 빌 때까지 하나씩 처리한다
        }
    }
}
