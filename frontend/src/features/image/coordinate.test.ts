import { describe, expect, it } from "vitest";

import { formatLatLon } from "@/features/image/coordinate";

describe("formatLatLon", () => {
    it("[경도, 위도]를 위도 먼저, 소수 5자리로", () => {
        expect(formatLatLon([126.978, 37.566123456])).toBe(
            "37.56612°N 126.97800°E",
        );
    });

    it("좌표가 없으면 빈 문자열", () => {
        expect(formatLatLon(undefined)).toBe("");
    });
});
