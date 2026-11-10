package com.all4land.seoulsatellitearchive.image.dto;

import com.all4land.seoulsatellitearchive.collection.dto.CollectionResponse;
import com.all4land.seoulsatellitearchive.image.ImageFile;
import com.all4land.seoulsatellitearchive.image.ImageResolution;
import com.all4land.seoulsatellitearchive.image.ImageSar;
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
    private final CollectionResponse collection;
    private final String sourceId;
    private final String title;
    private final String satellite;
    private final List<String> instruments;
    private final SensorType sensorType;
    private final Instant acquiredAt;
    private final Instant acquiredEndAt;

    @Schema(description = "파일의 픽셀 간격 범위. 없는 영상은 센서 해상도로 대체. 둘 다 없으면 null")
    private final ResolutionResponse resolution;

    private final BigDecimal cloudCover;
    private final OrbitDirection orbitDirection;

    @Schema(description = "SAR 정보가 없으면 null")
    private final SarResponse sar;

    @Schema(type = "object", description = "GeoJSON MultiPolygon")
    @JsonRawValue
    private final String footprint;

    private final String url;
    private final List<String> downloadUrls;
    private final List<ImageFileResponse> files;
    private final Instant sourceUpdatedAt;
    private final Map<String, Object> rawMetadata;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static ImageDetailResponse from(
            SatelliteImage image, ImageSar sar, ImageResolution resolution, List<ImageFile> files) {
        return new ImageDetailResponse(
                image.getId(),
                image.getCollection().getProvider().getCode(),
                CollectionResponse.from(image.getCollection()),
                image.getSourceId(),
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
                image.getUrl(),
                List.of(image.getDownloadUrls()),
                files.stream().map(ImageFileResponse::from).toList(),
                image.getSourceUpdatedAt(),
                image.getRawMetadata(),
                image.getCreatedAt(),
                image.getUpdatedAt());
    }
}
