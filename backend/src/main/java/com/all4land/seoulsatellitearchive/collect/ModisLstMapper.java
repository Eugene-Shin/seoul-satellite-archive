package com.all4land.seoulsatellitearchive.collect;

import java.util.List;
import java.util.Set;
import tools.jackson.databind.JsonNode;

// NASA MODIS 지표 온도 일별 합성(MOD11A1 Terra, MYD11A1 Aqua v061, 1 km). .hdf 하나에 레이어 12개가 들어 있고,
// 밴드는 저장할 때 밴드 규칙(collect_band_rule)으로 붙인다
public class ModisLstMapper extends CmrGranuleMapper {

    @Override
    public Set<String> collections() {
        return Set.of("C1748058432-LPCLOUD", "C1748046084-LPCLOUD");
    }

    // sinusoidal 타일(h28v05)
    @Override
    public String grid(JsonNode item) {
        JsonNode umm = item.path("umm");
        String horizontal = attribute(umm, "HORIZONTALTILENUMBER");
        String vertical = attribute(umm, "VERTICALTILENUMBER");
        return horizontal == null || vertical == null ? null : "h" + horizontal + "v" + vertical;
    }

    // 그래뉼에 Platforms가 없어서 장면 ID 앞부분(MOD: Terra, MYD: Aqua)으로 정한다
    @Override
    protected String satellite(JsonNode umm) {
        String granule = umm.path("GranuleUR").asString("");
        if (granule.startsWith("MOD")) {
            return "TERRA";
        }
        return granule.startsWith("MYD") ? "AQUA" : null;
    }

    @Override
    protected List<String> instruments(JsonNode platforms) {
        return List.of("MODIS");
    }
}
