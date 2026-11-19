package com.all4land.seoulsatellitearchive.collect;

import static org.assertj.core.api.Assertions.assertThat;

import com.all4land.seoulsatellitearchive.global.config.TestcontainersConfiguration;
import com.all4land.seoulsatellitearchive.image.ImageFileBand;
import com.all4land.seoulsatellitearchive.image.ImageFileDetails;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class Sentinel2MapperDbTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void commonNamesMatchDbCheck() {
        String check = jdbc.queryForObject("""
                SELECT pg_get_constraintdef(oid)
                FROM pg_constraint
                WHERE conrelid = 'image_file_band'::regclass
                  AND contype = 'c'
                  AND pg_get_constraintdef(oid) LIKE '%common_name%'
                """, String.class);
        Matcher matcher = Pattern.compile("'([^']+)'").matcher(check);
        Set<String> dbNames = new HashSet<>();
        while (matcher.find()) {
            dbNames.add(matcher.group(1));
        }

        assertThat(dbNames).isEqualTo(StacItemMapper.COMMON_NAMES);
    }

    // 수집기가 만드는 파일·밴드 행이 V18 백필과 같아야 한다(시드 S2 영상으로 비교)
    @Test
    void filesMatchV18Backfill() {
        List<Map<String, Object>> images = jdbc.queryForList("""
                SELECT i.image_id, i.raw_metadata::text AS raw
                FROM satellite_image i
                         JOIN image_collection c ON c.collection_id = i.collection_id
                WHERE c.provider = 'earth-search'
                """);
        assertThat(images).isNotEmpty();

        for (Map<String, Object> image : images) {
            Long imageId = ((Number) image.get("image_id")).longValue();
            Map<String, ImageFileDetails> mapped = new Sentinel2Mapper()
                    .map(JsonMapper.shared().readTree((String) image.get("raw")), 1L)
                    .files();

            assertThat(mapperRows(mapped)).as("image %d", imageId).isEqualTo(dbRows(imageId));
        }
    }

    private static Set<String> mapperRows(Map<String, ImageFileDetails> files) {
        return files.entrySet().stream()
                .map(entry -> {
                    ImageFileDetails file = entry.getValue();
                    String bands = file.bands().stream()
                            .map(Sentinel2MapperDbTest::band)
                            .collect(Collectors.joining(";"));
                    return String.join(
                            "|",
                            file.assetKey(),
                            file.role().name(),
                            entry.getKey(),
                            String.valueOf(file.mediaType()),
                            plain(file.pixelSpacingMeters()),
                            bands);
                })
                .collect(Collectors.toSet());
    }

    private Set<String> dbRows(Long imageId) {
        return jdbc.queryForList("""
                SELECT f.asset_key, f.role, f.href, f.media_type, f.pixel_spacing_meters,
                       coalesce(string_agg(concat_ws(',', b.band_index, coalesce(b.band_name, 'null'),
                                                     coalesce(b.common_name, 'null'),
                                                     coalesce(trim_scale(b.center_wavelength_um)::text, 'null'),
                                                     coalesce(trim_scale(b.bandwidth_um)::text, 'null'),
                                                     coalesce(trim_scale(b.resolution_meters)::text, 'null')),
                                           ';' ORDER BY b.band_index) FILTER (WHERE b.file_id IS NOT NULL), '') AS bands
                FROM image_file f
                         LEFT JOIN image_file_band b ON b.file_id = f.file_id
                WHERE f.image_id = ?
                GROUP BY f.file_id
                """, imageId).stream()
                .map(row -> String.join(
                        "|",
                        (String) row.get("asset_key"),
                        (String) row.get("role"),
                        (String) row.get("href"),
                        String.valueOf(row.get("media_type")),
                        plain((BigDecimal) row.get("pixel_spacing_meters")),
                        (String) row.get("bands")))
                .collect(Collectors.toSet());
    }

    private static String band(ImageFileBand band) {
        return String.join(
                ",",
                String.valueOf(band.getBandIndex()),
                String.valueOf(band.getBandName()),
                String.valueOf(band.getCommonName()),
                plain(band.getCenterWavelengthUm()),
                plain(band.getBandwidthUm()),
                plain(band.getResolutionMeters()));
    }

    // DB 칸은 소수 자릿수가 정해져 있어서(10.00) 끝의 0을 떼고 비교한다
    private static String plain(BigDecimal value) {
        return value == null ? "null" : value.stripTrailingZeros().toPlainString();
    }
}
