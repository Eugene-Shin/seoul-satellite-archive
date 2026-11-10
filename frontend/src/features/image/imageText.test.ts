import { describe, expect, it } from "vitest";

import {
    cloudCoverText,
    collectionOptionLabel,
    creditText,
    imageFileDetailText,
    orbitDirectionText,
    providerText,
    resolutionText,
    satelliteText,
    sensorTypeText,
} from "@/features/image/imageText";

describe("creditText", () => {
    const collection = {
        collectionId: 1,
        providerCode: "earth-search",
        code: "sentinel-2-l2a",
        title: "Sentinel-2 Level-2A",
        productType: "L2A",
        licenseName: null,
        licenseUrl: null,
    };

    it("표기 문구에 UTC 기준 촬영 연도를 붙인다", () => {
        expect(
            creditText({
                collection: { ...collection, creditText: "Contains data" },
                acquiredAt: "2024-12-31T23:30:00Z",
            }),
        ).toBe("Contains data 2024");
    });

    it("표기 문구가 없으면 null", () => {
        expect(
            creditText({
                collection: { ...collection, creditText: null },
                acquiredAt: "2024-12-31T23:30:00Z",
            }),
        ).toBeNull();
    });
});

describe("imageFileDetailText", () => {
    const band = {
        bandIndex: 1,
        bandName: null,
        commonName: null,
        centerWavelengthUm: null,
        bandwidthUm: null,
        resolutionMeters: null,
    };

    it("픽셀 간격과 밴드 이름을 함께 보여준다", () => {
        expect(
            imageFileDetailText({
                pixelSpacingMeters: 10,
                bands: [
                    { ...band, bandName: "red" },
                    { ...band, bandIndex: 2, bandName: "green" },
                ],
            }),
        ).toBe("10 m · red, green");
    });

    it("밴드 이름이 없으면 표준 이름, 그것도 없으면 번호", () => {
        expect(
            imageFileDetailText({
                pixelSpacingMeters: null,
                bands: [
                    { ...band, commonName: "nir" },
                    { ...band, bandIndex: 2 },
                ],
            }),
        ).toBe("nir, 밴드 2");
    });

    it("값이 없으면 빈 문자열", () => {
        expect(
            imageFileDetailText({ pixelSpacingMeters: null, bands: [] }),
        ).toBe("");
    });
});

describe("satelliteText", () => {
    it("센서가 있으면 괄호 안에 쉼표로 붙인다", () => {
        expect(
            satelliteText({
                satellite: "KOMPSAT-3",
                instruments: ["MS", "PAN"],
            }),
        ).toBe("KOMPSAT-3 (MS, PAN)");
    });

    it("센서가 없으면 위성 이름만", () => {
        expect(
            satelliteText({ satellite: "KOMPSAT-3", instruments: null }),
        ).toBe("KOMPSAT-3");
    });
});

describe("sensorTypeText", () => {
    it("코드를 한글 이름으로", () => {
        expect(sensorTypeText({ sensorType: "OPTICAL" })).toBe("광학");
    });
});

describe("resolutionText", () => {
    it("범위면 최솟값 ~ 최댓값", () => {
        expect(
            resolutionText({ resolution: { minMeters: 10, maxMeters: 60 } }),
        ).toBe("10 ~ 60 m");
    });

    it("값이 하나면 그 값만", () => {
        expect(
            resolutionText({ resolution: { minMeters: 30, maxMeters: 30 } }),
        ).toBe("30 m");
    });

    it("null이면 -", () => {
        expect(resolutionText({ resolution: null })).toBe("-");
    });
});

describe("cloudCoverText", () => {
    it("0도 값으로 표시한다", () => {
        expect(cloudCoverText({ cloudCover: 0 })).toBe("0%");
    });

    it("null이면 -", () => {
        expect(cloudCoverText({ cloudCover: null })).toBe("-");
    });
});

describe("orbitDirectionText", () => {
    it("코드를 한글 이름으로", () => {
        expect(orbitDirectionText({ orbitDirection: "ASCENDING" })).toBe(
            "상향",
        );
    });

    it("null이면 -", () => {
        expect(orbitDirectionText({ orbitDirection: null })).toBe("-");
    });
});

describe("providerText", () => {
    it("유료 여부를 붙인다", () => {
        expect(
            providerText({
                provider: {
                    code: "KARI",
                    name: "항우연",
                    isPaid: true,
                },
            }),
        ).toBe("항우연 (유료)");
        expect(
            providerText({
                provider: {
                    code: "ESA",
                    name: "ESA",
                    isPaid: false,
                },
            }),
        ).toBe("ESA (무료)");
    });
});

describe("collectionOptionLabel", () => {
    const COLLECTION = {
        collectionId: 1,
        providerCode: "earth-search",
        code: "sentinel-2-l2a",
        title: "Sentinel-2 Level-2A",
        productType: "L2A",
        licenseName: null,
        licenseUrl: null,
        creditText: null,
    };

    it("컬렉션을 정의한 출처 이름을 앞에 붙인다", () => {
        expect(
            collectionOptionLabel(COLLECTION, [
                {
                    code: "earth-search",
                    name: "Earth Search",
                    isPaid: false,
                },
            ]),
        ).toBe("Earth Search · Sentinel-2 Level-2A");
    });

    it("출처 이름을 모르면 코드를 쓴다", () => {
        expect(collectionOptionLabel(COLLECTION, [])).toBe(
            "earth-search · Sentinel-2 Level-2A",
        );
    });
});
