import {
    DEFAULT_SCHEDULE,
    fromCron,
    type Schedule,
    toCron,
} from "@/features/collect/schedule";
import type {
    CollectTarget,
    CollectTargetRequest,
} from "@/features/collect/types";
import { multiPolygonSchema } from "@/features/image/schemas";

export type CollectTargetFormValues = {
    collectionId: number | null;
    schedule: Schedule;
    lookbackDays: string;
    enabled: boolean;
    aoi: string; // GeoJSON 글자
};

export type CollectTargetField =
    "collectionId" | "schedule" | "lookbackDays" | "aoi";

const DEFAULT_LOOKBACK_DAYS = 7;

type MultiPolygon = CollectTargetRequest["aoi"];

export function emptyFormValues(
    defaultAoi: MultiPolygon,
    collectionId: number | null,
): CollectTargetFormValues {
    return {
        collectionId,
        schedule: DEFAULT_SCHEDULE,
        lookbackDays: String(DEFAULT_LOOKBACK_DAYS),
        enabled: true,
        aoi: JSON.stringify(defaultAoi),
    };
}

export function toFormValues(target: CollectTarget): CollectTargetFormValues {
    return {
        collectionId: target.collection.collectionId,
        schedule: fromCron(target.scheduleCron),
        lookbackDays: String(target.lookbackDays),
        enabled: target.enabled,
        aoi: JSON.stringify(target.aoi),
    };
}

// GeoJSON MultiPolygon이면 도형, 아니면 null. 미리보기와 제출 검사가 함께 쓴다
export function parseAoi(text: string): MultiPolygon | null {
    let json: unknown;
    try {
        json = JSON.parse(text);
    } catch {
        return null;
    }
    const result = multiPolygonSchema.safeParse(json);
    return result.success ? result.data : null;
}

// 형식(cron·범위·도형 유효성)은 서버가 검사한다. 여기서는 비었거나 읽을 수 없는 칸만 막는다
export function missingFields(
    form: CollectTargetFormValues,
): CollectTargetField[] {
    const missing: CollectTargetField[] = [];
    if (form.collectionId === null) {
        missing.push("collectionId");
    }
    if (toCron(form.schedule) === "" || isWeeklyWithoutDay(form.schedule)) {
        missing.push("schedule");
    }
    if (form.lookbackDays.trim() === "") {
        missing.push("lookbackDays");
    }
    if (parseAoi(form.aoi) === null) {
        missing.push("aoi");
    }
    return missing;
}

function isWeeklyWithoutDay(schedule: Schedule): boolean {
    return schedule.mode === "weekly" && schedule.weekdays.length === 0;
}

export function isSameForm(
    a: CollectTargetFormValues,
    b: CollectTargetFormValues,
): boolean {
    return (
        a.collectionId === b.collectionId &&
        toCron(a.schedule) === toCron(b.schedule) &&
        a.lookbackDays === b.lookbackDays &&
        a.enabled === b.enabled &&
        a.aoi === b.aoi
    );
}

export function toRequest(form: CollectTargetFormValues): CollectTargetRequest {
    const aoi = parseAoi(form.aoi);
    if (form.collectionId === null || aoi === null) {
        throw new Error("입력하지 않은 칸이 있습니다.");
    }
    return {
        collectionId: form.collectionId,
        aoi,
        scheduleCron: toCron(form.schedule),
        lookbackDays: Number(form.lookbackDays),
        enabled: form.enabled,
    };
}

// 표의 스위치처럼 다른 값은 그대로 두고 일부만 바꿔 보낼 때
export function requestFromTarget(
    target: CollectTarget,
    changes: Partial<CollectTargetRequest>,
): CollectTargetRequest {
    return {
        collectionId: target.collection.collectionId,
        aoi: target.aoi,
        scheduleCron: target.scheduleCron,
        lookbackDays: target.lookbackDays,
        enabled: target.enabled,
        ...changes,
    };
}
