import type {
    ImageDetailResponse,
    ImageWriteRequest,
    OrbitDirection,
    SensorType,
} from "@/features/image/types";
import { toKstDateTimeLocal } from "@/shared/utils/date";

export type ImageFormValues = {
    collectionId: number | null;
    sourceId: string;
    title: string;
    satellite: string;
    instruments: string[];
    sensorType: SensorType;
    acquiredAt: string; // datetime-local, KST
    acquiredEndAt: string; // datetime-local, KST
    gsdMeters: string;
    cloudCover: string;
    orbitDirection: OrbitDirection | null;
    footprint: string; // GeoJSON
    url: string;
    downloadUrls: string; // 한 줄에 하나
    sourceUpdatedAt: string; // datetime-local, KST
    rawMetadata: string; // JSON 객체
};

export const EMPTY_IMAGE_FORM: ImageFormValues = {
    collectionId: null,
    sourceId: "",
    title: "",
    satellite: "",
    instruments: [],
    sensorType: "OPTICAL",
    acquiredAt: "",
    acquiredEndAt: "",
    gsdMeters: "",
    cloudCover: "",
    orbitDirection: null,
    footprint: "",
    url: "",
    downloadUrls: "",
    sourceUpdatedAt: "",
    rawMetadata: "",
};

export const REQUIRED_FIELDS = [
    "collectionId",
    "sourceId",
    "title",
    "satellite",
    "sensorType",
    "acquiredAt",
    "url",
    "downloadUrls",
    "footprint",
] as const satisfies (keyof ImageFormValues)[];

export type RequiredField = (typeof REQUIRED_FIELDS)[number];

export function missingRequiredFields(form: ImageFormValues): RequiredField[] {
    return REQUIRED_FIELDS.filter((field) => {
        const value = form[field];
        return (
            value === null || (typeof value === "string" && value.trim() === "")
        );
    });
}

export function isSameForm(a: ImageFormValues, b: ImageFormValues): boolean {
    return (Object.keys(a) as (keyof ImageFormValues)[]).every((key) => {
        const left = a[key];
        const right = b[key];
        return Array.isArray(left) && Array.isArray(right)
            ? left.length === right.length &&
                  left.every((value, i) => value === right[i])
            : left === right;
    });
}

export function toFormValues(detail: ImageDetailResponse): ImageFormValues {
    return {
        collectionId: detail.collection.collectionId,
        sourceId: detail.sourceId,
        title: detail.title,
        satellite: detail.satellite,
        instruments: detail.instruments ?? [],
        sensorType: detail.sensorType,
        acquiredAt: toKstDateTimeLocal(detail.acquiredAt),
        acquiredEndAt: detail.acquiredEndAt
            ? toKstDateTimeLocal(detail.acquiredEndAt)
            : "",
        gsdMeters: detail.gsdMeters?.toString() ?? "",
        cloudCover: detail.cloudCover?.toString() ?? "",
        orbitDirection: detail.orbitDirection,
        footprint: JSON.stringify(detail.footprint),
        url: detail.url,
        downloadUrls: detail.downloadUrls.join("\n"),
        sourceUpdatedAt: detail.sourceUpdatedAt
            ? toKstDateTimeLocal(detail.sourceUpdatedAt)
            : "",
        rawMetadata:
            Object.keys(detail.rawMetadata).length > 0
                ? JSON.stringify(detail.rawMetadata, null, 2)
                : "",
    };
}

// 내용은 서버가 검증하고, 여기선 요청 형식으로 바꾸기만 한다 (JSON 문법 오류만 미리 알림)
export function toWriteRequest(form: ImageFormValues): ImageWriteRequest {
    return {
        collectionId: form.collectionId,
        sourceId: form.sourceId,
        title: form.title,
        satellite: form.satellite.trim(),
        instruments: form.instruments.length > 0 ? form.instruments : null,
        sensorType: form.sensorType,
        acquiredAt: toKstOffsetDateTime(form.acquiredAt),
        acquiredEndAt: form.acquiredEndAt
            ? toKstOffsetDateTime(form.acquiredEndAt)
            : null,
        gsdMeters: toNumber(form.gsdMeters),
        cloudCover: toNumber(form.cloudCover),
        orbitDirection: form.orbitDirection,
        footprint: parseJson(form.footprint, "footprint"),
        url: form.url,
        downloadUrls: splitLines(form.downloadUrls),
        sourceUpdatedAt: form.sourceUpdatedAt
            ? toKstOffsetDateTime(form.sourceUpdatedAt)
            : null,
        rawMetadata: form.rawMetadata
            ? parseJson(form.rawMetadata, "rawMetadata")
            : null,
    };
}

function splitLines(text: string): string[] {
    return text
        .split("\n")
        .map((line) => line.trim())
        .filter((line) => line !== "");
}

function toNumber(text: string): number | null {
    return text === "" ? null : Number(text);
}

// 입력칸은 초·밀리초가 0이면 "2026-10-19T10:00"처럼 생략해서 준다
function toKstOffsetDateTime(localDateTime: string): string {
    const withSeconds =
        localDateTime.length === "YYYY-MM-DDTHH:mm".length
            ? `${localDateTime}:00`
            : localDateTime;
    return `${withSeconds}+09:00`;
}

function parseJson(text: string, field: string): unknown {
    try {
        return JSON.parse(text);
    } catch {
        throw new Error(`${field}: JSON 형식이 아닙니다.`);
    }
}
