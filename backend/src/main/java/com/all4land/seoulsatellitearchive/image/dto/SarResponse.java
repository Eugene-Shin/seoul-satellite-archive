package com.all4land.seoulsatellitearchive.image.dto;

import com.all4land.seoulsatellitearchive.image.ImageSar;
import java.math.BigDecimal;
import java.util.List;

public record SarResponse(
        Integer relativeOrbit,
        Integer frame,
        String instrumentMode,
        List<String> polarizations,
        String lookDirection,
        BigDecimal incidenceAngle,
        BigDecimal resolutionRangeMeters,
        BigDecimal resolutionAzimuthMeters) {

    public static SarResponse from(ImageSar sar) {
        return new SarResponse(
                sar.getRelativeOrbit(),
                sar.getFrame(),
                sar.getInstrumentMode(),
                sar.getPolarizations() == null ? null : List.of(sar.getPolarizations()),
                sar.getLookDirection(),
                sar.getIncidenceAngle(),
                sar.getResolutionRangeMeters(),
                sar.getResolutionAzimuthMeters());
    }
}
