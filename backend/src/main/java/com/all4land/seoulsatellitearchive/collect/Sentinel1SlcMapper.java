package com.all4land.seoulsatellitearchive.collect;

import com.all4land.seoulsatellitearchive.image.dto.ImageWriteRequest.SarRequest;
import java.util.Arrays;
import java.util.Set;
import tools.jackson.databind.JsonNode;

// ASF가 CMR에 올린 Sentinel-1 SLC(1A·1C·1D는 컬렉션이 따로다). 촬영 기하는 V17 백필과 같은 속성에서 읽는다.
// 해상도는 그래뉼에 값으로 오지 않아 저장할 때 SAR 규칙(collect_sar_rule)에서 모드로 채운다
public class Sentinel1SlcMapper extends CmrGranuleMapper {

    @Override
    public Set<String> collections() {
        return Set.of("C1214470488-ASF", "C3470873558-ASF", "C4175278193-ASF");
    }

    @Override
    protected String sensorType() {
        return "SAR";
    }

    @Override
    protected String orbitDirection(JsonNode umm) {
        return attribute(umm, "ASCENDING_DESCENDING");
    }

    @Override
    protected SarRequest sar(JsonNode umm) {
        String polarization = attribute(umm, "POLARIZATION");
        return new SarRequest(
                integer(attribute(umm, "PATH_NUMBER")),
                integer(attribute(umm, "FRAME_NUMBER")),
                attribute(umm, "BEAM_MODE"),
                polarization == null ? null : Arrays.asList(polarization.split("\\+")),
                lookDirection(attribute(umm, "LOOK_DIRECTION")),
                null,
                null,
                null);
    }

    private static String lookDirection(String value) {
        if ("L".equals(value)) {
            return "LEFT";
        }
        return "R".equals(value) ? "RIGHT" : null;
    }

    private static Integer integer(String value) {
        return value == null ? null : Integer.valueOf(value);
    }
}
