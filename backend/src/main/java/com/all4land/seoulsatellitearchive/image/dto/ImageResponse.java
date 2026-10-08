package com.all4land.seoulsatellitearchive.image.dto;

import com.all4land.seoulsatellitearchive.image.OrbitDirection;
import com.all4land.seoulsatellitearchive.image.SatelliteImage;
import com.all4land.seoulsatellitearchive.image.SensorType;
import com.all4land.seoulsatellitearchive.provider.dto.ProviderResponse;
import com.fasterxml.jackson.annotation.JsonRawValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ImageResponse(
        Long imageId,
        ProviderResponse provider,
        String title,
        String satellite,
        List<String> instruments,
        SensorType sensorType,
        Instant acquiredAt,
        BigDecimal gsdMeters,
        BigDecimal cloudCover,
        OrbitDirection orbitDirection,

        @Schema(type = "object", description = "GeoJSON MultiPolygon") @JsonRawValue
        String footprint,

        List<String> downloadUrls,
        Instant sourceUpdatedAt) {

    public static ImageResponse from(SatelliteImage image) {
        return new ImageResponse(
                image.getId(),
                ProviderResponse.from(image.getProvider()),
                image.getTitle(),
                image.getSatellite(),
                image.getInstruments() == null ? null : List.of(image.getInstruments()),
                image.getSensorType(),
                image.getAcquiredAt(),
                image.getGsdMeters(),
                image.getCloudCover(),
                image.getOrbitDirection(),
                image.getFootprintGeoJson(),
                List.of(image.getDownloadUrls()),
                image.getSourceUpdatedAt());
    }
}
