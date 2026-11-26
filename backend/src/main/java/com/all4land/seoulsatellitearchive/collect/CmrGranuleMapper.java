package com.all4land.seoulsatellitearchive.collect;

import com.all4land.seoulsatellitearchive.image.ImageFileDetails;
import com.all4land.seoulsatellitearchive.image.ImageFileRole;
import com.all4land.seoulsatellitearchive.image.dto.ImageWriteRequest;
import com.all4land.seoulsatellitearchive.image.dto.ImageWriteRequest.SarRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

// NASA CMR 그래뉼(UMM-G: meta + umm) 공통 변환. 파일 규칙은 V18 CMR 백필과 같다.
// 제품마다 다른 위성·센서 이름, 센서 종류, 구름량, 궤도 방향, SAR 기하만 하위 클래스가 바꾼다. CMR은 밴드 정보를 주지 않아서
// 밴드는 저장할 때 밴드 규칙(collect_band_rule)으로 붙인다
public abstract class CmrGranuleMapper implements ItemMapper {

    @Override
    public final Mapped map(JsonNode item, Long collectionId) {
        JsonNode meta = item.path("meta");
        JsonNode umm = item.path("umm");
        Map<String, ImageFileDetails> files = files(umm);

        ImageWriteRequest request = new ImageWriteRequest();
        request.setCollectionId(collectionId);
        request.setSourceId(umm.path("GranuleUR").asString(null));
        request.setTitle(umm.path("GranuleUR").asString(null));
        request.setSatellite(satellite(umm));
        request.setInstruments(instruments(umm.path("Platforms")));
        request.setSensorType(sensorType());
        JsonNode range = umm.path("TemporalExtent").path("RangeDateTime");
        request.setAcquiredAt(dateTime(range.path("BeginningDateTime")));
        request.setAcquiredEndAt(dateTime(range.path("EndingDateTime")));
        request.setCloudCover(cloudCover(umm));
        request.setOrbitDirection(orbitDirection(umm));
        request.setSar(sar(umm));
        request.setFootprint(footprint(umm));
        request.setUrl("https://cmr.earthdata.nasa.gov/search/concepts/"
                + meta.path("concept-id").asString());
        request.setDownloadUrls(List.copyOf(files.keySet()));
        request.setSourceUpdatedAt(dateTime(meta.path("revision-date")));
        request.setRawMetadata(JsonMapper.shared().convertValue(item, Map.class));
        return new Mapped(request, files);
    }

    // MGRS 타일(HLS). 타일 속성이 없는 제품은 하위 클래스가 바꾼다
    @Override
    public String grid(JsonNode item) {
        return attribute(item.path("umm"), "MGRS_TILE_ID");
    }

    protected String satellite(JsonNode umm) {
        return umm.path("Platforms").path(0).path("ShortName").asString(null);
    }

    protected List<String> instruments(JsonNode platforms) {
        List<String> values = new ArrayList<>();
        for (JsonNode platform : platforms) {
            for (JsonNode instrument : platform.path("Instruments")) {
                values.add(instrument.path("ShortName").asString(null));
            }
        }
        return values;
    }

    protected String sensorType() {
        return "OPTICAL";
    }

    // 출처마다 구름량을 주는 속성이 다르다. 없으면 비운다
    protected BigDecimal cloudCover(JsonNode umm) {
        return null;
    }

    protected String orbitDirection(JsonNode umm) {
        return null;
    }

    // SAR 촬영 기하(image_sar). 광학은 없다
    protected SarRequest sar(JsonNode umm) {
        return null;
    }

    protected static String attribute(JsonNode umm, String name) {
        for (JsonNode attribute : umm.path("AdditionalAttributes")) {
            if (name.equals(attribute.path("Name").asString(null))) {
                return attribute.path("Values").path(0).asString(null);
            }
        }
        return null;
    }

    protected static BigDecimal decimal(String value, Integer scale) {
        if (value == null) {
            return null;
        }
        BigDecimal number = new BigDecimal(value);
        return scale == null ? number : number.setScale(scale, RoundingMode.HALF_UP);
    }

    // GPolygons는 바깥 경계만 쓰고, 사각형으로만 주는 제품(ECOSTRESS 타일)은 BoundingRectangles를 다각형으로 바꾼다
    private static JsonNode footprint(JsonNode umm) {
        JsonNode geometry =
                umm.path("SpatialExtent").path("HorizontalSpatialDomain").path("Geometry");
        ObjectNode multi = JsonNodeFactory.instance.objectNode();
        multi.put("type", "MultiPolygon");
        ArrayNode polygons = multi.putArray("coordinates");
        for (JsonNode polygon : geometry.path("GPolygons")) {
            ArrayNode ring = polygons.addArray().addArray();
            for (JsonNode point : polygon.path("Boundary").path("Points")) {
                ring.addArray()
                        .add(point.path("Longitude").asDouble())
                        .add(point.path("Latitude").asDouble());
            }
        }
        for (JsonNode rectangle : geometry.path("BoundingRectangles")) {
            double west = rectangle.path("WestBoundingCoordinate").asDouble();
            double east = rectangle.path("EastBoundingCoordinate").asDouble();
            double south = rectangle.path("SouthBoundingCoordinate").asDouble();
            double north = rectangle.path("NorthBoundingCoordinate").asDouble();
            ArrayNode ring = polygons.addArray().addArray();
            ring.addArray().add(west).add(south);
            ring.addArray().add(east).add(south);
            ring.addArray().add(east).add(north);
            ring.addArray().add(west).add(north);
            ring.addArray().add(west).add(south);
        }
        return multi;
    }

    // https 링크만. s3:// 직접 접근, 안내 페이지(s3credentials 등), OPeNDAP 같은 서비스 주소는 파일이 아니라 뺀다.
    // 데이터 파일은 그래뉼의 SPATIAL_RESOLUTION을 픽셀 간격으로 쓴다(없으면 비운다)
    private Map<String, ImageFileDetails> files(JsonNode umm) {
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
                            List.of()));
        }
        return files;
    }

    // 안내 링크(VIEW RELATED INFORMATION) 중 xml·json은 메타데이터, 이미지는 레이어별 미리보기(ECOSTRESS)다.
    // 이미지는 V18 백필 이후에 추가한 규칙이다
    private static ImageFileRole role(String type, String url) {
        return switch (type) {
            case "GET DATA" ->
                url.matches("(?i).*\\.(zip|tar|tar\\.gz|tgz)$") ? ImageFileRole.ORIGINAL : ImageFileRole.DATA;
            case "GET RELATED VISUALIZATION" -> ImageFileRole.THUMBNAIL;
            case "VIEW RELATED INFORMATION" -> {
                if (url.matches("(?i).*\\.(xml|json)$")) {
                    yield ImageFileRole.METADATA;
                }
                yield url.matches("(?i).*\\.(jpe?g|png)$") ? ImageFileRole.PREVIEW : null;
            }
            default -> null;
        };
    }

    private static OffsetDateTime dateTime(JsonNode node) {
        return node.isString() ? OffsetDateTime.parse(node.asString()) : null;
    }
}
