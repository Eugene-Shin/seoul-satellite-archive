package com.all4land.seoulsatellitearchive.image.dto;

import com.all4land.seoulsatellitearchive.image.ImageFile;
import com.all4land.seoulsatellitearchive.image.ImageFileRole;
import java.math.BigDecimal;
import java.util.List;

public record ImageFileResponse(
        Long fileId,
        String assetKey,
        ImageFileRole role,
        String href,
        String mediaType,
        BigDecimal pixelSpacingMeters,
        List<ImageFileBandResponse> bands) {

    public static ImageFileResponse from(ImageFile file) {
        return new ImageFileResponse(
                file.getId(),
                file.getAssetKey(),
                file.getRole(),
                file.getHref(),
                file.getMediaType(),
                file.getPixelSpacingMeters(),
                file.getBands().stream().map(ImageFileBandResponse::from).toList());
    }
}
