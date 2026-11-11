package com.all4land.seoulsatellitearchive.collect;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectRunSkip {

    private String sourceId;

    private String reason;
}
