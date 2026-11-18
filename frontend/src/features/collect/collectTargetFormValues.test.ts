import { describe, expect, it } from "vitest";

import {
    emptyFormValues,
    missingFields,
    parseAoi,
    requestFromTarget,
    toFormValues,
    toRequest,
} from "@/features/collect/collectTargetFormValues";
import type { CollectTarget } from "@/features/collect/types";

const AOI = {
    type: "MultiPolygon" as const,
    coordinates: [
        [
            [
                [126.9, 37.5],
                [127.1, 37.5],
                [127.1, 37.6],
                [126.9, 37.5],
            ],
        ],
    ],
};

const TARGET: CollectTarget = {
    targetId: 1,
    collection: {
        collectionId: 4,
        providerCode: "earth-search",
        code: "sentinel-2-c1-l2a",
        title: "Sentinel-2 Collection 1 Level-2A",
        productType: "L2A",
        licenseName: null,
        licenseUrl: null,
        creditText: null,
        collectable: true,
    },
    aoi: AOI,
    scheduleCron: "0 0 3 * * *",
    nextRunAt: "2026-11-18T18:00:00Z",
    lookbackDays: 7,
    enabled: true,
    createdAt: "2026-11-17T00:00:00Z",
    updatedAt: "2026-11-17T00:00:00Z",
};

describe("parseAoi", () => {
    it("GeoJSON MultiPolygon만 도형으로 읽는다", () => {
        expect(parseAoi(JSON.stringify(AOI))).toEqual(AOI);
        expect(parseAoi("{")).toBeNull();
        expect(
            parseAoi(JSON.stringify({ ...AOI, type: "Polygon" })),
        ).toBeNull();
    });
});

describe("missingFields", () => {
    it("비었거나 읽을 수 없는 칸을 알려 준다", () => {
        expect(
            missingFields({
                collectionId: null,
                schedule: { mode: "weekly", time: "03:00", weekdays: [] },
                lookbackDays: " ",
                enabled: true,
                aoi: "not json",
            }),
        ).toEqual(["collectionId", "schedule", "lookbackDays", "aoi"]);
        expect(missingFields(emptyFormValues(AOI, 4))).toEqual([]);
    });
});

describe("toFormValues / toRequest", () => {
    it("수정 폼으로 풀었다가 다시 요청으로 만들면 같은 값이 된다", () => {
        expect(toRequest(toFormValues(TARGET))).toEqual({
            collectionId: 4,
            aoi: AOI,
            scheduleCron: "0 0 3 * * *",
            lookbackDays: 7,
            enabled: true,
        });
    });
});

describe("requestFromTarget", () => {
    it("바꾼 값만 덮고 나머지는 대상 값을 그대로 보낸다", () => {
        expect(requestFromTarget(TARGET, { enabled: false })).toEqual({
            collectionId: 4,
            aoi: AOI,
            scheduleCron: "0 0 3 * * *",
            lookbackDays: 7,
            enabled: false,
        });
    });
});
