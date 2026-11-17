package com.all4land.seoulsatellitearchive.collect.dto;

import com.all4land.seoulsatellitearchive.collect.CollectTarget;
import com.all4land.seoulsatellitearchive.collection.dto.CollectionResponse;
import com.fasterxml.jackson.annotation.JsonRawValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record CollectTargetResponse(
        Long targetId,
        CollectionResponse collection,

        @Schema(type = "object", description = "GeoJSON MultiPolygon") @JsonRawValue
        String aoi,

        String scheduleCron,

        @Schema(description = "다음 정기 수집 등록 시각. 지난 시각이면 다음 분에 등록된다. 꺼져 있으면 null")
        Instant nextRunAt,

        long lookbackDays,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt) {

    public static CollectTargetResponse from(CollectTarget target, Instant lastScheduledCreatedAt) {
        return new CollectTargetResponse(
                target.getId(),
                CollectionResponse.from(target.getCollection()),
                target.getAoiGeoJson(),
                target.getScheduleCron(),
                target.isEnabled() ? target.nextRunAt(lastScheduledCreatedAt) : null,
                target.getLookback().toDays(),
                target.isEnabled(),
                target.getCreatedAt(),
                target.getUpdatedAt());
    }
}
