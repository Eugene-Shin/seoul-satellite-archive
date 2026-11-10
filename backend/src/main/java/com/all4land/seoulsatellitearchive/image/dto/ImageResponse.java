package com.all4land.seoulsatellitearchive.image.dto;

import com.all4land.seoulsatellitearchive.collection.dto.CollectionResponse;
import com.all4land.seoulsatellitearchive.image.ImageResolution;
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

        @Schema(description = "파일의 픽셀 간격 범위. 없는 영상은 센서 해상도로 대체. 둘 다 없으면 null")
        ResolutionResponse resolution,

        BigDecimal cloudCover,
        OrbitDirection orbitDirection,
        @Schema(description = "SAR 정보가 없으면 null") SarResponse sar,

        @Schema(type = "object", description = "GeoJSON MultiPolygon") @JsonRawValue
        String footprint,

        Instant sourceUpdatedAt,
        @Schema(description = "썸네일이 없으면 null") String thumbnailUrl,
        @Schema(description = "지도에 겹칠 COG. 없으면 null") String overlayUrl) {

    public static ImageResponse from(
            SatelliteImage image, ImageSar sar, ImageResolution resolution, String thumbnailUrl, String overlayUrl) {
        return new ImageResponse(
                image.getId(),
                ProviderResponse.from(image.getCollection().getProvider()),
                CollectionResponse.from(image.getCollection()),
                image.getTitle(),
                image.getSatellite(),
                image.getInstruments() == null ? null : List.of(image.getInstruments()),
                image.getSensorType(),
                image.getAcquiredAt(),
                image.getAcquiredEndAt(),
                ResolutionResponse.from(resolution),
                image.getCloudCover(),
                image.getOrbitDirection(),
                sar == null ? null : SarResponse.from(sar),
                image.getFootprintGeoJson(),
                image.getSourceUpdatedAt(),
                thumbnailUrl,
                overlayUrl);
    }
}
