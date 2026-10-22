import { describe, expect, it } from "vitest";

import { visibleVworldLayers } from "@/features/image/baseMap";

describe("visibleVworldLayers", () => {
    it("일반 지도는 테마에 따라 Base 또는 midnight", () => {
        expect(visibleVworldLayers("map", false)).toEqual(["Base"]);
        expect(visibleVworldLayers("map", true)).toEqual(["midnight"]);
    });

    it("위성 영상은 테마와 관계없이 Satellite", () => {
        expect(visibleVworldLayers("satellite", false)).toEqual(["Satellite"]);
        expect(visibleVworldLayers("satellite", true)).toEqual(["Satellite"]);
    });

    it("위성 + 지명은 Satellite 위에 Hybrid", () => {
        expect(visibleVworldLayers("hybrid", true)).toEqual([
            "Satellite",
            "Hybrid",
        ]);
    });
});
