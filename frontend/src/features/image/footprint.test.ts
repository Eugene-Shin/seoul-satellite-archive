import { fromLonLat } from "ol/proj";
import { describe, expect, it } from "vitest";

import {
    largerFirst,
    pickImages,
    sameIds,
    toFeature,
} from "@/features/image/footprint";

function footprint(lon: number, lat: number, size: number) {
    return {
        type: "MultiPolygon" as const,
        coordinates: [
            [
                [
                    [lon, lat],
                    [lon + size, lat],
                    [lon + size, lat + size],
                    [lon, lat + size],
                    [lon, lat],
                ],
            ],
        ],
    };
}

describe("toFeature", () => {
    it("id는 imageId, 좌표는 지도 좌표(EPSG:3857)로", () => {
        const feature = toFeature({
            imageId: 7,
            footprint: footprint(126.9, 37.5, 0.1),
        });

        const [x, y] = feature.getGeometry()?.getFlatCoordinates() ?? [];
        const [expectedX, expectedY] = fromLonLat([126.9, 37.5]);
        expect(feature.getId()).toBe(7);
        expect(x).toBeCloseTo(expectedX);
        expect(y).toBeCloseTo(expectedY);
    });

    it("넓이를 속성으로 미리 계산해 둔다", () => {
        const feature = toFeature({
            imageId: 1,
            footprint: footprint(126.9, 37.5, 0.1),
        });

        expect(feature.get("area")).toBeGreaterThan(0);
    });
});

describe("largerFirst", () => {
    it("넓은 footprint가 먼저(아래에) 오게 정렬한다", () => {
        const small = toFeature({
            imageId: 1,
            footprint: footprint(127, 37, 0.1),
        });
        const large = toFeature({
            imageId: 2,
            footprint: footprint(127, 37, 1),
        });

        const sorted = [small, large].sort(largerFirst);

        expect(sorted.map((feature) => feature.getId())).toEqual([2, 1]);
    });
});

describe("pickImages", () => {
    const images = [{ imageId: 3 }, { imageId: 1 }, { imageId: 2 }];

    it("id 순서가 아니라 결과 목록 순서를 유지한다", () => {
        expect(pickImages(images, [2, 3])).toEqual([
            { imageId: 3 },
            { imageId: 2 },
        ]);
    });

    it("중복 id와 없는 id는 영향을 주지 않는다", () => {
        expect(pickImages(images, [1, 1, 99, undefined])).toEqual([
            { imageId: 1 },
        ]);
    });
});

describe("sameIds", () => {
    it("순서가 달라도 같은 id들이면 같다", () => {
        expect(sameIds([1, 2, 3], [3, 1, 2])).toBe(true);
        expect(sameIds([], [])).toBe(true);
    });

    it("중복이 있어도 같은 id들이면 같다", () => {
        expect(sameIds([1, 1, 2], [2, 1])).toBe(true);
    });

    it("한쪽에만 있는 id가 있으면 다르다", () => {
        expect(sameIds([1, 2], [1, 3])).toBe(false);
        expect(sameIds([1], [1, 2])).toBe(false);
        expect(sameIds([1, 2], [])).toBe(false);
    });
});
