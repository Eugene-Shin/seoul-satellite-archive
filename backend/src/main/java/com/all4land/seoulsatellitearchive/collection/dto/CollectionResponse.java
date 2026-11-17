package com.all4land.seoulsatellitearchive.collection.dto;

import com.all4land.seoulsatellitearchive.collect.CollectableCollections;
import com.all4land.seoulsatellitearchive.collection.ImageCollection;
import io.swagger.v3.oas.annotations.media.Schema;

public record CollectionResponse(
        Long collectionId,
        String providerCode,
        String code,
        String title,
        String productType,
        String licenseName,
        String licenseUrl,
        String creditText,
        @Schema(description = "자동 수집 대상으로 고를 수 있는 컬렉션") boolean collectable) {

    public static CollectionResponse from(ImageCollection collection) {
        return new CollectionResponse(
                collection.getId(),
                collection.getProvider().getCode(),
                collection.getCode(),
                collection.getTitle(),
                collection.getProductType(),
                collection.getLicenseName(),
                collection.getLicenseUrl(),
                collection.getCreditText(),
                CollectableCollections.contains(collection.getCode()));
    }
}
