package com.all4land.seoulsatellitearchive.collect.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.OffsetDateTime;

public record CollectRunRequest(
        @Schema(
                description = "선택. windowTo와 함께 준다. 없으면 마지막으로 성공한 정기 실행의 windowTo(없으면 지금)에서 수집 대상의 lookback만큼 앞부터 지금까지",
                example = "2026-10-01T00:00:00+09:00")
        OffsetDateTime windowFrom,

        @Schema(description = "선택. windowFrom보다 뒤", example = "2026-11-01T00:00:00+09:00")
        OffsetDateTime windowTo) {

    public Instant windowFromInstant() {
        return windowFrom == null ? null : windowFrom.toInstant();
    }

    public Instant windowToInstant() {
        return windowTo == null ? null : windowTo.toInstant();
    }
}
