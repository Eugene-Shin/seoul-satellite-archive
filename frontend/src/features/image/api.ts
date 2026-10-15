import {
    imageFilterOptionsSchema,
    imageSearchResultSchema,
} from "@/features/image/schemas";
import type { ImageSearchParams } from "@/features/image/types";
import { apiGet } from "@/shared/api/client";

export function fetchFilterOptions() {
    return apiGet("/api/images/filter-options", imageFilterOptionsSchema);
}

export function searchImages(params: ImageSearchParams) {
    return apiGet("/api/images", imageSearchResultSchema, params);
}
