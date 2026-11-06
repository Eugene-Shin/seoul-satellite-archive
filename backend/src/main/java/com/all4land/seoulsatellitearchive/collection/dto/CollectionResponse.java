package com.all4land.seoulsatellitearchive.collection.dto;

import com.all4land.seoulsatellitearchive.collection.ImageCollection;

public record CollectionResponse(
        Long collectionId,
        String providerCode,
        String code,
        String title,
        String productType,
        String licenseName,
        String licenseUrl,
        String creditText) {

    public static CollectionResponse from(ImageCollection collection) {
        return new CollectionResponse(
                collection.getId(),
                collection.getProvider().getCode(),
                collection.getCode(),
                collection.getTitle(),
                collection.getProductType(),
                collection.getLicenseName(),
                collection.getLicenseUrl(),
                collection.getCreditText());
    }
}
