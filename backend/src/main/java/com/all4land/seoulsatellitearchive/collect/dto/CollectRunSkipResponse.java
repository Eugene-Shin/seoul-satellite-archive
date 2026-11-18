package com.all4land.seoulsatellitearchive.collect.dto;

import com.all4land.seoulsatellitearchive.collect.CollectRunSkip;
import io.swagger.v3.oas.annotations.media.Schema;

public record CollectRunSkipResponse(
        @Schema(description = "출처가 붙인 영상 ID") String sourceId,
        @Schema(description = "건너뛴 사유") String reason) {

    public static CollectRunSkipResponse from(CollectRunSkip skip) {
        return new CollectRunSkipResponse(skip.getSourceId(), skip.getReason());
    }
}
