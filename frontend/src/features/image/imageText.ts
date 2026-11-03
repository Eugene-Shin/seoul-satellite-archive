import type {
    Collection,
    ImageResponse,
    LookDirection,
    OrbitDirection,
    Provider,
    SensorType,
} from "@/features/image/types";
import { formatKstDateTime } from "@/shared/utils/date";

export const SENSOR_TYPE_NAMES: Record<SensorType, string> = {
    OPTICAL: "광학",
    SAR: "SAR",
};

export const ORBIT_DIRECTION_NAMES: Record<OrbitDirection, string> = {
    ASCENDING: "상향",
    DESCENDING: "하향",
};

export const LOOK_DIRECTION_NAMES: Record<LookDirection, string> = {
    LEFT: "좌측",
    RIGHT: "우측",
};

export function satelliteText({
    satellite,
    instruments,
}: Pick<ImageResponse, "satellite" | "instruments">): string {
    return instruments ? `${satellite} (${instruments.join(", ")})` : satellite;
}

export function sensorTypeText({
    sensorType,
}: Pick<ImageResponse, "sensorType">): string {
    return SENSOR_TYPE_NAMES[sensorType];
}

export function acquiredAtText({
    acquiredAt,
}: Pick<ImageResponse, "acquiredAt">): string {
    return formatKstDateTime(acquiredAt);
}

export function acquiredEndAtText({
    acquiredEndAt,
}: Pick<ImageResponse, "acquiredEndAt">): string {
    return acquiredEndAt ? formatKstDateTime(acquiredEndAt) : "-";
}

export function resolutionText({
    resolution,
}: Pick<ImageResponse, "resolution">): string {
    if (resolution === null) {
        return "-";
    }
    const { minMeters, maxMeters } = resolution;
    return minMeters === maxMeters
        ? `${minMeters} m`
        : `${minMeters} ~ ${maxMeters} m`;
}

export function cloudCoverText({
    cloudCover,
}: Pick<ImageResponse, "cloudCover">): string {
    return cloudCover === null ? "-" : `${cloudCover}%`;
}

export function orbitDirectionText({
    orbitDirection,
}: Pick<ImageResponse, "orbitDirection">): string {
    return orbitDirection ? ORBIT_DIRECTION_NAMES[orbitDirection] : "-";
}

export function sourceUpdatedAtText({
    sourceUpdatedAt,
}: Pick<ImageResponse, "sourceUpdatedAt">): string {
    return sourceUpdatedAt ? formatKstDateTime(sourceUpdatedAt) : "-";
}

export function providerText({
    provider,
}: Pick<ImageResponse, "provider">): string {
    return `${provider.name} (${provider.isPaid ? "유료" : "무료"})`;
}

// 컬렉션은 정의한 출처가 영상의 제공 기관과 다를 수 있어서(올포랜드 영상) 출처를 함께 보여준다
export function collectionOptionLabel(
    collection: Collection,
    providers: Provider[],
): string {
    const providerName =
        providers.find((provider) => provider.code === collection.providerCode)
            ?.name ?? collection.providerCode;
    return `${providerName} · ${collection.title}`;
}
