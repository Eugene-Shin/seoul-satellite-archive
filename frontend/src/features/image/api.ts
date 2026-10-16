import {
    imageFilterOptionsSchema,
    imageSearchResultSchema,
} from "@/features/image/schemas";
import type { ImageSearchParams } from "@/features/image/types";
import { apiGet } from "@/shared/api/client";

export function fetchFilterOptions(signal?: AbortSignal) {
    return apiGet(
        "/api/images/filter-options",
        imageFilterOptionsSchema,
        {},
        signal,
    );
}

export function searchImages(params: ImageSearchParams, signal?: AbortSignal) {
    return apiGet("/api/images", imageSearchResultSchema, params, signal);
}
