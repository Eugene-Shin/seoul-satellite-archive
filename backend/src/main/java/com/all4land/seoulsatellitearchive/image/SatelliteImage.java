package com.all4land.seoulsatellitearchive.image;

import com.all4land.seoulsatellitearchive.global.entity.BaseEntity;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Formula;
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
}
