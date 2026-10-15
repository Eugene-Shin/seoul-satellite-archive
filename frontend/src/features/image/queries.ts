import { useInfiniteQuery, useQuery } from "@tanstack/react-query";

import { fetchFilterOptions, searchImages } from "@/features/image/api";
import type { ImageSearchParams } from "@/features/image/types";

const FILTER_OPTIONS_STALE_TIME = 5 * 60 * 1000;

export function useFilterOptions() {
    return useQuery({
        queryKey: ["image", "filter-options"],
        queryFn: fetchFilterOptions,
        staleTime: FILTER_OPTIONS_STALE_TIME,
    });
}

const SEARCH_PAGE_SIZE = 20;

export function useImageSearch(
    searchId: number,
    conditions: ImageSearchParams,
) {
    return useInfiniteQuery({
        queryKey: ["image", "search", searchId, conditions],
        queryFn: ({ pageParam }) =>
            searchImages({
                ...conditions,
                page: pageParam,
                size: SEARCH_PAGE_SIZE,
            }),
        initialPageParam: 0,
        getNextPageParam: (lastPage, allPages) =>
            lastPage.hasNext ? allPages.length : undefined,
    });
}
