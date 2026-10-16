package com.all4land.seoulsatellitearchive.image;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Sort;

@Getter
@AllArgsConstructor
public enum ImageSort {
    ACQUIRED_AT_ASC("acquiredAt", Sort.Direction.ASC),
    ACQUIRED_AT_DESC("acquiredAt", Sort.Direction.DESC),
    CLOUD_COVER_ASC("cloudCover", Sort.Direction.ASC),
    CLOUD_COVER_DESC("cloudCover", Sort.Direction.DESC);

    private final String property;
    private final Sort.Direction direction;

    public Sort.Order toOrder() {
        return new Sort.Order(direction, property).nullsLast();
    }
}
