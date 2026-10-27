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
    fetchImageDetail,
    searchImages,
    updateImage,
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

export function useImageDetail(imageId: number) {
    return useQuery({
        queryKey: ["image", "detail", imageId],
        queryFn: ({ signal }) => fetchImageDetail(imageId, signal),
    });
}

// 쓰기 뒤 검색 결과·관리 목록·선택지·상세를 모두 다시 받게 한다
function useInvalidateImages() {
    const queryClient = useQueryClient();

    return () => {
        // 기다리지 않는다: 관리 목록 전체를 다시 받는 동안 팝업을 붙잡아 두지 않게
        void queryClient.invalidateQueries({ queryKey: ["image"] });
    };
}

// 변환도 mutationFn 안에서 해서 JSON 문법 오류와 서버 오류가 같은 error로 모인다
export function useCreateImage() {
    const invalidateImages = useInvalidateImages();

    return useMutation({
        mutationFn: (form: ImageFormValues) =>
            createImage(toWriteRequest(form)),
        onSuccess: invalidateImages,
    });
}

export function useUpdateImage(imageId: number) {
    const invalidateImages = useInvalidateImages();

    return useMutation({
        mutationFn: (form: ImageFormValues) =>
            updateImage(imageId, toWriteRequest(form)),
        onSuccess: invalidateImages,
    });
}
