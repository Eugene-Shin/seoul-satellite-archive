import { useInfiniteQuery, useQuery } from "@tanstack/react-query";

import { fetchFilterOptions, searchImages } from "@/features/image/api";
import type { ImageSearchParams } from "@/features/image/types";

const FILTER_OPTIONS_STALE_TIME = 5 * 60 * 1000;

export function useFilterOptions() {
    return useQuery({
        queryKey: ["image", "filter-options"],
        queryFn: ({ signal }) => fetchFilterOptions(signal),
        staleTime: FILTER_OPTIONS_STALE_TIME,
    });
}

const SEARCH_PAGE_SIZE = 100;

export function useImageSearch(
    searchId: number,
    conditions: ImageSearchParams,
) {
    return useInfiniteQuery({
        queryKey: ["image", "search", searchId, conditions],
        queryFn: ({ pageParam, signal }) =>
            searchImages(
                { ...conditions, cursor: pageParam, size: SEARCH_PAGE_SIZE },
                signal,
            ),
        initialPageParam: undefined as string | undefined,
        getNextPageParam: (lastPage) => lastPage.nextCursor,
    });
}

const ADMIN_PAGE_SIZE = 100;

export function useAdminImages() {
    return useInfiniteQuery({
        queryKey: ["image", "admin"],
        queryFn: ({ pageParam, signal }) =>
            searchImages({ cursor: pageParam, size: ADMIN_PAGE_SIZE }, signal),
        initialPageParam: undefined as string | undefined,
        getNextPageParam: (lastPage) => lastPage.nextCursor,
    });
}
