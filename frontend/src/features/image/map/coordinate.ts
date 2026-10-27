import type { Coordinate } from "ol/coordinate";

// 지도 이동 범위가 북위·동경 안이라 N/E로 고정한다. 소수 5자리 ≈ 1m
export function formatLatLon(coordinate?: Coordinate): string {
    if (!coordinate) {
        return "";
    }
    const [lon, lat] = coordinate;
    return `${lat.toFixed(5)}°N ${lon.toFixed(5)}°E`;
}
