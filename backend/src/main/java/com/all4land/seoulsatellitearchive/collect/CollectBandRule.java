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

// 수집할 때 데이터 파일에 붙일 밴드 규칙(V35). 파일 이름이 fileSuffix로 끝나면 이 밴드를 붙인다
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectBandRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long id;

    private Long collectionId;

    private String fileSuffix;

    private Short bandIndex;

    private String bandName;

    private String commonName;

    private BigDecimal centerWavelengthUm;

    private BigDecimal resolutionMeters;
}
