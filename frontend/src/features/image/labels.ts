import type { OrbitDirection, SensorType } from "@/features/image/types";

export const SENSOR_TYPE_LABELS: Record<SensorType, string> = {
    OPTICAL: "광학",
    SAR: "SAR",
};

export const ORBIT_DIRECTION_LABELS: Record<OrbitDirection, string> = {
    ASCENDING: "상향",
    DESCENDING: "하향",
};
