import type { ImageSort } from "@/features/image/types";

export type ImageSortField = "acquiredAt" | "cloudCover";

export type SortDirection = "asc" | "desc";

export const SORT_OPTIONS: Record<
    ImageSort,
    { label: string; field: ImageSortField; direction: SortDirection }
> = {
    ACQUIRED_AT_DESC: {
        label: "촬영일 최신순",
        field: "acquiredAt",
        direction: "desc",
    },
    ACQUIRED_AT_ASC: {
        label: "촬영일 오래된순",
        field: "acquiredAt",
        direction: "asc",
    },
    CLOUD_COVER_ASC: {
        label: "구름량 적은순",
        field: "cloudCover",
        direction: "asc",
    },
    CLOUD_COVER_DESC: {
        label: "구름량 많은순",
        field: "cloudCover",
        direction: "desc",
    },
};

export function isSortedBy(
    sort: ImageSort | undefined,
    field: ImageSortField,
): boolean {
    return sort !== undefined && SORT_OPTIONS[sort].field === field;
}

export function sortDirectionOf(
    sort: ImageSort | undefined,
    field: ImageSortField,
): SortDirection | undefined {
    return sort !== undefined && isSortedBy(sort, field)
        ? SORT_OPTIONS[sort].direction
        : undefined;
}
