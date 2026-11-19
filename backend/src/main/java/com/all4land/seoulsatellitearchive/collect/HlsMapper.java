package com.all4land.seoulsatellitearchive.collect;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import tools.jackson.databind.JsonNode;

// NASA HLS L30·S30. CMR은 밴드 정보를 주지 않아서 밴드는 저장할 때 밴드 규칙(collect_band_rule)으로 붙인다
public class HlsMapper extends CmrGranuleMapper {

    @Override
    public Set<String> collections() {
        return Set.of("C2021957657-LPCLOUD", "C2021957295-LPCLOUD");
    }

    // 같은 센서를 S2 c1은 MSI, HLS S30은 "Sentinel-2 MSI"로 준다. 센서로 검색할 때 함께 나오도록 맞춘다
    @Override
    protected List<String> instruments(JsonNode platforms) {
        return super.instruments(platforms).stream()
                .map(name -> name == null ? null : name.replaceFirst("^Sentinel-2 ", ""))
                .toList();
    }

    @Override
    protected BigDecimal cloudCover(JsonNode umm) {
        return decimal(attribute(umm, "CLOUD_COVERAGE"), 2);
    }
}
