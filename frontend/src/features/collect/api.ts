import { z } from "zod";

import {
    aoiPresetSchema,
    collectRunPageSchema,
    collectRunSchema,
    collectRunSkipSchema,
    collectTargetSchema,
} from "@/features/collect/schemas";
import type { CollectTargetRequest } from "@/features/collect/types";
import { apiGet, apiPost, apiSend } from "@/shared/api/client";

const TARGETS = "/api/collect-targets";

export function fetchCollectTargets(signal?: AbortSignal) {
    return apiGet(TARGETS, z.array(collectTargetSchema), {}, signal);
}

export function fetchDefaultAoi(signal?: AbortSignal) {
    return apiGet(`${TARGETS}/default-aoi`, aoiPresetSchema, {}, signal);
}

export function createCollectTarget(request: CollectTargetRequest) {
    return apiSend("POST", TARGETS, request);
}

export function updateCollectTarget(
    targetId: number,
    request: CollectTargetRequest,
) {
    return apiSend("PUT", `${TARGETS}/${targetId}`, request);
}

export function deleteCollectTarget(targetId: number) {
    return apiSend("DELETE", `${TARGETS}/${targetId}`);
}

// 단건 실행을 대기열에 넣는다. 진행은 실행 이력에서 본다
export function runCollectTarget(targetId: number) {
    return apiPost(`${TARGETS}/${targetId}/runs`, collectRunSchema);
}

const RUNS = "/api/collect-runs";
const RUN_PAGE_SIZE = 50;

// 최근 등록이 위로. 다음 페이지는 응답의 nextCursor로 받는다
export function fetchCollectRuns(cursor?: string, signal?: AbortSignal) {
    return apiGet(
        RUNS,
        collectRunPageSchema,
        { cursor, size: RUN_PAGE_SIZE },
        signal,
    );
}

export function fetchCollectRunSkips(runId: number, signal?: AbortSignal) {
    return apiGet(
        `${RUNS}/${runId}/skips`,
        z.array(collectRunSkipSchema),
        {},
        signal,
    );
}

export function cancelCollectRun(runId: number) {
    return apiSend("POST", `${RUNS}/${runId}/cancel`);
}
