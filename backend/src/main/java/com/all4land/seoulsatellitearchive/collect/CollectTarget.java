package com.all4land.seoulsatellitearchive.collect;

import com.all4land.seoulsatellitearchive.collection.ImageCollection;
import com.all4land.seoulsatellitearchive.global.config.ClockConfig;
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
import java.time.Instant;
import java.time.ZonedDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.MultiPolygon;
import org.springframework.scheduling.support.CronExpression;

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

    @Formula("ST_AsGeoJSON(aoi)")
    private String aoiGeoJson;

    private String scheduleCron;

    @JdbcTypeCode(SqlTypes.INTERVAL_SECOND)
    private Duration lookback;

    private boolean enabled;

    public CollectTarget(
            ImageCollection collection, MultiPolygon aoi, String scheduleCron, Duration lookback, boolean enabled) {
        this.collection = collection;
        replaceSettings(aoi, scheduleCron, lookback, enabled);
    }

    // 마지막 정기 등록(없으면 대상을 만든 시각) 다음의 cron 시각. cron은 한국 시간으로 해석한다
    public Instant nextRunAt(Instant lastScheduledCreatedAt) {
        Instant base = lastScheduledCreatedAt == null ? getCreatedAt() : lastScheduledCreatedAt;
        ZonedDateTime next = CronExpression.parse(scheduleCron).next(base.atZone(ClockConfig.KST));
        return next == null ? null : next.toInstant();
    }

    // 컬렉션은 바꾸지 않는다. 바꾸면 이어받는 조회 시작점이 다른 컬렉션의 기록이 되기 때문이다
    public void replaceSettings(MultiPolygon aoi, String scheduleCron, Duration lookback, boolean enabled) {
        this.aoi = aoi;
        this.scheduleCron = scheduleCron;
        this.lookback = lookback;
        this.enabled = enabled;
    }
}
