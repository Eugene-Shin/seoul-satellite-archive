import { describe, expect, it } from "vitest";

import {
    finishedMessage,
    newlyFinished,
    runStatusText,
} from "@/features/collect/runText";
import type { CollectRun } from "@/features/collect/types";

function run(changes: Partial<CollectRun>): CollectRun {
    return {
        runId: 1,
        targetId: 1,
        collectionTitle: "Sentinel-2 Collection 1 Level-2A",
        runType: "ONE_OFF",
        status: "SUCCEEDED",
        windowFrom: "2026-11-10T00:00:00Z",
        windowTo: "2026-11-17T00:00:00Z",
        found: 3,
        inserted: 2,
        updated: 0,
        skipped: 1,
        failureReason: null,
        createdAt: "2026-11-17T00:00:00Z",
        startedAt: "2026-11-17T00:00:01Z",
        cancelRequestedAt: null,
        finishedAt: "2026-11-17T00:00:03Z",
        ...changes,
    };
}

describe("newlyFinished", () => {
    it("대기·실행 중이었다가 끝난 실행만 고른다", () => {
        const previous = new Map([
            [1, "RUNNING" as const],
            [2, "QUEUED" as const],
            [3, "SUCCEEDED" as const],
        ]);
        const runs = [
            run({ runId: 1, status: "SUCCEEDED" }),
            run({ runId: 2, status: "QUEUED" }),
            run({ runId: 3, status: "SUCCEEDED" }),
            run({ runId: 4, status: "FAILED" }),
        ];

        expect(newlyFinished(previous, runs).map((r) => r.runId)).toEqual([1]);
    });
});

describe("runStatusText", () => {
    it("실행 중 취소 요청은 상태 옆에 표시한다", () => {
        expect(
            runStatusText(
                run({
                    status: "RUNNING",
                    cancelRequestedAt: "2026-11-17T00:00:02Z",
                }),
            ),
        ).toBe("실행 중 (취소 요청됨)");
        expect(runStatusText(run({ status: "QUEUED" }))).toBe("대기");
    });
});

describe("finishedMessage", () => {
    it("결과에 따라 건수나 실패 사유를 알린다", () => {
        expect(finishedMessage(run({}))).toEqual({
            title: "Sentinel-2 Collection 1 Level-2A 수집이 끝났습니다",
            description: "2건 추가, 0건 갱신, 1건 건너뜀",
        });
        expect(
            finishedMessage(
                run({ status: "FAILED", failureReason: "connection refused" }),
            ),
        ).toEqual({
            title: "Sentinel-2 Collection 1 Level-2A 수집에 실패했습니다",
            description: "2건 추가까지 반영 · connection refused",
        });
    });
});
