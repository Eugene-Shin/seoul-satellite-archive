package com.all4land.seoulsatellitearchive.collect;

import static org.assertj.core.api.Assertions.assertThat;

import com.all4land.seoulsatellitearchive.image.ImageFileDetails;
import com.all4land.seoulsatellitearchive.image.ImageFileRole;
import com.all4land.seoulsatellitearchive.image.dto.ImageWriteRequest;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// CMR 실제 검색 응답(서울, 2026-11-01)의 ECOSTRESS v003, MOD11A1, MYD11A1 그래뉼 하나씩
class LstMapperTest {

    private static JsonNode item(int index) throws IOException {
        try (InputStream in = LstMapperTest.class.getResourceAsStream("/collect/lst-search.json")) {
            return JsonMapper.shared().readTree(in).path("items").get(index);
        }
    }

    // 사각형(BoundingRectangles)으로만 주는 영역도 다각형으로 저장한다
    @Test
    void mapsEcostressFieldsAndRectangleFootprint() throws IOException {
        ImageWriteRequest request = new EcostressMapper().map(item(0), 7L).request();

        assertThat(request.getSourceId()).isEqualTo("ECOv003_L2T_LSTE_47208_011_52SCG_20261101T210238_01");
        assertThat(request.getSatellite()).isEqualTo("ISS");
        assertThat(request.getInstruments()).containsExactly("ECOSTRESS");
        assertThat(request.getSensorType()).isEqualTo("OPTICAL");
        assertThat(request.getAcquiredAt()).isEqualTo(OffsetDateTime.parse("2026-11-01T21:02:38.921Z"));
        assertThat(request.getCloudCover()).isNull();
        assertThat(request.toFootprint().getEnvelopeInternal().getMinX()).isEqualTo(126.72447176);
        assertThat(request.toFootprint().getEnvelopeInternal().getMaxY()).isEqualTo(37.9431072);
    }

    // 레이어 COG는 데이터, 레이어별 그림은 미리보기, 그 좌표 정보(.aux.xml)는 메타데이터. 밴드는 저장할 때 규칙으로 붙인다
    @Test
    void classifiesEcostressLayerFiles() throws IOException {
        Map<ImageFileRole, Long> roles = new EcostressMapper()
                .map(item(0), 7L).files().values().stream()
                        .collect(Collectors.groupingBy(ImageFileDetails::role, Collectors.counting()));

        assertThat(roles)
                .containsEntry(ImageFileRole.DATA, 19L)
                .containsEntry(ImageFileRole.PREVIEW, 19L)
                .containsEntry(ImageFileRole.METADATA, 21L)
                .containsEntry(ImageFileRole.THUMBNAIL, 1L);
    }

    // 그래뉼에 Platforms가 없어서 장면 ID로 Terra·Aqua를 정한다
    @Test
    void mapsModisSatelliteFromGranuleId() throws IOException {
        ModisLstMapper mapper = new ModisLstMapper();
        ImageWriteRequest terra = mapper.map(item(1), 7L).request();

        assertThat(terra.getSatellite()).isEqualTo("TERRA");
        assertThat(terra.getInstruments()).containsExactly("MODIS");
        assertThat(terra.getAcquiredAt()).isEqualTo(OffsetDateTime.parse("2026-11-01T00:00:00Z"));
        assertThat(terra.getAcquiredEndAt()).isEqualTo(OffsetDateTime.parse("2026-11-01T23:59:59Z"));
        assertThat(mapper.map(item(2), 7L).request().getSatellite()).isEqualTo("AQUA");
    }

    // OPeNDAP 서비스 주소는 파일이 아니라 뺀다
    @Test
    void mapsModisFiles() throws IOException {
        Map<String, ImageFileRole> files = new ModisLstMapper()
                .map(item(1), 7L).files().values().stream()
                        .collect(Collectors.toMap(ImageFileDetails::assetKey, ImageFileDetails::role));

        assertThat(files)
                .hasSize(4)
                .containsEntry("MOD11A1.A2026305.h28v05.061.2026306092403.hdf", ImageFileRole.DATA)
                .containsEntry("MOD11A1.A2026305.h28v05.061.2026306092403.cmr.xml", ImageFileRole.METADATA)
                .containsEntry("BROWSE.MOD11A1.A2026305.h28v05.061.2026306092406.1.jpg", ImageFileRole.THUMBNAIL);
        assertThat(files.keySet()).noneMatch(name -> name.contains("opendap"));
    }
}
