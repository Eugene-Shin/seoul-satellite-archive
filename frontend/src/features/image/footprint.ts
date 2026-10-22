import Feature from "ol/Feature";
import MultiPolygon from "ol/geom/MultiPolygon";
import type { OrderFunction } from "ol/render";

import type { ImageResponse } from "@/features/image/types";

export const largerFirst: OrderFunction = (a, b) =>
    b.get("area") - a.get("area");

export function toFeature(
    image: Pick<ImageResponse, "imageId" | "footprint">,
): Feature<MultiPolygon> {
    const geometry = new MultiPolygon(image.footprint.coordinates).transform(
        "EPSG:4326",
        "EPSG:3857",
    );
    const feature = new Feature({ geometry, area: geometry.getArea() });
    feature.setId(image.imageId);
    return feature;
}

export function pickImages<T extends Pick<ImageResponse, "imageId">>(
    images: T[],
    ids: ReadonlyArray<string | number | undefined>,
): T[] {
    return images.filter((image) => ids.includes(image.imageId));
}

// 순서는 보지 않는다. 지도에서 맞은 순서와 결과 목록 순서가 달라서
export function sameIds(a: readonly number[], b: readonly number[]): boolean {
    return a.every((id) => b.includes(id)) && b.every((id) => a.includes(id));
}
