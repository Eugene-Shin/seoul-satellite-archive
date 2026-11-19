package com.all4land.seoulsatellitearchive.image.dto;

import com.all4land.seoulsatellitearchive.global.geo.GeoJsonMultiPolygon;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.MultiPolygon;
import tools.jackson.databind.JsonNode;

@Getter
@Setter
@NoArgsConstructor
public class ImageWriteRequest {

    @NotNull @Schema(description = "image_collection.collection_id. 제공 기관은 컬렉션을 따른다", example = "1")
    private Long collectionId;

    @NotBlank @Size(max = 200) @Schema(example = "admin-image-001")
    private String sourceId;

    @NotBlank @Size(max = 300) @Schema(example = "서울 위성 영상")
    private String title;

    @NotBlank @Size(max = 50) @Schema(description = "앞뒤 공백 제거 후 대문자로 저장", example = "sentinel-2a")
    private String satellite;

    @Size(min = 1) @Schema(description = "선택. 값이 있으면 비어 있지 않은 배열. 원소는 앞뒤 공백 제거 후 대문자로 저장", example = "[\"msi\"]")
    private List<@NotBlank String> instruments;

    @NotBlank @Pattern(regexp = "OPTICAL|SAR") @Schema(
            allowableValues = {"OPTICAL", "SAR"},
            example = "OPTICAL")
    private String sensorType;

    @NotNull @Schema(description = "시간대가 포함된 ISO 8601", example = "2026-10-19T10:00:00+09:00")
    private OffsetDateTime acquiredAt;

    @Schema(description = "선택. 시간대가 포함된 ISO 8601. acquiredAt과 같거나 이후", example = "2026-10-19T10:00:27+09:00")
    private OffsetDateTime acquiredEndAt;

    @Digits(integer = 3, fraction = 2) @DecimalMin("0") @DecimalMax("100") @Schema(description = "SAR이면 null", example = "5.25")
    private BigDecimal cloudCover;

    @Pattern(regexp = "ASCENDING|DESCENDING") @Schema(allowableValues = {"ASCENDING", "DESCENDING"})
    private String orbitDirection;

    @NotNull @Schema(
            type = "object",
            description = "경위도 좌표의 유효한 GeoJSON MultiPolygon (SRID 4326)",
            example =
                    "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[126.9,37.5],[127.0,37.5],[127.0,37.6],[126.9,37.5]]]]}")
    private JsonNode footprint;

    @NotBlank @Schema(description = "메타데이터 URL 또는 저장 경로", example = "archive/metadata.json")
    private String url;

    @NotNull @Size(min = 1) @Schema(example = "[\"s3://archive/image.tif\"]")
    private List<@NotBlank String> downloadUrls;

    @Schema(description = "선택. 시간대가 포함된 ISO 8601", example = "2026-10-19T01:00:00Z")
    private OffsetDateTime sourceUpdatedAt;

    @Schema(description = "JSON 객체. 생략하거나 null이면 {}", example = "{}")
    private Map<String, Object> rawMetadata;

    @Valid @Schema(description = "SAR 영상만. 생략하거나 null이면 SAR 정보를 지운다")
    private SarRequest sar;

    public record SarRequest(
            @Positive Integer relativeOrbit,
            @PositiveOrZero Integer frame,

            @Size(max = 10) @Schema(description = "앞뒤 공백 제거 후 대문자로 저장", example = "IW")
            String instrumentMode,

            @Size(min = 1) @Schema(example = "[\"VV\", \"VH\"]")
            List<@NotNull @Pattern(regexp = "HH|HV|VH|VV") String> polarizations,

            @Pattern(regexp = "LEFT|RIGHT") @Schema(allowableValues = {"LEFT", "RIGHT"})
            String lookDirection,

            @Digits(integer = 2, fraction = 2) @DecimalMin("0") @DecimalMax("90") @Schema(description = "장면 중심 입사각(도)")
            BigDecimal incidenceAngle,

            @Digits(integer = 6, fraction = 2) @Positive @Schema(description = "위성 진행 방향에 수직인 해상도(지상 거리 기준)")
            BigDecimal resolutionRangeMeters,

            @Digits(integer = 6, fraction = 2) @Positive @Schema(description = "위성 진행 방향 해상도")
            BigDecimal resolutionAzimuthMeters) {

        public SarRequest {
            instrumentMode =
                    instrumentMode == null ? null : instrumentMode.strip().toUpperCase(Locale.ROOT);
        }
    }

    public void setSatellite(String satellite) {
        this.satellite = satellite == null ? null : satellite.strip().toUpperCase(Locale.ROOT);
    }

    // null 원소는 그대로 둬서 @NotBlank가 거부하게 한다
    public void setInstruments(List<String> instruments) {
        this.instruments = instruments == null
                ? null
                : instruments.stream()
                        .map(instrument ->
                                instrument == null ? null : instrument.strip().toUpperCase(Locale.ROOT))
                        .toList();
    }

    @AssertTrue(message = "SAR 영상의 cloudCover는 null이어야 합니다.") @Schema(hidden = true)
    public boolean isCloudCoverValid() {
        return !"SAR".equals(sensorType) || cloudCover == null;
    }

    @AssertTrue(message = "SAR 영상만 sar를 가질 수 있습니다.") @Schema(hidden = true)
    public boolean isSarValid() {
        return "SAR".equals(sensorType) || sar == null;
    }

    @AssertTrue(message = "acquiredEndAt은 acquiredAt과 같거나 이후여야 합니다.") @Schema(hidden = true)
    public boolean isAcquiredRangeValid() {
        return acquiredAt == null || acquiredEndAt == null || !acquiredEndAt.isBefore(acquiredAt);
    }

    public MultiPolygon toFootprint() {
        return GeoJsonMultiPolygon.parse(footprint, "footprint");
    }
}
