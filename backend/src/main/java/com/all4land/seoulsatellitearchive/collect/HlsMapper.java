package com.all4land.seoulsatellitearchive.collect;

import com.all4land.seoulsatellitearchive.image.ImageFileBand;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;

// NASA HLS L30·S30. CMR은 밴드 정보를 주지 않아서 파일 이름의 밴드 이름만 넣고,
// 분류·파장·해상도는 저장할 때 collection_band에서 채운다
public class HlsMapper extends CmrGranuleMapper {

    private static final Pattern BAND_FILE = Pattern.compile("\\.(B\\d\\d|B8A)\\.tif$");

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

    // 파일 하나에 밴드 하나. Fmask와 각도 파일(SZA 등)은 밴드가 아니다
    @Override
    protected List<ImageFileBand> bands(String url) {
        Matcher matcher = BAND_FILE.matcher(url);
        if (!matcher.find()) {
            return List.of();
        }
        return List.of(new ImageFileBand((short) 1, matcher.group(1), null, null, null, null));
    }
}
