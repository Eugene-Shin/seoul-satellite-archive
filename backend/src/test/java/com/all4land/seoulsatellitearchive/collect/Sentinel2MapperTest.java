package com.all4land.seoulsatellitearchive.collect;

import static org.assertj.core.api.Assertions.assertThat;

import com.all4land.seoulsatellitearchive.image.ImageFileBand;
import com.all4land.seoulsatellitearchive.image.ImageFileDetails;
import com.all4land.seoulsatellitearchive.image.ImageFileRole;
import com.all4land.seoulsatellitearchive.image.dto.ImageWriteRequest;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class Sentinel2MapperTest {

    private static final Sentinel2Mapper MAPPER = new Sentinel2Mapper();

    private static final String ITEM_DIR =
            "https://e84-earth-search-sentinel-data.s3.us-west-2.amazonaws.com/sentinel-2-c1-l2a/52/S/CG/2026/10/"
                    + "S2B_T52SCG_20261028T022157_L2A/";

    // Earth Search 실제 검색 응답(서울, 2026-10)의 첫 영상
    static ObjectNode firstItem() throws IOException {
        try (InputStream in = Sentinel2MapperTest.class.getResourceAsStream("/collect/sentinel-2-c1-l2a-search.json")) {
            return (ObjectNode)
                    JsonMapper.shared().readTree(in).path("features").get(0);
        }
    }

    @Test
    void mapsImageFields() throws IOException {
        ImageWriteRequest request = MAPPER.map(firstItem(), 7L).request();

        assertThat(request.getCollectionId()).isEqualTo(7L);
        assertThat(request.getSourceId()).isEqualTo("S2B_T52SCG_20261028T022157_L2A");
        assertThat(request.getTitle()).isEqualTo("S2B_T52SCG_20261028T022157_L2A");
        assertThat(request.getSatellite()).isEqualTo("SENTINEL-2B");
        assertThat(request.getInstruments()).containsExactly("MSI");
        assertThat(request.getSensorType()).isEqualTo("OPTICAL");
        assertThat(request.getAcquiredAt()).isEqualTo(OffsetDateTime.parse("2026-10-28T02:27:13.169Z"));
        assertThat(request.getAcquiredEndAt()).isNull();
        assertThat(request.getCloudCover()).isEqualByComparingTo("0.94");
        assertThat(request.getOrbitDirection()).isNull();
        assertThat(request.toFootprint().getNumGeometries()).isEqualTo(1);
        assertThat(request.getUrl())
                .isEqualTo("https://earth-search.aws.element84.com/v1/collections/sentinel-2-c1-l2a/items/"
                        + "S2B_T52SCG_20261028T022157_L2A");
        assertThat(request.getDownloadUrls()).hasSize(23).contains(ITEM_DIR + "B04.tif");
        assertThat(request.getSourceUpdatedAt()).isEqualTo(OffsetDateTime.parse("2026-10-28T05:37:32.873Z"));
        assertThat(request.getRawMetadata()).containsEntry("id", "S2B_T52SCG_20261028T022157_L2A");
    }

    @Test
    void mapsFilesWithRolesSpacingAndBands() throws IOException {
        Map<String, ImageFileDetails> files = MAPPER.map(firstItem(), 7L).files();

        ImageFileDetails red = files.get(ITEM_DIR + "B04.tif");
        assertThat(red.assetKey()).isEqualTo("red");
        assertThat(red.role()).isEqualTo(ImageFileRole.DATA);
        assertThat(red.mediaType()).isEqualTo("image/tiff; application=geotiff; profile=cloud-optimized");
        assertThat(red.pixelSpacingMeters()).isEqualByComparingTo("10");
        assertThat(red.bands()).singleElement().satisfies(band -> {
            assertThat(band.getBandIndex()).isEqualTo((short) 1);
            assertThat(band.getBandName()).isEqualTo("B04");
            assertThat(band.getCommonName()).isEqualTo("red");
            assertThat(band.getCenterWavelengthUm()).isEqualByComparingTo("0.665");
            assertThat(band.getBandwidthUm()).isEqualByComparingTo("0.038");
            assertThat(band.getResolutionMeters()).isEqualByComparingTo("10");
        });
        assertThat(files.get(ITEM_DIR + "TCI.tif").role()).isEqualTo(ImageFileRole.PREVIEW);
        assertThat(files.get(ITEM_DIR + "TCI.tif").bands())
                .extracting(ImageFileBand::getCommonName)
                .containsExactly("red", "green", "blue");
        assertThat(files.get(ITEM_DIR + "L2A_PVI.jpg").role()).isEqualTo(ImageFileRole.THUMBNAIL);
        assertThat(files.values())
                .filteredOn(file -> file.assetKey().equals("granule_metadata"))
                .singleElement()
                .satisfies(file -> assertThat(file.role()).isEqualTo(ImageFileRole.METADATA));
    }

    @Test
    void acceptsInstrumentArray() throws IOException {
        ObjectNode item = firstItem();
        ((ObjectNode) item.path("properties")).putArray("instruments").add(" msi ");

        assertThat(MAPPER.map(item, 7L).request().getInstruments()).containsExactly("MSI");
    }
}
