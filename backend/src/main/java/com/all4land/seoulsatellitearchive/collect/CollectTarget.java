package com.all4land.seoulsatellitearchive.collect;

import com.all4land.seoulsatellitearchive.collection.ImageCollection;
import com.all4land.seoulsatellitearchive.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Duration;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.MultiPolygon;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectTarget extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "target_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id")
    private ImageCollection collection;

    private MultiPolygon aoi;

    private String scheduleCron;

    @JdbcTypeCode(SqlTypes.INTERVAL_SECOND)
    private Duration lookback;

    private boolean enabled;
}
