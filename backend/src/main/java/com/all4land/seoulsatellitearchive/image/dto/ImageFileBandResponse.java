package com.all4land.seoulsatellitearchive.image.dto;

import com.all4land.seoulsatellitearchive.image.ImageFileBand;
import java.math.BigDecimal;

public record ImageFileBandResponse(
        Short bandIndex,
        String bandName,
        String commonName,
        BigDecimal centerWavelengthUm,
        BigDecimal bandwidthUm,
        BigDecimal resolutionMeters) {

    public static ImageFileBandResponse from(ImageFileBand band) {
        return new ImageFileBandResponse(
                band.getBandIndex(),
                band.getBandName(),
                band.getCommonName(),
                band.getCenterWavelengthUm(),
                band.getBandwidthUm(),
                band.getResolutionMeters());
    }
}
