package com.all4land.seoulsatellitearchive.collect.dto;

import com.all4land.seoulsatellitearchive.collect.CollectRun;
import com.all4land.seoulsatellitearchive.collect.CollectRunStatus;
import com.all4land.seoulsatellitearchive.collect.CollectRunType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record CollectRunResponse(
        Long runId,
        Long targetId,
        @Schema(description = "수집 대상의 컬렉션 이름") String collectionTitle,
        CollectRunType runType,
        CollectRunStatus status,

        @Schema(description = "워커가 시작하기 전이거나 시작 전에 취소됐으면 null일 수 있다")
        Instant windowFrom,

        @Schema(description = "워커가 시작하기 전이거나 시작 전에 취소됐으면 null일 수 있다")
        Instant windowTo,

        int found,
        int inserted,
        int updated,
        int skipped,
        @Schema(description = "실패했을 때만 값이 있다") String failureReason,
        Instant createdAt,
        Instant startedAt,

        @Schema(description = "실행 중 취소를 요청한 시각. 상태는 워커가 확인한 뒤 바뀐다")
        Instant cancelRequestedAt,

        Instant finishedAt) {

    public static CollectRunResponse from(CollectRun run) {
        return new CollectRunResponse(
                run.getId(),
                run.getTarget().getId(),
                run.getTarget().getCollection().getTitle(),
                run.getRunType(),
                run.getStatus(),
                run.getWindowFrom(),
                run.getWindowTo(),
                run.getFound(),
                run.getInserted(),
                run.getUpdated(),
                run.getSkipped(),
                run.getFailureReason(),
                run.getCreatedAt(),
                run.getStartedAt(),
                run.getCancelRequestedAt(),
                run.getFinishedAt());
    }
}
