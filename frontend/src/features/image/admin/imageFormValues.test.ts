import { describe, expect, it } from "vitest";

import {
    EMPTY_IMAGE_FORM,
    isSameForm,
    missingRequiredFields,
    toFormValues,
    toWriteRequest,
} from "@/features/image/admin/imageFormValues";
import type { ImageDetailResponse } from "@/features/image/types";

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

    it("입력칸이 초를 생략하면 0초로, 밀리초가 있으면 그대로 보낸다", () => {
        expect(
            toWriteRequest({ ...FILLED_FORM, acquiredAt: "2026-10-19T10:00" })
                .acquiredAt,
        ).toBe("2026-10-19T10:00:00+09:00");
        expect(
            toWriteRequest({
                ...FILLED_FORM,
                acquiredAt: "2026-10-19T10:00:12.560",
            }).acquiredAt,
        ).toBe("2026-10-19T10:00:12.560+09:00");
    });

    it("JSON 문법이 틀리면 칸 이름과 함께 알린다", () => {
        expect(() =>
            toWriteRequest({ ...FILLED_FORM, footprint: "{type:" }),
        ).toThrow("footprint: JSON 형식이 아닙니다.");
    });
});

describe("toFormValues", () => {
    const DETAIL = {
        imageId: 1,
        providerCode: "dx",
        sourceId: "admin-image-001",
        title: "서울 위성 영상",
        satellite: "SENTINEL-2A",
        instruments: ["msi"],
        sensorType: "OPTICAL" as const,
        acquiredAt: "2024-09-03T20:27:12.560Z",
        gsdMeters: 10.5,
        cloudCover: null,
        orbitDirection: null,
        footprint: FOOTPRINT as ImageDetailResponse["footprint"],
        url: "archive/metadata.json",
        downloadUrls: ["s3://archive/a.tif", "s3://archive/b.tif"],
        sourceUpdatedAt: null,
        rawMetadata: { cloud: 5 },
    };

    it("상세 응답을 KST 입력칸 값과 글자로 바꾼다", () => {
        expect(toFormValues(DETAIL)).toMatchObject({
            acquiredAt: "2024-09-04T05:27:12.560",
            gsdMeters: "10.5",
            cloudCover: "",
            downloadUrls: "s3://archive/a.tif\ns3://archive/b.tif",
            sourceUpdatedAt: "",
        });
    });

    it("불러와서 그대로 저장하면 같은 값이 된다", () => {
        const request = toWriteRequest(toFormValues(DETAIL));

        expect(request).toMatchObject({
            providerCode: DETAIL.providerCode,
            satellite: DETAIL.satellite,
            instruments: DETAIL.instruments,
            gsdMeters: DETAIL.gsdMeters,
            cloudCover: null,
            footprint: DETAIL.footprint,
            downloadUrls: DETAIL.downloadUrls,
            rawMetadata: DETAIL.rawMetadata,
        });
        expect(Date.parse(request.acquiredAt)).toBe(
            Date.parse(DETAIL.acquiredAt),
        );
    });

    it("비어 있는 원본 메타데이터는 빈 칸", () => {
        expect(toFormValues({ ...DETAIL, rawMetadata: {} }).rawMetadata).toBe(
            "",
        );
    });
});

describe("isSameForm", () => {
    it("모든 칸이 같으면 같다", () => {
        expect(isSameForm(FILLED_FORM, { ...FILLED_FORM })).toBe(true);
    });

    it("한 칸만 달라도 다르다", () => {
        expect(isSameForm(FILLED_FORM, { ...FILLED_FORM, title: "x" })).toBe(
            false,
        );
    });

    it("센서의 개수나 순서가 다르면 다르다", () => {
        expect(
            isSameForm(FILLED_FORM, { ...FILLED_FORM, instruments: ["msi"] }),
        ).toBe(false);
        expect(
            isSameForm(FILLED_FORM, {
                ...FILLED_FORM,
                instruments: ["OLI", "msi"],
            }),
        ).toBe(false);
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
