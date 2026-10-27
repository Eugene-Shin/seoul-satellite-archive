import {
    keepPreviousData,
    useInfiniteQuery,
    useMutation,
    useQuery,
    useQueryClient,
} from "@tanstack/react-query";

import {
    type ImageFormValues,
    toWriteRequest,
} from "@/features/image/admin/imageFormValues";
import {
    createImage,
    fetchFilterOptions,
    searchImages,
} from "@/features/image/api";
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

export function useAdminImages(conditions: ImageSearchParams) {
    return useInfiniteQuery({
        queryKey: ["image", "admin", conditions],
        queryFn: ({ pageParam, signal }) =>
            searchImages(
                { ...conditions, cursor: pageParam, size: ADMIN_PAGE_SIZE },
                signal,
            ),
        initialPageParam: undefined as string | undefined,
        getNextPageParam: (lastPage) => lastPage.nextCursor,
        placeholderData: keepPreviousData,
    });
}

// 변환도 mutationFn 안에서 해서 JSON 문법 오류와 서버 오류가 같은 error로 모인다
export function useCreateImage() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (form: ImageFormValues) =>
            createImage(toWriteRequest(form)),
        onSuccess: () => {
            // 기다리지 않는다: 관리 목록 전체를 다시 받는 동안 팝업을 붙잡아 두지 않게
            void queryClient.invalidateQueries({ queryKey: ["image"] });
        },
    });
}
