import type {
    ImageWriteRequest,
    OrbitDirection,
    SensorType,
} from "@/features/image/types";

export type ImageFormValues = {
    providerCode: string | null;
    sourceId: string;
    title: string;
    satellite: string;
    instruments: string[];
    sensorType: SensorType;
    acquiredAt: string; // datetime-local, KST
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
    providerCode: null,
    sourceId: "",
    title: "",
    satellite: "",
    instruments: [],
    sensorType: "OPTICAL",
    acquiredAt: "",
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
    "providerCode",
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
        return value === null || value.trim() === "";
    });
}

// 내용은 서버가 검증하고, 여기선 요청 형식으로 바꾸기만 한다 (JSON 문법 오류만 미리 알림)
export function toWriteRequest(form: ImageFormValues): ImageWriteRequest {
    return {
        providerCode: form.providerCode ?? "",
        sourceId: form.sourceId,
        title: form.title,
        satellite: form.satellite.trim(),
        instruments: form.instruments.length > 0 ? form.instruments : null,
        sensorType: form.sensorType,
        acquiredAt: toKstOffsetDateTime(form.acquiredAt),
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

function toKstOffsetDateTime(localDateTime: string): string {
    return `${localDateTime}:00+09:00`;
}

function parseJson(text: string, field: string): unknown {
    try {
        return JSON.parse(text);
    } catch {
        throw new Error(`${field}: JSON 형식이 아닙니다.`);
    }
}
