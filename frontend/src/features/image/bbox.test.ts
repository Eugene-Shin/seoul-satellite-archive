import { fromLonLat } from "ol/proj";
import { describe, expect, it } from "vitest";

import { bboxToExtent, extentToBbox, toBbox } from "@/features/image/bbox";

describe("toBbox", () => {
    it("올바른 값은 소수점 6자리로 반올림해 돌려준다", () => {
        expect(toBbox([126.123456789, 37.5, 127.987654321, 37.7])).toEqual([
            126.123457, 37.5, 127.987654, 37.7,
        ]);
    });

    it("한 점(최소 = 최대)도 허용한다", () => {
        expect(toBbox([127, 37.5, 127, 37.5])).toEqual([127, 37.5, 127, 37.5]);
    });

    it.each([
        ["값 3개", [126.9, 37.4, 127.2]],
        ["값 5개", [126.9, 37.4, 127.2, 37.7, 1]],
        ["NaN", [Number.NaN, 37.4, 127.2, 37.7]],
        ["Infinity", [Number.POSITIVE_INFINITY, 37.4, 127.2, 37.7]],
        ["경도 범위 밖", [-181, 37.4, 127.2, 37.7]],
        ["위도 범위 밖", [126.9, -91, 127.2, 37.7]],
        ["경도 순서 뒤바뀜", [127.2, 37.4, 126.9, 37.7]],
        ["위도 순서 뒤바뀜", [126.9, 37.7, 127.2, 37.4]],
    ])("%s이면 null", (_, values) => {
        expect(toBbox(values)).toBeNull();
    });
});

describe("extentToBbox / bboxToExtent", () => {
    it("지도 범위를 경위도 bbox로 바꾼다", () => {
        const extent = [
            ...fromLonLat([126.9, 37.4]),
            ...fromLonLat([127.2, 37.7]),
        ];

        expect(extentToBbox(extent)).toEqual([126.9, 37.4, 127.2, 37.7]);
    });

    it("bbox → 지도 범위 → bbox가 원래 값과 같다", () => {
        const bbox = toBbox([126.9, 37.4, 127.2, 37.7]);

        expect(bbox && extentToBbox(bboxToExtent(bbox))).toEqual(bbox);
    });
});
