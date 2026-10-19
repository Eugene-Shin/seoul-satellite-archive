package com.all4land.seoulsatellitearchive.image;

import com.all4land.seoulsatellitearchive.global.entity.BaseEntity;
import com.all4land.seoulsatellitearchive.image.dto.ImageWriteRequest;
import com.all4land.seoulsatellitearchive.provider.Provider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.MultiPolygon;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SatelliteImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider")
    private Provider provider;

    private String sourceId;

    private String title;

    private String satellite;

    private String[] instruments;

    @Enumerated(EnumType.STRING)
    private SensorType sensorType;

    private Instant acquiredAt;

    private BigDecimal gsdMeters;

    private BigDecimal cloudCover;

    @Enumerated(EnumType.STRING)
    private OrbitDirection orbitDirection;

    private MultiPolygon footprint;

    @Formula("ST_AsGeoJSON(footprint)")
    private String footprintGeoJson;

    private String url;

    private String[] downloadUrls;

    private Instant sourceUpdatedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_metadata", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> rawMetadata;

    public void replaceMetadata(ImageWriteRequest request, Provider provider, MultiPolygon footprint) {
        this.provider = provider;
        this.sourceId = request.getSourceId();
        this.title = request.getTitle();
        this.satellite = request.getSatellite();
        this.instruments = request.getInstruments() == null
                ? null
                : request.getInstruments().toArray(String[]::new);
        this.sensorType = SensorType.valueOf(request.getSensorType());
        this.acquiredAt = request.getAcquiredAt().toInstant();
        this.gsdMeters = request.getGsdMeters();
        this.cloudCover = request.getCloudCover();
        this.orbitDirection =
                request.getOrbitDirection() == null ? null : OrbitDirection.valueOf(request.getOrbitDirection());
        this.footprint = footprint;
        this.url = request.getUrl();
        this.downloadUrls = request.getDownloadUrls().toArray(String[]::new);
        this.sourceUpdatedAt = request.getSourceUpdatedAt() == null
                ? null
                : request.getSourceUpdatedAt().toInstant();
        this.rawMetadata = request.getRawMetadata() == null ? Map.of() : request.getRawMetadata();
    }
}
