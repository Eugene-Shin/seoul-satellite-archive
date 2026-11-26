package com.all4land.seoulsatellitearchive.collect;

import com.all4land.seoulsatellitearchive.image.ImageFileRole;
import java.math.BigDecimal;
import java.util.Set;
import tools.jackson.databind.JsonNode;

// USGS Landsat C2 L2. SR과 ST는 규칙이 같고 밴드만 다르다. 썸네일·MTL·QA_PIXEL 등은 두 Item이 같은 파일을 가리킨다
public class LandsatMapper extends StacItemMapper {

    @Override
    public Set<String> collections() {
        return Set.of("landsat-c2l2-sr", "landsat-c2l2-st");
    }

    // WRS 경로-행(116-034)
    @Override
    public String grid(JsonNode item) {
        JsonNode properties = item.path("properties");
        String path = properties.path("landsat:wrs_path").asString(null);
        String row = properties.path("landsat:wrs_row").asString(null);
        return path == null || row == null ? null : path + "-" + row;
    }

    // 위성 이름은 출처가 달라도 같아야 해서 다른 출처처럼 -로 잇는다(LANDSAT_9 → LANDSAT-9)
    @Override
    protected String satellite(String platform) {
        return platform == null ? null : platform.replace('_', '-');
    }

    // 에셋에는 proj:transform이 없고 Item에 하나 있다. 열적외 밴드도 30 m로 재샘플링돼 있다
    @Override
    protected BigDecimal pixelSpacing(JsonNode item, JsonNode asset) {
        JsonNode properties = item.path("properties");
        return isUtm(properties.path("proj:epsg")) ? pixelSpacing(properties.path("proj:transform")) : null;
    }

    // QA_AEROSOL 같은 마스크 TIF에도 metadata 역할이 붙어 있어서, 래스터는 데이터로 둔다
    @Override
    protected ImageFileRole role(JsonNode asset) {
        if (asset.path("type").asString("").startsWith("image/vnd.stac.geotiff")) {
            return ImageFileRole.DATA;
        }
        return super.role(asset);
    }
}
