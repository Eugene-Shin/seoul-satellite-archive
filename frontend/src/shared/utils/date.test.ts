import { describe, expect, it } from "vitest";

import { toKstDate, toKstDateTimeLocal } from "@/shared/utils/date";

describe("toKstDateTimeLocal", () => {
    it("UTC를 KST 입력칸 값으로, 밀리초까지 남긴다", () => {
        expect(toKstDateTimeLocal("2024-09-03T20:27:12.560Z")).toBe(
            "2024-09-04T05:27:12.560",
        );
    });
});

describe("toKstDate", () => {
    it("UTC 15:00은 KST로 다음 날이다", () => {
        expect(toKstDate("2024-01-01T15:00:00Z")).toBe("2024-01-02");
    });

    it("UTC 14:59는 KST로 같은 날이다", () => {
        expect(toKstDate("2024-01-01T14:59:59Z")).toBe("2024-01-01");
    });

    it("시간대가 붙은 값도 KST 날짜로 바꾼다", () => {
        expect(toKstDate("2024-01-02T08:00:00+09:00")).toBe("2024-01-02");
    });
});
