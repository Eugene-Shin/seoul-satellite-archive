package com.all4land.seoulsatellitearchive.collect.dto;

import com.all4land.seoulsatellitearchive.collect.AoiPreset;
import com.fasterxml.jackson.annotation.JsonRawValue;
import io.swagger.v3.oas.annotations.media.Schema;

public record AoiPresetResponse(
        String code,
        String name,

        @Schema(type = "object", description = "GeoJSON MultiPolygon") @JsonRawValue
        String aoi,

        @Schema(description = "화면에 표시할 출처 문구") String source) {

    public static AoiPresetResponse from(AoiPreset preset) {
        return new AoiPresetResponse(preset.getCode(), preset.getName(), preset.getGeomGeoJson(), preset.getSource());
    }
}
