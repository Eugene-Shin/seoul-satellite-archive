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

// USGS 실제 검색 응답(서울, 2026-10~11)의 첫 장면
class LandsatMapperTest {

    private static final LandsatMapper MAPPER = new LandsatMapper();
    private static final String SCENE_DIR = "https://landsatlook.usgs.gov/data/collection02/level-2/standard/oli-tirs/"
            + "2026/116/034/LC09_L2SP_116034_20261115_20261116_02_T1/LC09_L2SP_116034_20261115_20261116_02_T1_";

    private static JsonNode firstItem(String collection) throws IOException {
        try (InputStream in = LandsatMapperTest.class.getResourceAsStream("/collect/" + collection + "-search.json")) {
            return JsonMapper.shared().readTree(in).path("features").get(0);
        }
    }

    @Test
    void mapsImageFields() throws IOException {
        ImageWriteRequest request = MAPPER.map(firstItem("landsat-c2l2-sr"), 7L).request();

        assertThat(request.getSourceId()).isEqualTo("LC09_L2SP_116034_20261115_20261116_02_T1_SR");
        assertThat(request.getTitle()).isEqualTo("LC09_L2SP_116034_20261115_20261116_02_T1_SR");
        assertThat(request.getSatellite()).isEqualTo("LANDSAT-9");
        assertThat(request.getInstruments()).containsExactly("OLI", "TIRS");
        assertThat(request.getAcquiredAt()).isEqualTo(OffsetDateTime.parse("2026-11-15T02:11:12.009986Z"));
        assertThat(request.getAcquiredEndAt()).isNull();
        assertThat(request.getCloudCover()).isEqualByComparingTo("6.23");
        assertThat(request.toFootprint().getNumGeometries()).isEqualTo(1);
        assertThat(request.getUrl())
                .isEqualTo("https://landsatlook.usgs.gov/stac-server/collections/landsat-c2l2-sr/items/"
                        + "LC09_L2SP_116034_20261115_20261116_02_T1_SR");
        assertThat(request.getDownloadUrls()).hasSize(17);
        assertThat(request.getSourceUpdatedAt()).isEqualTo(OffsetDateTime.parse("2026-11-16T11:07:30.770Z"));
    }

    // 픽셀 간격은 Item의 proj:transform(30 m), 센서 해상도는 밴드 gsd다
    @Test
    void mapsFilesWithItemSpacingAndRasterAsData() throws IOException {
        Map<String, ImageFileDetails> sr =
                MAPPER.map(firstItem("landsat-c2l2-sr"), 7L).files();

        ImageFileDetails red = sr.get(SCENE_DIR + "SR_B4.TIF");
        assertThat(red.role()).isEqualTo(ImageFileRole.DATA);
        assertThat(red.pixelSpacingMeters()).isEqualByComparingTo("30");
        assertThat(red.bands()).singleElement().satisfies(band -> {
            assertThat(band.getBandName()).isEqualTo("B4");
            assertThat(band.getCommonName()).isEqualTo("red");
            assertThat(band.getResolutionMeters()).isEqualByComparingTo("30");
        });
        assertThat(sr.get(SCENE_DIR + "SR_QA_AEROSOL.TIF").role()).isEqualTo(ImageFileRole.DATA);
        assertThat(sr.get(SCENE_DIR + "thumb_small.jpeg").role()).isEqualTo(ImageFileRole.THUMBNAIL);
        assertThat(sr.get(SCENE_DIR + "thumb_large.jpeg").role()).isEqualTo(ImageFileRole.PREVIEW);
        assertThat(sr.get(SCENE_DIR + "MTL.json").role()).isEqualTo(ImageFileRole.METADATA);

        ImageFileDetails thermal =
                MAPPER.map(firstItem("landsat-c2l2-st"), 8L).files().get(SCENE_DIR + "ST_B10.TIF");
        assertThat(thermal.pixelSpacingMeters()).isEqualByComparingTo("30");
        assertThat(thermal.bands()).singleElement().satisfies(band -> {
            assertThat(band.getCommonName()).isEqualTo("lwir11");
            assertThat(band.getResolutionMeters()).isEqualByComparingTo("100");
        });
    }
}
