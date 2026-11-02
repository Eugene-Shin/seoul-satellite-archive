package com.all4land.seoulsatellitearchive.image.dto;

import com.all4land.seoulsatellitearchive.collection.dto.CollectionResponse;
import com.all4land.seoulsatellitearchive.image.ImageSar;
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
        CollectionResponse collection,
        String title,
        String satellite,
        List<String> instruments,
        SensorType sensorType,
        Instant acquiredAt,
        Instant acquiredEndAt,
        BigDecimal gsdMeters,
        BigDecimal cloudCover,
        OrbitDirection orbitDirection,
        @Schema(description = "SAR 정보가 없으면 null") SarResponse sar,

        @Schema(type = "object", description = "GeoJSON MultiPolygon") @JsonRawValue
        String footprint,

        List<String> downloadUrls,
        Instant sourceUpdatedAt) {

    public static ImageResponse from(SatelliteImage image, ImageSar sar) {
        return new ImageResponse(
                image.getId(),
                ProviderResponse.from(image.getProvider()),
                CollectionResponse.from(image.getCollection()),
                image.getTitle(),
                image.getSatellite(),
                image.getInstruments() == null ? null : List.of(image.getInstruments()),
                image.getSensorType(),
                image.getAcquiredAt(),
                image.getAcquiredEndAt(),
                image.getGsdMeters(),
                image.getCloudCover(),
                image.getOrbitDirection(),
                sar == null ? null : SarResponse.from(sar),
                image.getFootprintGeoJson(),
                List.of(image.getDownloadUrls()),
                image.getSourceUpdatedAt());
    }
}
