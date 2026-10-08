import { apiGet } from "@/shared/api/client";
import type { SliceResponse } from "@/shared/api/types";
import type {
    ImageFilterOptionsResponse,
    ImageResponse,
    ImageSearchParams,
} from "@/features/image/types";

export function fetchFilterOptions(): Promise<ImageFilterOptionsResponse> {
    return apiGet<ImageFilterOptionsResponse>("/api/images/filter-options");
}

export function searchImages(
    params: ImageSearchParams,
): Promise<SliceResponse<ImageResponse>> {
    return apiGet<SliceResponse<ImageResponse>>("/api/images", params);
}
