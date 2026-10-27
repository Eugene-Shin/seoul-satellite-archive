import { describe, expect, it } from "vitest";

import { sensorTypesChange } from "@/features/image/filters/sensorTypesChange";

describe("sensorTypesChange", () => {
    it("SAR만 고르면 구름량 값과 구름량 정렬을 비운다", () => {
        const conditions = {
            cloudMax: 30,
            sorts: ["CLOUD_COVER_ASC" as const, "ACQUIRED_AT_DESC" as const],
        };

        expect(sensorTypesChange(conditions, ["SAR"])).toEqual({
            sensorTypes: ["SAR"],
            cloudMax: undefined,
            sorts: ["ACQUIRED_AT_DESC"],
        });
    });

    it("SAR만이 아니면 유형만 바꾼다", () => {
        const conditions = {
            cloudMax: 30,
            sorts: ["CLOUD_COVER_ASC" as const],
        };

        expect(sensorTypesChange(conditions, ["SAR", "OPTICAL"])).toEqual({
            sensorTypes: ["SAR", "OPTICAL"],
        });
    });
});
