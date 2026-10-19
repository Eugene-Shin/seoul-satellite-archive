package com.all4land.seoulsatellitearchive.image.dto;

import com.all4land.seoulsatellitearchive.image.OrbitDirection;
import com.all4land.seoulsatellitearchive.image.SatelliteImage;
import com.all4land.seoulsatellitearchive.image.SensorType;
import com.fasterxml.jackson.annotation.JsonRawValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ImageDetailResponse {

    private final Long imageId;
    private final String providerCode;
    private final String sourceId;
    private final String title;
    private final String satellite;
    private final List<String> instruments;
    private final SensorType sensorType;
    private final Instant acquiredAt;
    private final BigDecimal gsdMeters;
    private final BigDecimal cloudCover;
    private final OrbitDirection orbitDirection;

    @Schema(type = "object", description = "GeoJSON MultiPolygon")
    @JsonRawValue
    private final String footprint;

    private final String url;
    private final List<String> downloadUrls;
    private final Instant sourceUpdatedAt;
    private final Map<String, Object> rawMetadata;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static ImageDetailResponse from(SatelliteImage image) {
        return new ImageDetailResponse(
                image.getId(),
                image.getProvider().getCode(),
                image.getSourceId(),
                image.getTitle(),
                image.getSatellite(),
                image.getInstruments() == null ? null : List.of(image.getInstruments()),
                image.getSensorType(),
                image.getAcquiredAt(),
                image.getGsdMeters(),
                image.getCloudCover(),
                image.getOrbitDirection(),
                image.getFootprintGeoJson(),
                image.getUrl(),
                List.of(image.getDownloadUrls()),
                image.getSourceUpdatedAt(),
                image.getRawMetadata(),
                image.getCreatedAt(),
                image.getUpdatedAt());
    }
}
