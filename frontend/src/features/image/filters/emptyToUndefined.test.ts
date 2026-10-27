import { describe, expect, it } from "vitest";

import { emptyToUndefined } from "@/features/image/filters/emptyToUndefined";

describe("emptyToUndefined", () => {
    it("빈 배열은 undefined", () => {
        expect(emptyToUndefined([])).toBeUndefined();
    });

    it("값이 있으면 그대로", () => {
        expect(emptyToUndefined(["A", "B"])).toEqual(["A", "B"]);
    });
});
