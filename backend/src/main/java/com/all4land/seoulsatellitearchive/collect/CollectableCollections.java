package com.all4land.seoulsatellitearchive.collect;

import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

// 수집할 수 있는(매퍼가 있는) 컬렉션 코드. 새 컬렉션의 매퍼를 만들면 그 코드를 여기에도 추가한다
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CollectableCollections {

    private static final Set<String> CODES = Set.of(Sentinel2Mapper.COLLECTION);

    public static boolean contains(String collectionCode) {
        return CODES.contains(collectionCode);
    }
}
