package com.all4land.seoulsatellitearchive.image;

import java.math.BigDecimal;
import java.util.List;

public record ImageFileDetails(
        String assetKey,
        ImageFileRole role,
        String mediaType,
        BigDecimal pixelSpacingMeters,
        List<ImageFileBand> bands) {}
