import { describe, expect, it } from "vitest";

import { formatArea, formatLength } from "@/features/image/measure";

describe("formatLength", () => {
    it("1 km 미만은 m 정수로", () => {
        expect(formatLength(849.6)).toBe("850 m");
    });

    it("1 km부터는 km 소수 둘째 자리로", () => {
        expect(formatLength(1000)).toBe("1.00 km");
        expect(formatLength(12345)).toBe("12.35 km");
    });
});

describe("formatArea", () => {
    it("1 km² 미만은 m² 정수에 천 단위 쉼표", () => {
        expect(formatArea(3400.4)).toBe("3,400 m²");
    });

    it("1 km²부터는 km² 소수 둘째 자리로", () => {
        expect(formatArea(1_000_000)).toBe("1.00 km²");
        expect(formatArea(2_345_678)).toBe("2.35 km²");
    });
});
