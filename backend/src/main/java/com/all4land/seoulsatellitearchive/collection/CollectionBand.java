package com.all4land.seoulsatellitearchive.collection;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 출처가 밴드 정보를 주지 않는 컬렉션의 밴드 하나(V33)
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionBand {

    private String bandName;

    private String commonName;

    private BigDecimal centerWavelengthUm;

    private BigDecimal resolutionMeters;
}
