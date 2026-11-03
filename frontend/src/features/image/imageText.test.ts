import { describe, expect, it } from "vitest";

import {
    cloudCoverText,
    collectionOptionLabel,
    orbitDirectionText,
    providerText,
    resolutionText,
    satelliteText,
    sensorTypeText,
} from "@/features/image/imageText";

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
                    ownOrganization: false,
                },
            }),
        ).toBe("항우연 (유료)");
        expect(
            providerText({
                provider: {
                    code: "ESA",
                    name: "ESA",
                    isPaid: false,
                    ownOrganization: false,
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
    };

    it("컬렉션을 정의한 출처 이름을 앞에 붙인다", () => {
        expect(
            collectionOptionLabel(COLLECTION, [
                {
                    code: "earth-search",
                    name: "Earth Search",
                    isPaid: false,
                    ownOrganization: false,
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
