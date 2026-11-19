package com.all4land.seoulsatellitearchive.collect;

import java.util.Set;

// Earth Search Sentinel-2. 파일 규칙은 V18 백필과 같다
public class Sentinel2Mapper extends StacItemMapper {

    @Override
    public Set<String> collections() {
        return Set.of("sentinel-2-c1-l2a");
    }
}
