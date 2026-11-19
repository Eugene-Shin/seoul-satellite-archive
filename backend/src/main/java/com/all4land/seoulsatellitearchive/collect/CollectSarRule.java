package com.all4land.seoulsatellitearchive.collect;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 수집할 때 SAR 영상에 채울 촬영 모드별 해상도 규칙(V38)
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectSarRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long id;

    private Long collectionId;

    private String instrumentMode;

    private BigDecimal resolutionRangeMeters;

    private BigDecimal resolutionAzimuthMeters;
}
