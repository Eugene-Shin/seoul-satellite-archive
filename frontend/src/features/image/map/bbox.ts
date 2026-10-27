import type { Extent } from "ol/extent";
import { transformExtent } from "ol/proj";

import type { Bbox } from "@/features/image/types";

const MAX_LON = 180;
const MAX_LAT = 90;
const COORDINATE_DIGITS = 6;

function round(value: number): number {
    return Number(value.toFixed(COORDINATE_DIGITS));
}

export function toBbox(values: readonly number[]): Bbox | null {
    if (values.length !== 4) {
        return null;
    }

    const [minLon, minLat, maxLon, maxLat] = values.map(round);
    const isValid =
        Math.abs(minLon) <= MAX_LON &&
        Math.abs(maxLon) <= MAX_LON &&
        Math.abs(minLat) <= MAX_LAT &&
        Math.abs(maxLat) <= MAX_LAT &&
        minLon <= maxLon &&
        minLat <= maxLat;

    return isValid ? [minLon, minLat, maxLon, maxLat] : null;
}

export function extentToBbox(extent: Extent): Bbox | null {
    return toBbox(transformExtent(extent, "EPSG:3857", "EPSG:4326"));
}

export function bboxToExtent(bbox: Bbox): Extent {
    return transformExtent(bbox, "EPSG:4326", "EPSG:3857");
}
