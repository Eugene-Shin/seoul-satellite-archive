import { z } from "zod";

import {
    aoiPresetSchema,
    collectTargetSchema,
} from "@/features/collect/schemas";
import type { CollectTargetRequest } from "@/features/collect/types";
import { apiGet, apiSend } from "@/shared/api/client";

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

// 단발 실행을 대기열에 넣는다. 진행은 실행 이력 화면에서 본다
export function runCollectTarget(targetId: number) {
    return apiSend("POST", `${TARGETS}/${targetId}/runs`);
}
