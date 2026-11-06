import { get as getProjection, transform } from "ol/proj";
import { beforeAll, describe, expect, it } from "vitest";

import { registerUtmProjections } from "@/features/image/map/projections";

describe("registerUtmProjections", () => {
    beforeAll(registerUtmProjections);

    it("남북반구 모든 구역을 등록한다", () => {
        for (const code of [
            "EPSG:32601",
            "EPSG:32652",
            "EPSG:32660",
            "EPSG:32701",
            "EPSG:32760",
        ]) {
            expect(getProjection(code)).not.toBeNull();
        }
    });

    // 52번 구역의 중앙 경선은 129°이고, 그 위의 동향 좌표는 500000 m다
    it("북반구 좌표를 경위도로 바꾼다", () => {
        const [lon, lat] = transform([500000, 0], "EPSG:32652", "EPSG:4326");
        expect(lon).toBeCloseTo(129, 6);
        expect(lat).toBeCloseTo(0, 6);
    });

    it("남반구는 적도의 북향 좌표가 10000000 m다", () => {
        const [lon, lat] = transform(
            [500000, 10000000],
            "EPSG:32752",
            "EPSG:4326",
        );
        expect(lon).toBeCloseTo(129, 6);
        expect(lat).toBeCloseTo(0, 6);
    });
});
