package com.all4land.seoulsatellitearchive.collect;

import com.all4land.seoulsatellitearchive.image.ImageFileBand;
import com.all4land.seoulsatellitearchive.image.ImageFileDetails;
import com.all4land.seoulsatellitearchive.image.ImageFileRole;
import com.all4land.seoulsatellitearchive.image.dto.ImageWriteRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

// NASA CMR HLS 그래뉼(UMM-G: meta + umm). 파일 규칙은 V18 CMR 백필과 같다.
// CMR은 밴드 정보를 주지 않아서 파일 이름의 밴드 이름만 넣고, 분류·파장·해상도는 저장할 때 collection_band에서 채운다
public class HlsMapper implements ItemMapper {

    private static final Pattern BAND_FILE = Pattern.compile("\\.(B\\d\\d|B8A)\\.tif$");

    @Override
    public Set<String> collections() {
        return Set.of("C2021957657-LPCLOUD", "C2021957295-LPCLOUD");
    }

    @Override
    public Mapped map(JsonNode item, Long collectionId) {
        JsonNode meta = item.path("meta");
        JsonNode umm = item.path("umm");
        Map<String, ImageFileDetails> files = files(umm);

        ImageWriteRequest request = new ImageWriteRequest();
        request.setCollectionId(collectionId);
        request.setSourceId(umm.path("GranuleUR").asString(null));
        request.setTitle(umm.path("GranuleUR").asString(null));
        request.setSatellite(umm.path("Platforms").path(0).path("ShortName").asString(null));
        request.setInstruments(instruments(umm.path("Platforms")));
        request.setSensorType("OPTICAL");
        JsonNode range = umm.path("TemporalExtent").path("RangeDateTime");
        request.setAcquiredAt(dateTime(range.path("BeginningDateTime")));
        request.setAcquiredEndAt(dateTime(range.path("EndingDateTime")));
        request.setCloudCover(decimal(attribute(umm, "CLOUD_COVERAGE"), 2));
        request.setFootprint(footprint(umm));
        request.setUrl("https://cmr.earthdata.nasa.gov/search/concepts/"
                + meta.path("concept-id").asString());
        request.setDownloadUrls(List.copyOf(files.keySet()));
        request.setSourceUpdatedAt(dateTime(meta.path("revision-date")));
        request.setRawMetadata(JsonMapper.shared().convertValue(item, Map.class));
        return new Mapped(request, files);
    }

    // 같은 센서를 S2 c1은 MSI, HLS S30은 "Sentinel-2 MSI"로 준다. 센서로 검색할 때 함께 나오도록 맞춘다
    private static List<String> instruments(JsonNode platforms) {
        List<String> values = new ArrayList<>();
        for (JsonNode platform : platforms) {
            for (JsonNode instrument : platform.path("Instruments")) {
                values.add(instrument.path("ShortName").asString("").replaceFirst("^Sentinel-2 ", ""));
            }
        }
        return values;
    }

    private static String attribute(JsonNode umm, String name) {
        for (JsonNode attribute : umm.path("AdditionalAttributes")) {
            if (name.equals(attribute.path("Name").asString(null))) {
                return attribute.path("Values").path(0).asString(null);
            }
        }
        return null;
    }

    // GPolygons의 바깥 경계만 쓴다
    private static JsonNode footprint(JsonNode umm) {
        ObjectNode multi = JsonNodeFactory.instance.objectNode();
        multi.put("type", "MultiPolygon");
        ArrayNode polygons = multi.putArray("coordinates");
        for (JsonNode polygon : umm.path("SpatialExtent")
                .path("HorizontalSpatialDomain")
                .path("Geometry")
                .path("GPolygons")) {
            ArrayNode ring = polygons.addArray().addArray();
            for (JsonNode point : polygon.path("Boundary").path("Points")) {
                ring.addArray()
                        .add(point.path("Longitude").asDouble())
                        .add(point.path("Latitude").asDouble());
            }
        }
        return multi;
    }

    // https 링크만. s3:// 직접 접근, 안내 페이지(s3credentials 등), OPeNDAP 같은 서비스 주소는 파일이 아니라 뺀다
    private static Map<String, ImageFileDetails> files(JsonNode umm) {
        BigDecimal spatialResolution = decimal(attribute(umm, "SPATIAL_RESOLUTION"), null);
        Map<String, ImageFileDetails> files = new LinkedHashMap<>();
        for (JsonNode related : umm.path("RelatedUrls")) {
            String url = related.path("URL").asString("");
            ImageFileRole role = role(related.path("Type").asString(""), url);
            if (!url.startsWith("https://")
                    || role == null
                    || related.path("Subtype")
                            .asString("")
                            .matches("(?i).*(OPENDAP|THREDDS|WMS|WMTS|WCS|WFS|SUBSET|SERVICE|API).*")
                    || files.containsKey(url)) {
                continue;
            }
            files.put(
                    url,
                    new ImageFileDetails(
                            url.substring(url.lastIndexOf('/') + 1),
                            role,
                            related.path("MimeType").asString(null),
                            role == ImageFileRole.DATA ? spatialResolution : null,
                            band(url)));
        }
        return files;
    }

    private static ImageFileRole role(String type, String url) {
        return switch (type) {
            case "GET DATA" ->
                url.matches("(?i).*\\.(zip|tar|tar\\.gz|tgz)$") ? ImageFileRole.ORIGINAL : ImageFileRole.DATA;
            case "GET RELATED VISUALIZATION" -> ImageFileRole.THUMBNAIL;
            case "VIEW RELATED INFORMATION" -> url.matches("(?i).*\\.(xml|json)$") ? ImageFileRole.METADATA : null;
            default -> null;
        };
    }

    // 파일 하나에 밴드 하나. Fmask와 각도 파일(SZA 등)은 밴드가 아니다
    private static List<ImageFileBand> band(String url) {
        Matcher matcher = BAND_FILE.matcher(url);
        if (!matcher.find()) {
            return List.of();
        }
        return List.of(new ImageFileBand((short) 1, matcher.group(1), null, null, null, null));
    }

    private static OffsetDateTime dateTime(JsonNode node) {
        return node.isString() ? OffsetDateTime.parse(node.asString()) : null;
    }

    private static BigDecimal decimal(String value, Integer scale) {
        if (value == null) {
            return null;
        }
        BigDecimal number = new BigDecimal(value);
        return scale == null ? number : number.setScale(scale, RoundingMode.HALF_UP);
    }
}
