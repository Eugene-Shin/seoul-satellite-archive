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
