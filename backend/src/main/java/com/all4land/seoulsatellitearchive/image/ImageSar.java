package com.all4land.seoulsatellitearchive.image;

import com.all4land.seoulsatellitearchive.image.dto.ImageWriteRequest.SarRequest;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 영상 쪽에서 참조하지 않는다. 역방향 1:1은 지연 로딩이 안 돼서 목록 조회마다 행별 쿼리가 나간다
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageSar {

    @Id
    private Long imageId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private SatelliteImage image;

    private Integer relativeOrbit;

    private Integer frame;

    private String instrumentMode;

    private String[] polarizations;

    private String lookDirection;

    private BigDecimal incidenceAngle;

    private BigDecimal resolutionRangeMeters;

    private BigDecimal resolutionAzimuthMeters;

    public ImageSar(SatelliteImage image) {
        this.image = image;
    }

    public void replace(SarRequest request) {
        this.relativeOrbit = request.relativeOrbit();
        this.frame = request.frame();
        this.instrumentMode = request.instrumentMode();
        this.polarizations =
                request.polarizations() == null ? null : request.polarizations().toArray(String[]::new);
        this.lookDirection = request.lookDirection();
        this.incidenceAngle = request.incidenceAngle();
        this.resolutionRangeMeters = request.resolutionRangeMeters();
        this.resolutionAzimuthMeters = request.resolutionAzimuthMeters();
    }
}
