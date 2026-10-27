import { describe, expect, it } from "vitest";

import {
    EMPTY_IMAGE_FORM,
    missingRequiredFields,
    toWriteRequest,
} from "@/features/image/admin/imageFormValues";

const FOOTPRINT = {
    type: "MultiPolygon",
    coordinates: [
        [
            [
                [126.9, 37.5],
                [127.0, 37.5],
                [127.0, 37.6],
                [126.9, 37.5],
            ],
        ],
    ],
};

const FILLED_FORM = {
    ...EMPTY_IMAGE_FORM,
    providerCode: "dx",
    sourceId: "admin-image-001",
    title: "서울 위성 영상",
    satellite: " sentinel-2a ",
    instruments: ["msi", "OLI"],
    acquiredAt: "2026-10-19T10:00",
    gsdMeters: "10.5",
    cloudCover: "5.25",
    orbitDirection: "ASCENDING" as const,
    footprint: JSON.stringify(FOOTPRINT),
    url: "archive/metadata.json",
    downloadUrls: "s3://archive/a.tif\n\n s3://archive/b.tif ",
    sourceUpdatedAt: "2026-10-20T09:30",
    rawMetadata: '{"cloud": 5}',
};

describe("toWriteRequest", () => {
    it("입력칸 글자를 요청 형식으로 바꾼다", () => {
        expect(toWriteRequest(FILLED_FORM)).toEqual({
            providerCode: "dx",
            sourceId: "admin-image-001",
            title: "서울 위성 영상",
            satellite: "sentinel-2a",
            instruments: ["msi", "OLI"],
            sensorType: "OPTICAL",
            acquiredAt: "2026-10-19T10:00:00+09:00",
            gsdMeters: 10.5,
            cloudCover: 5.25,
            orbitDirection: "ASCENDING",
            footprint: FOOTPRINT,
            url: "archive/metadata.json",
            downloadUrls: ["s3://archive/a.tif", "s3://archive/b.tif"],
            sourceUpdatedAt: "2026-10-20T09:30:00+09:00",
            rawMetadata: { cloud: 5 },
        });
    });

    it("비운 선택 칸은 null", () => {
        const request = toWriteRequest({
            ...FILLED_FORM,
            instruments: [],
            gsdMeters: "",
            cloudCover: "",
            orbitDirection: null,
            sourceUpdatedAt: "",
            rawMetadata: "",
        });

        expect(request).toMatchObject({
            instruments: null,
            gsdMeters: null,
            cloudCover: null,
            orbitDirection: null,
            sourceUpdatedAt: null,
            rawMetadata: null,
        });
    });

    it("JSON 문법이 틀리면 칸 이름과 함께 알린다", () => {
        expect(() =>
            toWriteRequest({ ...FILLED_FORM, footprint: "{type:" }),
        ).toThrow("footprint: JSON 형식이 아닙니다.");
    });
});

describe("missingRequiredFields", () => {
    it("비었거나 공백뿐인 필수 칸을 순서대로 알린다", () => {
        expect(
            missingRequiredFields({
                ...FILLED_FORM,
                providerCode: null,
                title: "  ",
                footprint: "",
            }),
        ).toEqual(["providerCode", "title", "footprint"]);
    });

    it("다 채우면 빈 목록", () => {
        expect(missingRequiredFields(FILLED_FORM)).toEqual([]);
    });
});
