package com.all4land.seoulsatellitearchive.archive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.all4land.seoulsatellitearchive.archive.ArchivePaths.Scene;
import com.all4land.seoulsatellitearchive.collect.EcostressMapper;
import com.all4land.seoulsatellitearchive.collect.HlsMapper;
import com.all4land.seoulsatellitearchive.collect.ItemMapper;
import com.all4land.seoulsatellitearchive.collect.LandsatMapper;
import com.all4land.seoulsatellitearchive.collect.ModisLstMapper;
import com.all4land.seoulsatellitearchive.collect.Sentinel1SlcMapper;
import com.all4land.seoulsatellitearchive.collect.Sentinel2Mapper;
import com.all4land.seoulsatellitearchive.image.ImageFile;
import com.all4land.seoulsatellitearchive.image.ImageFileDetails;
import com.all4land.seoulsatellitearchive.image.ImageFileRole;
import com.all4land.seoulsatellitearchive.image.dto.ImageWriteRequest;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// 수집 매퍼 픽스처(실제 출처 응답)를 매퍼에 통과시킨 결과로 경로를 만든다
class ArchivePathsTest {

    private static JsonNode item(String fixture, String list, int index) throws IOException {
        try (InputStream in = ArchivePathsTest.class.getResourceAsStream("/collect/" + fixture + "-search.json")) {
            return JsonMapper.shared().readTree(in).path(list).get(index);
        }
    }

    // 링크 → 경로
    private static Map<String, String> paths(
            ItemMapper mapper, JsonNode item, String code, String constellation, String productType) {
        ItemMapper.Mapped mapped = mapper.map(item, 1L);
        ImageWriteRequest request = mapped.request();
        Scene scene = new Scene(
                constellation,
                productType,
                code,
                request.getSourceId(),
                request.getAcquiredAt().toInstant(),
                request.getOrbitDirection(),
                request.getSar() == null ? null : request.getSar().relativeOrbit(),
                item);
        List<ImageFile> files = mapped.files().entrySet().stream()
                .map(entry -> new ImageFile(null, entry.getKey(), entry.getValue()))
                .toList();
        return ArchivePaths.paths(scene, files).entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().getHref(), Map.Entry::getValue));
    }

    private static List<String> names(Collection<String> paths) {
        return paths.stream()
                .map(path -> path.substring(path.lastIndexOf('/') + 1))
                .toList();
    }

    private static String folder(Collection<String> paths) {
        String path = paths.iterator().next();
        return path.substring(0, path.lastIndexOf('/'));
    }

    @Test
    void sentinel2UsesMgrsAndBandNames() throws IOException {
        Map<String, String> paths = paths(
                new Sentinel2Mapper(),
                item("sentinel-2-c1-l2a", "features", 0),
                "sentinel-2-c1-l2a",
                "SENTINEL-2",
                "L2A");

        assertThat(folder(paths.values()))
                .isEqualTo("SENTINEL-2/L2A/52SCG/20261028T022713_S2B_T52SCG_20261028T022157_L2A");
        assertThat(names(paths.values())).contains("B04_red.tif", "visual.tif", "thumbnail.jpg");
    }

    @Test
    void landsatUsesWrsPathRow() throws IOException {
        Map<String, String> paths = paths(
                new LandsatMapper(), item("landsat-c2l2-sr", "features", 0), "landsat-c2l2-sr", "LANDSAT", "L2SR");

        assertThat(folder(paths.values())).startsWith("LANDSAT/L2SR/116-034/");
        assertThat(names(paths.values())).contains("B4_red.TIF");
    }

    // 매퍼 결과만 본다. CMR은 밴드를 저장할 때 규칙으로 붙이므로(B05_nir08.tif) 여기서는 장면 ID만 뗀 이름이다
    @Test
    void hlsMapperFilesStripSceneIdAndKeepThumbnailName() throws IOException {
        Map<String, String> paths =
                paths(new HlsMapper(), item("hls", "items", 0), "C2021957657-LPCLOUD", "HLS", "L30");

        assertThat(folder(paths.values()))
                .isEqualTo("HLS/L30/52SCG/20261006T021105_HLS.L30.T52SCG.2026279T021105.v2.0");
        assertThat(names(paths.values())).contains("B05.tif", "Fmask.tif", "HLS.L30.T52SCG.2026279T021105.v2.0.jpg");
    }

    @Test
    void ecostressLayersKeepLayerNames() throws IOException {
        Map<String, String> paths =
                paths(new EcostressMapper(), item("lst", "items", 0), "C3998139651-LPCLOUD", "ECOSTRESS", "L2T_LSTE");

        assertThat(folder(paths.values()))
                .isEqualTo(
                        "ECOSTRESS/L2T_LSTE/52SCG/20261101T210238_ECOv003_L2T_LSTE_47208_011_52SCG_20261101T210238_01");
        assertThat(names(paths.values())).contains("LST.tif", "LST.jpeg", "LST.jpeg.aux.xml", "cmr.xml");
    }

    @Test
    void modisUsesSinusoidalTileAndKeepsHdfName() throws IOException {
        Map<String, String> paths =
                paths(new ModisLstMapper(), item("lst", "items", 1), "C1748058432-LPCLOUD", "MODIS", "MOD11A1");

        assertThat(folder(paths.values())).startsWith("MODIS/MOD11A1/h28v05/");
        assertThat(names(paths.values())).contains("MOD11A1.A2026305.h28v05.061.2026306092403.hdf");
    }

    @Test
    void sentinel1UsesDirectionAndTrackAndOriginalZip() throws IOException {
        Map<String, String> paths =
                paths(new Sentinel1SlcMapper(), item("s1", "items", 0), "C3470873558-ASF", "SENTINEL-1", "SLC");

        assertThat(folder(paths.values())).matches("SENTINEL-1/SLC/(ASC|DSC)-\\d{3}/\\d{8}T\\d{6}_S1C_IW_SLC__.*-SLC");
        assertThat(names(paths.values())).contains("original.zip");
    }

    // SAR 궤도 정보가 하나라도 없으면 _
    @Test
    void missingTrackIsUnderscore() {
        Scene scene = scene("SENTINEL-1", "SLC", "ASCENDING", null);

        assertThat(ArchivePaths.paths(scene, List.of(file("https://x.test/a.zip", ImageFileRole.ORIGINAL))))
                .containsValue("SENTINEL-1/SLC/_/20261101T000000_S1A_X/original.zip");
    }

    // 대소문자만 다른 이름과 우리가 만드는 이름(metadata.json)은 겹친 것으로 보고 번호를 붙인다
    @Test
    void collidingNamesGetNumbers() {
        Map<ImageFile, String> paths = ArchivePaths.paths(
                scene("PAZ", "SSC", null, null),
                List.of(
                        file("https://x.test/README.txt", ImageFileRole.METADATA),
                        file("https://x.test/readme.txt", ImageFileRole.METADATA),
                        file("https://x.test/metadata.json", ImageFileRole.METADATA)));

        assertThat(names(paths.values())).containsExactly("README.txt", "readme_2.txt", "metadata_2.json");
    }

    // 허용하지 않는 문자는 _로 바꾸고 상위 폴더(..)를 막는다
    @Test
    void unsafeCharactersAreReplaced() {
        Map<ImageFile, String> paths = ArchivePaths.paths(
                scene("PAZ", "SSC", null, null), List.of(file("https://x.test/a b..c%.txt", ImageFileRole.METADATA)));

        assertThat(names(paths.values())).containsExactly("a_b__c_.txt");
    }

    @Test
    void tooLongPathIsRejected() {
        Scene scene =
                new Scene("PAZ", "SSC", "x", "S".repeat(ArchivePaths.MAX_LENGTH), Instant.EPOCH, null, null, null);

        assertThatThrownBy(() -> ArchivePaths.paths(scene, List.of(file("https://x.test/a.txt", ImageFileRole.DATA))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Scene scene(String constellation, String productType, String orbitDirection, Integer track) {
        return new Scene(
                constellation,
                productType,
                "no-mapper",
                "S1A_X",
                Instant.parse("2026-11-01T00:00:00Z"),
                orbitDirection,
                track,
                null);
    }

    private static ImageFile file(String href, ImageFileRole role) {
        String name = href.substring(href.lastIndexOf('/') + 1);
        return new ImageFile(null, href, new ImageFileDetails(name, role, null, null, List.of()));
    }
}
