import { describe, expect, it } from "vitest";

import {
    cloudCoverText,
    gsdText,
    orbitDirectionText,
    providerText,
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

describe("gsdText", () => {
    it("값이 있으면 m 단위", () => {
        expect(gsdText({ gsdMeters: 0.5 })).toBe("0.5 m");
    });

    it("null이면 -", () => {
        expect(gsdText({ gsdMeters: null })).toBe("-");
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
                provider: { code: "KARI", name: "항우연", isPaid: true },
            }),
        ).toBe("항우연 (유료)");
        expect(
            providerText({
                provider: { code: "ESA", name: "ESA", isPaid: false },
            }),
        ).toBe("ESA (무료)");
    });
});
