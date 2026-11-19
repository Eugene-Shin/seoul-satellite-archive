package com.all4land.seoulsatellitearchive.collect;

import java.util.Set;

// NASA ECOSTRESS 지표 온도·방출률 타일(ECO_L2T_LSTE v003, 70 m). 국제우주정거장(ISS)에 실린 센서라 위성 이름은 ISS다.
// 레이어마다 COG가 따로 있고, 밴드는 저장할 때 밴드 규칙(collect_band_rule)으로 붙인다
public class EcostressMapper extends CmrGranuleMapper {

    @Override
    public Set<String> collections() {
        return Set.of("C3998139651-LPCLOUD");
    }
}
