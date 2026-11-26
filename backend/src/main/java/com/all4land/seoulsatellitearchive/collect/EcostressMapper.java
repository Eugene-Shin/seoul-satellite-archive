package com.all4land.seoulsatellitearchive.collect;

import java.util.Set;
import tools.jackson.databind.JsonNode;

// NASA ECOSTRESS 지표 온도·방출률 타일(ECO_L2T_LSTE v003, 70 m). 국제우주정거장(ISS)에 실린 센서라 위성 이름은 ISS다.
// 레이어마다 COG가 따로 있고, 밴드는 저장할 때 밴드 규칙(collect_band_rule)으로 붙인다
public class EcostressMapper extends CmrGranuleMapper {

    @Override
    public Set<String> collections() {
        return Set.of("C3998139651-LPCLOUD");
    }

    // 타일 속성이 없어서 장면 ID의 여섯 번째 토큰(ECOv003_L2T_LSTE_47208_011_52SCG_...)을 쓴다
    @Override
    public String grid(JsonNode item) {
        String[] tokens = item.path("umm").path("GranuleUR").asString("").split("_");
        return tokens.length > 5 ? tokens[5] : null;
    }
}
