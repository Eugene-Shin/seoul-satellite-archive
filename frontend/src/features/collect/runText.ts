import type {
    CollectRun,
    CollectRunStatus,
    CollectRunType,
} from "@/features/collect/types";
import { formatKstDateTime } from "@/shared/utils/date";

export const RUN_STATUS_NAMES: Record<CollectRunStatus, string> = {
    QUEUED: "대기",
    RUNNING: "실행 중",
    SUCCEEDED: "성공",
    FAILED: "실패",
    CANCELED: "취소",
};

export const RUN_TYPE_NAMES: Record<CollectRunType, string> = {
    SCHEDULED: "정기",
    ONE_OFF: "단건",
};

export function isActive(status: CollectRunStatus): boolean {
    return status === "QUEUED" || status === "RUNNING";
}

// 실행 중 취소는 워커가 확인할 때까지 상태가 RUNNING으로 남는다
export function runStatusText(run: CollectRun): string {
    if (run.status === "RUNNING" && run.cancelRequestedAt !== null) {
        return "실행 중 (취소 요청됨)";
    }
    return RUN_STATUS_NAMES[run.status];
}

export function runWindowText(run: CollectRun): string {
    if (run.windowFrom === null || run.windowTo === null) {
        return "-";
    }
    return `${formatKstDateTime(run.windowFrom)} ~ ${formatKstDateTime(run.windowTo)}`;
}

// 직전에 대기·실행 중이었다가 지금 끝난 실행. 처음 보는 실행은 직전 상태를 모르니 넣지 않는다.
// [지금 실행]으로 등록한 실행은 응답을 목록에 먼저 넣어 두므로 직전 상태가 있다
export function newlyFinished(
    previous: ReadonlyMap<number, CollectRunStatus>,
    runs: CollectRun[],
): CollectRun[] {
    return runs.filter((run) => {
        const before = previous.get(run.runId);
        return (
            before !== undefined && isActive(before) && !isActive(run.status)
        );
    });
}

// 토스트의 제목과 그 아래 줄에 보일 설명
export function finishedMessage(run: CollectRun): {
    title: string;
    description: string;
} {
    switch (run.status) {
        case "SUCCEEDED":
            return {
                title: `${run.collectionTitle} 수집이 끝났습니다`,
                description: `${run.inserted}건 추가, ${run.updated}건 갱신, ${run.skipped}건 건너뜀`,
            };
        case "FAILED":
            return {
                title: `${run.collectionTitle} 수집에 실패했습니다`,
                description: `${run.inserted}건 추가까지 반영 · ${run.failureReason ?? "사유 없음"}`,
            };
        default:
            return {
                title: `${run.collectionTitle} 수집을 취소했습니다`,
                description: `${run.inserted}건 추가까지 반영`,
            };
    }
}
