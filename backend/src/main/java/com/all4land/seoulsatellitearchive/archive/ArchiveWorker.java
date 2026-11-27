package com.all4land.seoulsatellitearchive.archive;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 한 번에 하나씩 받는다. 테스트는 대기열을 직접 처리하므로 app.archive.worker.enabled=false로 끈다
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.archive.worker.enabled", havingValue = "true", matchIfMissing = true)
public class ArchiveWorker {

    private final ArchiveService archiveService;

    @Scheduled(fixedDelayString = "${app.archive.worker.poll-interval:PT10S}")
    public void poll() {
        while (archiveService.processNext()) {
            // 대기열이 빌 때까지 하나씩 처리한다
        }
    }
}
