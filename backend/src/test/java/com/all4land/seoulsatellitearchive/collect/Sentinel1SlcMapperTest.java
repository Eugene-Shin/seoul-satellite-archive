package com.all4land.seoulsatellitearchive.collect;

import static org.assertj.core.api.Assertions.assertThat;

import com.all4land.seoulsatellitearchive.image.ImageFileRole;
import com.all4land.seoulsatellitearchive.image.dto.ImageWriteRequest;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// CMR 실제 검색 응답(서울, 2026-10)의 Sentinel-1C(하강)·1D(상승) SLC 하나씩
class Sentinel1SlcMapperTest {

    private static final Sentinel1SlcMapper MAPPER = new Sentinel1SlcMapper();

    private static JsonNode item(int index) throws IOException {
        try (InputStream in = Sentinel1SlcMapperTest.class.getResourceAsStream("/collect/s1-search.json")) {
            return JsonMapper.shared().readTree(in).path("items").get(index);
        }
    }

    @Test
    void mapsSarImageFields() throws IOException {
        ImageWriteRequest request = MAPPER.map(item(0), 7L).request();

        assertThat(request.getSourceId())
                .isEqualTo("S1C_IW_SLC__1SDV_20261012T213121_20261012T213148_009857_013A5F_ADB1-SLC");
        assertThat(request.getSatellite()).isEqualTo("SENTINEL-1C");
        assertThat(request.getInstruments()).containsExactly("C-SAR");
        assertThat(request.getSensorType()).isEqualTo("SAR");
        assertThat(request.getOrbitDirection()).isEqualTo("DESCENDING");
        assertThat(request.getCloudCover()).isNull();
        assertThat(request.getAcquiredAt()).isEqualTo(OffsetDateTime.parse("2026-10-12T21:31:21.761Z"));
        assertThat(request.getAcquiredEndAt()).isEqualTo(OffsetDateTime.parse("2026-10-12T21:31:48.859Z"));
        assertThat(MAPPER.map(item(1), 7L).request().getOrbitDirection()).isEqualTo("ASCENDING");
    }

    // 촬영 기하는 V17 백필과 같은 속성에서 읽는다. 해상도는 저장할 때 SAR 규칙으로 채운다
    @Test
    void mapsSarGeometry() throws IOException {
        ImageWriteRequest.SarRequest sar = MAPPER.map(item(0), 7L).request().getSar();

        assertThat(sar.relativeOrbit()).isEqualTo(134);
        assertThat(sar.frame()).isEqualTo(466);
        assertThat(sar.instrumentMode()).isEqualTo("IW");
        assertThat(sar.polarizations()).containsExactly("VV", "VH");
        assertThat(sar.lookDirection()).isEqualTo("RIGHT");
        assertThat(sar.resolutionRangeMeters()).isNull();
    }

    // SLC 묶음 .zip 하나가 원본 파일이다
    @Test
    void mapsZipAsOriginal() throws IOException {
        assertThat(MAPPER.map(item(0), 7L).files().values()).singleElement().satisfies(file -> {
            assertThat(file.assetKey())
                    .isEqualTo("S1C_IW_SLC__1SDV_20261012T213121_20261012T213148_009857_013A5F_ADB1.zip");
            assertThat(file.role()).isEqualTo(ImageFileRole.ORIGINAL);
        });
    }
}
