import { describe, expect, it } from "vitest";

import {
    changeMode,
    describeCron,
    fromCron,
    toCron,
} from "@/features/collect/schedule";

describe("toCron", () => {
    it("고른 반복 방식을 Spring cron으로 만든다", () => {
        expect(toCron({ mode: "daily", time: "03:30" })).toBe("0 30 3 * * *");
        expect(
            toCron({ mode: "weekly", time: "03:00", weekdays: ["THU", "MON"] }),
        ).toBe("0 0 3 * * MON,THU");
        expect(toCron({ mode: "hourly", intervalHours: 6 })).toBe(
            "0 0 */6 * * *",
        );
        expect(toCron({ mode: "custom", cron: " 0 15 4 1 * * " })).toBe(
            "0 15 4 1 * *",
        );
    });
});

describe("fromCron", () => {
    it("고를 수 있는 모양이면 반복 방식으로 푼다", () => {
        expect(fromCron("0 30 3 * * *")).toEqual({
            mode: "daily",
            time: "03:30",
        });
        expect(fromCron("0 0 3 * * MON,THU")).toEqual({
            mode: "weekly",
            time: "03:00",
            weekdays: ["MON", "THU"],
        });
        expect(fromCron("0 0 */6 * * *")).toEqual({
            mode: "hourly",
            intervalHours: 6,
        });
    });

    it("그 밖의 cron은 직접 입력으로 연다", () => {
        for (const cron of [
            "0 15 */6 * * *",
            "0 0 */5 * * *",
            "0 0 3 1 * *",
            "0 0 3 * * 1-5",
            "0 75 3 * * *",
            "@daily",
        ]) {
            expect(fromCron(cron)).toEqual({ mode: "custom", cron });
        }
    });

    it("만든 cron을 다시 풀면 같은 일정이 된다", () => {
        const weekly = {
            mode: "weekly",
            time: "21:05",
            weekdays: ["SAT", "SUN"],
        } as const;
        expect(
            fromCron(toCron({ ...weekly, weekdays: ["SAT", "SUN"] })),
        ).toEqual({ ...weekly, weekdays: ["SAT", "SUN"] });
    });
});

describe("changeMode", () => {
    it("시각은 이어받고 그 방식의 기본값으로 채운다", () => {
        expect(changeMode({ mode: "daily", time: "05:00" }, "weekly")).toEqual({
            mode: "weekly",
            time: "05:00",
            weekdays: ["MON"],
        });
        expect(
            changeMode({ mode: "hourly", intervalHours: 2 }, "daily"),
        ).toEqual({ mode: "daily", time: "03:00" });
    });

    it("직접 입력은 지금 일정의 cron으로 시작한다", () => {
        expect(changeMode({ mode: "daily", time: "05:00" }, "custom")).toEqual({
            mode: "custom",
            cron: "0 0 5 * * *",
        });
    });
});

describe("describeCron", () => {
    it("날짜 조건이 없고 시가 하나면 '매일'을 붙이고 KST를 표시한다", () => {
        expect(describeCron("0 0 3 * * *")).toBe("매일 03:00 (KST)");
    });

    it("요일·간격이 있으면 번역문에 맡긴다", () => {
        expect(describeCron("0 0 3 * * MON,THU")).toBe(
            "03:00, 월요일, 목요일에만 (KST)",
        );
        expect(describeCron("0 0 */6 * * *")).toBe("정각, 6시간마다 (KST)");
    });

    it("초가 없는 cron이나 잘못된 식은 설명하지 않는다", () => {
        expect(describeCron("0 3 * * *")).toBeNull();
        expect(describeCron("0 0 3 * * XYZ")).toBeNull();
    });
});
