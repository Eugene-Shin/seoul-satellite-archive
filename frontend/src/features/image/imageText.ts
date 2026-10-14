import type {
    ImageResponse,
    OrbitDirection,
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

export function gsdText({
    gsdMeters,
}: Pick<ImageResponse, "gsdMeters">): string {
    return gsdMeters === null ? "-" : `${gsdMeters} m`;
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

export function providerText({
    provider,
}: Pick<ImageResponse, "provider">): string {
    return `${provider.name} (${provider.isPaid ? "유료" : "무료"})`;
}
