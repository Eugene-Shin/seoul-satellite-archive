package com.all4land.seoulsatellitearchive.collect;

import com.all4land.seoulsatellitearchive.image.ImageFileBand;
import com.all4land.seoulsatellitearchive.image.ImageFileDetails;
import com.all4land.seoulsatellitearchive.image.ImageFileRole;
import com.all4land.seoulsatellitearchive.image.dto.ImageWriteRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

// Earth Search Sentinel-2 STAC Item → 영상 저장 요청과 파일 목록. 파일 규칙은 V18 백필과 같다
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Sentinel2Mapper {

    public static final String COLLECTION = "sentinel-2-c1-l2a";

    private static final int ASSET_KEY_MAX_LENGTH = 200;
    // image_file_band.common_name CHECK와 같은 목록. 어긋나면 Sentinel2MapperDbTest가 실패한다
    static final Set<String> COMMON_NAMES = Set.of(
            "pan",
            "coastal",
            "blue",
            "green",
            "green05",
            "yellow",
            "red",
            "rededge",
            "rededge071",
            "rededge075",
            "rededge078",
            "nir",
            "nir08",
            "nir09",
            "cirrus",
            "swir16",
            "swir22",
            "lwir",
            "lwir11",
            "lwir12");

    public record Mapped(ImageWriteRequest request, Map<String, ImageFileDetails> files) {}

    public static Mapped map(JsonNode item, Long collectionId) {
        JsonNode properties = item.path("properties");
        Map<String, ImageFileDetails> files = files(item);

        ImageWriteRequest request = new ImageWriteRequest();
        request.setCollectionId(collectionId);
        request.setSourceId(item.path("id").asString(null));
        request.setTitle(item.path("id").asString(null));
        request.setSatellite(properties.path("platform").asString(null));
        request.setInstruments(instruments(properties.path("instruments")));
        request.setSensorType("OPTICAL");
        request.setAcquiredAt(dateTime(properties.path("datetime")));
        request.setAcquiredEndAt(dateTime(properties.path("end_datetime")));
        request.setCloudCover(cloudCover(properties.path("eo:cloud_cover")));
        request.setFootprint(multiPolygon(item.path("geometry")));
        request.setUrl(selfLink(item));
        request.setDownloadUrls(List.copyOf(files.keySet()));
        request.setSourceUpdatedAt(dateTime(properties.path("updated")));
        request.setRawMetadata(JsonMapper.shared().convertValue(item, Map.class));
        return new Mapped(request, files);
    }

    // c1은 문자열 하나, 이전 컬렉션은 배열로 준다
    private static List<String> instruments(JsonNode node) {
        if (node.isString()) {
            return List.of(node.asString());
        }
        if (!node.isArray()) {
            return null;
        }
        List<String> values = new ArrayList<>();
        node.forEach(value -> values.add(value.asString(null)));
        return values;
    }

    private static OffsetDateTime dateTime(JsonNode node) {
        return node.isString() ? OffsetDateTime.parse(node.asString()) : null;
    }

    // 컬럼이 소수 둘째 자리까지다
    private static BigDecimal cloudCover(JsonNode node) {
        return node.isNumber() ? node.decimalValue().setScale(2, RoundingMode.HALF_UP) : null;
    }

    private static JsonNode multiPolygon(JsonNode geometry) {
        if (!"Polygon".equals(geometry.path("type").asString(null))) {
            return geometry;
        }
        ObjectNode multi = JsonNodeFactory.instance.objectNode();
        multi.put("type", "MultiPolygon");
        multi.putArray("coordinates").add(geometry.path("coordinates"));
        return multi;
    }

    private static String selfLink(JsonNode item) {
        for (JsonNode link : item.path("links")) {
            if ("self".equals(link.path("rel").asString(null))) {
                return link.path("href").asString(null);
            }
        }
        return null;
    }

    // 링크가 같은 에셋이 여럿이면 앞의 것만 둔다
    private static Map<String, ImageFileDetails> files(JsonNode item) {
        boolean meterGrid = isUtm(item.path("properties").path("proj:epsg"));
        Map<String, ImageFileDetails> files = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> entry : item.path("assets").properties()) {
            JsonNode asset = entry.getValue();
            String href = asset.path("href").asString(null);
            if (href == null || files.containsKey(href)) {
                continue;
            }
            String key = entry.getKey();
            files.put(
                    href,
                    new ImageFileDetails(
                            key.length() > ASSET_KEY_MAX_LENGTH ? key.substring(0, ASSET_KEY_MAX_LENGTH) : key,
                            role(asset.path("roles")),
                            asset.path("type").asString(null),
                            meterGrid ? pixelSpacing(asset.path("proj:transform")) : null,
                            bands(asset)));
        }
        return files;
    }

    private static ImageFileRole role(JsonNode roles) {
        Set<String> values = new HashSet<>();
        roles.forEach(role -> values.add(role.asString(null)));
        if (values.contains("thumbnail")) {
            return ImageFileRole.THUMBNAIL;
        }
        if (values.contains("overview") || values.contains("visual")) {
            return ImageFileRole.PREVIEW;
        }
        if (values.contains("metadata")) {
            return ImageFileRole.METADATA;
        }
        return ImageFileRole.DATA;
    }

    // Sentinel-2 격자는 UTM(북 326xx, 남 327xx)이라 proj:transform 단위가 미터다
    private static boolean isUtm(JsonNode epsg) {
        int code = epsg.asInt(0);
        return (code >= 32601 && code <= 32660) || (code >= 32701 && code <= 32760);
    }

    // [a, b, c, d, e, f]의 가로 √(a²+d²)·세로 √(b²+e²) 중 거친 값
    private static BigDecimal pixelSpacing(JsonNode transform) {
        if (!transform.isArray() || transform.size() < 5) {
            return null;
        }
        double x = Math.hypot(transform.get(0).asDouble(), transform.get(3).asDouble());
        double y = Math.hypot(transform.get(1).asDouble(), transform.get(4).asDouble());
        return BigDecimal.valueOf(Math.max(x, y)).setScale(2, RoundingMode.HALF_UP);
    }

    private static List<ImageFileBand> bands(JsonNode asset) {
        List<ImageFileBand> bands = new ArrayList<>();
        JsonNode eoBands = asset.path("eo:bands");
        for (int i = 0; i < eoBands.size(); i++) {
            JsonNode band = eoBands.get(i);
            JsonNode gsd = band.has("gsd") ? band.path("gsd") : asset.path("gsd");
            bands.add(new ImageFileBand(
                    (short) (i + 1),
                    band.path("name").asString(null),
                    commonName(band.path("common_name").asString(null)),
                    decimal(band.path("center_wavelength")),
                    decimal(band.path("full_width_half_max")),
                    decimal(gsd)));
        }
        return bands;
    }

    private static String commonName(String value) {
        if (value == null || COMMON_NAMES.contains(value)) {
            return value;
        }
        log.warn("표준이 아닌 common_name을 비웁니다: {}", value);
        return null;
    }

    private static BigDecimal decimal(JsonNode node) {
        return node.isNumber() ? node.decimalValue() : null;
    }
}
