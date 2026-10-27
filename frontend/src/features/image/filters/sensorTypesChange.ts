import { emptyToUndefined } from "@/features/image/filters/emptyToUndefined";
import { isSortedBy } from "@/features/image/sortOptions";
import type { ImageSearchParams, SensorType } from "@/features/image/types";

export function isSarOnly(sensorTypes: SensorType[] | undefined): boolean {
    return sensorTypes ? sensorTypes.every((type) => type === "SAR") : false;
}

// SAR 영상은 구름량 정보가 없어서, SAR만 고르면 구름량 조건(값·정렬)을 함께 비운다
export function sensorTypesChange(
    conditions: ImageSearchParams,
    sensorTypes: SensorType[] | undefined,
): Partial<ImageSearchParams> {
    if (!isSarOnly(sensorTypes)) {
        return { sensorTypes };
    }
    return {
        sensorTypes,
        cloudMax: undefined,
        sorts: emptyToUndefined(
            (conditions.sorts ?? []).filter(
                (sort) => !isSortedBy(sort, "cloudCover"),
            ),
        ),
    };
}
