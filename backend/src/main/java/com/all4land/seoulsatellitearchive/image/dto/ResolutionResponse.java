package com.all4land.seoulsatellitearchive.image.dto;

import com.all4land.seoulsatellitearchive.image.ImageResolution;
import java.math.BigDecimal;

public record ResolutionResponse(BigDecimal minMeters, BigDecimal maxMeters) {

    public static ResolutionResponse from(ImageResolution resolution) {
        return resolution == null ? null : new ResolutionResponse(resolution.getMinMeters(), resolution.getMaxMeters());
    }
}
