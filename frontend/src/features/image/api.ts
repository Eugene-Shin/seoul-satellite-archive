import {
    imageDetailSchema,
    imageFilterOptionsSchema,
    imageSearchResultSchema,
} from "@/features/image/schemas";
import type {
    ImageSearchParams,
    ImageWriteRequest,
} from "@/features/image/types";
import { apiGet, apiSend } from "@/shared/api/client";

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

export function fetchImageDetail(imageId: number, signal?: AbortSignal) {
    return apiGet(`/api/images/${imageId}`, imageDetailSchema, {}, signal);
}

export function createImage(request: ImageWriteRequest) {
    return apiSend("POST", "/api/images", request);
}

export function updateImage(imageId: number, request: ImageWriteRequest) {
    return apiSend("PUT", `/api/images/${imageId}`, request);
}

export function deleteImage(imageId: number) {
    return apiSend("DELETE", `/api/images/${imageId}`);
}
