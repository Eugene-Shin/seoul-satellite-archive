package com.all4land.seoulsatellitearchive.image;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageFileBand {

    private Short bandIndex;

    private String bandName;

    private String commonName;

    private BigDecimal centerWavelengthUm;

    private BigDecimal bandwidthUm;

    private BigDecimal resolutionMeters;
}
