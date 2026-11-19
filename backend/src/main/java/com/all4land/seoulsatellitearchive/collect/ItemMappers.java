package com.all4land.seoulsatellitearchive.collect;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

// 컬렉션 코드 → 매퍼. 매퍼가 있는 컬렉션만 수집할 수 있다. 새 출처의 매퍼는 여기에 추가한다.
// 매퍼는 상태가 없어서 빈으로 두지 않는다(응답 DTO의 정적 변환에서도 수집 가능 여부를 본다)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ItemMappers {

    private static final Map<String, ItemMapper> BY_COLLECTION = List.of(
                    new Sentinel2Mapper(),
                    new LandsatMapper(),
                    new HlsMapper(),
                    new EcostressMapper(),
                    new ModisLstMapper(),
                    new Sentinel1SlcMapper())
            .stream()
            .flatMap(mapper -> mapper.collections().stream().map(code -> Map.entry(code, mapper)))
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

    public static Optional<ItemMapper> find(String collectionCode) {
        return Optional.ofNullable(BY_COLLECTION.get(collectionCode));
    }

    public static boolean supports(String collectionCode) {
        return BY_COLLECTION.containsKey(collectionCode);
    }
}
