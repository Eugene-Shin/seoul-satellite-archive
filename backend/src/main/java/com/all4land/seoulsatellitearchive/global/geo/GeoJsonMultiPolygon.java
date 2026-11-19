package com.all4land.seoulsatellitearchive.global.geo;

import com.all4land.seoulsatellitearchive.global.exception.BusinessException;
import com.all4land.seoulsatellitearchive.global.response.CommonErrorCode;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import tools.jackson.databind.JsonNode;

// 요청으로 받은 GeoJSON MultiPolygon을 경위도(SRID 4326) 도형으로 바꾼다. 잘못된 값은 field 이름을 붙여 400으로 알린다
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GeoJsonMultiPolygon {

    private static final GeometryFactory WGS84 = new GeometryFactory(new PrecisionModel(), 4326);

    public static MultiPolygon parse(JsonNode geoJson, String field) {
        try {
            if (geoJson == null
                    || !geoJson.isObject()
                    || !geoJson.path("type").isString()
                    || !"MultiPolygon".equals(geoJson.path("type").stringValue())) {
                throw new IllegalArgumentException();
            }
            JsonNode coordinates = geoJson.path("coordinates");
            if (!coordinates.isArray() || coordinates.isEmpty()) {
                throw new IllegalArgumentException();
            }
            List<Polygon> polygons = new ArrayList<>();
            for (JsonNode polygon : coordinates) {
                if (!polygon.isArray() || polygon.isEmpty()) {
                    throw new IllegalArgumentException();
                }
                LinearRing shell = toRing(polygon.get(0));
                LinearRing[] holes = new LinearRing[polygon.size() - 1];
                for (int i = 1; i < polygon.size(); i++) {
                    holes[i - 1] = toRing(polygon.get(i));
                }
                polygons.add(WGS84.createPolygon(shell, holes));
            }
            MultiPolygon result = WGS84.createMultiPolygon(polygons.toArray(Polygon[]::new));
            if (result.isEmpty() || !result.isValid()) {
                throw new IllegalArgumentException();
            }
            return result;
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(
                    CommonErrorCode.BAD_REQUEST, field + ": 경위도 범위의 비어 있지 않은 유효한 2차원 GeoJSON MultiPolygon이어야 합니다.");
        }
    }

    private static LinearRing toRing(JsonNode ring) {
        if (!ring.isArray() || ring.size() < 4) {
            throw new IllegalArgumentException();
        }
        Coordinate[] coordinates = new Coordinate[ring.size()];
        for (int i = 0; i < ring.size(); i++) {
            JsonNode position = ring.get(i);
            if (!position.isArray()
                    || position.size() != 2
                    || !position.get(0).isNumber()
                    || !position.get(1).isNumber()) {
                throw new IllegalArgumentException();
            }
            double lon = position.get(0).doubleValue();
            double lat = position.get(1).doubleValue();
            if (!Double.isFinite(lon) || !Double.isFinite(lat) || lon < -180 || lon > 180 || lat < -90 || lat > 90) {
                throw new IllegalArgumentException();
            }
            coordinates[i] = new Coordinate(lon, lat);
        }
        return WGS84.createLinearRing(coordinates);
    }
}
