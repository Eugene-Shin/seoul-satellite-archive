package com.all4land.seoulsatellitearchive.collect;

import static org.assertj.core.api.Assertions.assertThat;

import com.all4land.seoulsatellitearchive.image.ImageFileDetails;
import com.all4land.seoulsatellitearchive.image.ImageFileRole;
import com.all4land.seoulsatellitearchive.image.dto.ImageWriteRequest;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// CMR 실제 검색 응답(서울, 2026-10)의 L30·S30 그래뉼 하나씩
class HlsMapperTest {

    private static final HlsMapper MAPPER = new HlsMapper();
    private static final String L30_DIR = "https://data.lpdaac.earthdatacloud.nasa.gov/lp-prod-protected/HLSL30.020/"
            + "HLS.L30.T52SCG.2026279T021105.v2.0/HLS.L30.T52SCG.2026279T021105.v2.0";

    private static JsonNode item(int index) throws IOException {
        try (InputStream in = HlsMapperTest.class.getResourceAsStream("/collect/hls-search.json")) {
            return JsonMapper.shared().readTree(in).path("items").get(index);
        }
    }

    @Test
    void mapsL30Fields() throws IOException {
        ImageWriteRequest request = MAPPER.map(item(0), 7L).request();

        assertThat(request.getSourceId()).isEqualTo("HLS.L30.T52SCG.2026279T021105.v2.0");
        assertThat(request.getSatellite()).isEqualTo("LANDSAT-8");
        assertThat(request.getInstruments()).containsExactly("OLI");
        assertThat(request.getAcquiredAt()).isEqualTo(OffsetDateTime.parse("2026-10-06T02:11:05.455Z"));
        assertThat(request.getCloudCover()).isEqualByComparingTo("0");
        assertThat(request.toFootprint().getNumGeometries()).isEqualTo(1);
        assertThat(request.getUrl()).isEqualTo("https://cmr.earthdata.nasa.gov/search/concepts/G4348697487-LPCLOUD");
        assertThat(request.getSourceUpdatedAt()).isEqualTo(OffsetDateTime.parse("2026-10-08T01:44:49.536Z"));
    }

    // S2 c1과 같은 센서 이름(MSI)으로 맞춘다
    @Test
    void mapsS30SensorAsMsi() throws IOException {
        ImageWriteRequest request = MAPPER.map(item(1), 7L).request();

        assertThat(request.getSatellite()).isEqualTo("SENTINEL-2C");
        assertThat(request.getInstruments()).containsExactly("MSI");
        assertThat(request.getCloudCover()).isEqualByComparingTo("74");
    }

    // https만. 밴드는 파일 이름의 이름만 넣고 나머지는 저장할 때 collection_band에서 채운다
    @Test
    void mapsHttpsFilesWithGranuleSpacingAndBandNameOnly() throws IOException {
        Map<String, ImageFileDetails> files = MAPPER.map(item(0), 7L).files();

        assertThat(files).hasSize(18).allSatisfy((href, file) -> assertThat(href)
                .startsWith("https://"));
        assertThat(files.keySet()).noneMatch(href -> href.endsWith("s3credentials"));

        ImageFileDetails nir = files.get(L30_DIR + ".B05.tif");
        assertThat(nir.assetKey()).isEqualTo("HLS.L30.T52SCG.2026279T021105.v2.0.B05.tif");
        assertThat(nir.role()).isEqualTo(ImageFileRole.DATA);
        assertThat(nir.pixelSpacingMeters()).isEqualByComparingTo("30");
        assertThat(nir.bands()).singleElement().satisfies(band -> {
            assertThat(band.getBandName()).isEqualTo("B05");
            assertThat(band.getCommonName()).isNull();
        });
        assertThat(files.get(L30_DIR + ".Fmask.tif").bands()).isEmpty();
        assertThat(files.get(L30_DIR + ".cmr.xml").role()).isEqualTo(ImageFileRole.METADATA);
        assertThat(files.get(L30_DIR.replace("protected", "public") + ".jpg").role())
                .isEqualTo(ImageFileRole.THUMBNAIL);
    }
}
