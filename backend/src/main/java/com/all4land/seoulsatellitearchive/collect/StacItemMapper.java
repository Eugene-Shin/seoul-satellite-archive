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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

// STAC Item 공통 변환. 출처마다 다른 위성 이름, 픽셀 간격 위치, 파일 역할만 하위 클래스가 바꾼다.
// common_name은 출처 값 그대로 넘기고, 사전에 없는 값은 저장할 때 CollectService가 비운다
public abstract class StacItemMapper implements ItemMapper {

    private static final int ASSET_KEY_MAX_LENGTH = 200;

    @Override
    public final Mapped map(JsonNode item, Long collectionId) {
        JsonNode properties = item.path("properties");
        Map<String, ImageFileDetails> files = files(item);

        ImageWriteRequest request = new ImageWriteRequest();
        request.setCollectionId(collectionId);
        request.setSourceId(item.path("id").asString(null));
        request.setTitle(item.path("id").asString(null));
        request.setSatellite(satellite(properties.path("platform").asString(null)));
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

    // 대문자로 바꾸는 것은 ImageWriteRequest가 한다
    protected String satellite(String platform) {
        return platform;
    }

    // 에셋의 proj:transform으로 구한다. 격자가 UTM일 때만 단위가 미터다
    protected BigDecimal pixelSpacing(JsonNode item, JsonNode asset) {
        return isUtm(item.path("properties").path("proj:epsg")) ? pixelSpacing(asset.path("proj:transform")) : null;
    }

    protected ImageFileRole role(JsonNode asset) {
        Set<String> values = new HashSet<>();
        asset.path("roles").forEach(role -> values.add(role.asString(null)));
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

    // 북 326xx, 남 327xx
    protected static boolean isUtm(JsonNode epsg) {
        int code = epsg.asInt(0);
        return (code >= 32601 && code <= 32660) || (code >= 32701 && code <= 32760);
    }

    // [a, b, c, d, e, f]의 가로 √(a²+d²)·세로 √(b²+e²) 중 거친 값
    protected static BigDecimal pixelSpacing(JsonNode transform) {
        if (!transform.isArray() || transform.size() < 5) {
            return null;
        }
        double x = Math.hypot(transform.get(0).asDouble(), transform.get(3).asDouble());
        double y = Math.hypot(transform.get(1).asDouble(), transform.get(4).asDouble());
        return BigDecimal.valueOf(Math.max(x, y)).setScale(2, RoundingMode.HALF_UP);
    }

    // 문자열 하나로 주는 출처도 있다(S2 c1)
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
    private Map<String, ImageFileDetails> files(JsonNode item) {
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
                            role(asset),
                            asset.path("type").asString(null),
                            pixelSpacing(item, asset),
                            bands(asset)));
        }
        return files;
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
                    band.path("common_name").asString(null),
                    decimal(band.path("center_wavelength")),
                    decimal(band.path("full_width_half_max")),
                    decimal(gsd)));
        }
        return bands;
    }

    private static BigDecimal decimal(JsonNode node) {
        return node.isNumber() ? node.decimalValue() : null;
    }
}
