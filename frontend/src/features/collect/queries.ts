import {
    type InfiniteData,
    useInfiniteQuery,
    useMutation,
    useQuery,
    useQueryClient,
} from "@tanstack/react-query";

import {
    cancelCollectRun,
    createCollectTarget,
    deleteCollectTarget,
    fetchCollectRuns,
    fetchCollectRunSkips,
    fetchCollectTargets,
    fetchDefaultAoi,
    runCollectTarget,
    updateCollectTarget,
} from "@/features/collect/api";
import {
    type CollectTargetFormValues,
    requestFromTarget,
    toRequest,
} from "@/features/collect/collectTargetFormValues";
import { isActive } from "@/features/collect/runText";
import type { CollectRunPage, CollectTarget } from "@/features/collect/types";

const TARGETS_KEY = ["collect", "targets"];

export function useCollectTargets() {
    return useQuery({
        queryKey: TARGETS_KEY,
        queryFn: ({ signal }) => fetchCollectTargets(signal),
    });
}

export function useDefaultAoi() {
    return useQuery({
        queryKey: ["collect", "default-aoi"],
        queryFn: ({ signal }) => fetchDefaultAoi(signal),
        staleTime: Infinity,
    });
}

function useInvalidateTargets() {
    const queryClient = useQueryClient();

    return () => {
        void queryClient.invalidateQueries({ queryKey: TARGETS_KEY });
    };
}

// target이 없으면 등록, 있으면 수정. 변환도 mutationFn 안에서 해서 입력 오류와 서버 오류가 같은 error로 모인다
export function useSaveCollectTarget(target: CollectTarget | null) {
    const invalidateTargets = useInvalidateTargets();

    return useMutation({
        mutationFn: (form: CollectTargetFormValues) =>
            target
                ? updateCollectTarget(target.targetId, toRequest(form))
                : createCollectTarget(toRequest(form)),
        onSuccess: invalidateTargets,
    });
}

export function useToggleCollectTarget(target: CollectTarget) {
    const invalidateTargets = useInvalidateTargets();

    return useMutation({
        mutationFn: (enabled: boolean) =>
            updateCollectTarget(
                target.targetId,
                requestFromTarget(target, { enabled }),
            ),
        onSuccess: invalidateTargets,
    });
}

// 수집한 영상이 일반 영상이 되므로 영상 목록도 다시 받는다
export function useDeleteCollectTarget(targetId: number) {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: () => deleteCollectTarget(targetId),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: TARGETS_KEY });
            void queryClient.invalidateQueries({ queryKey: ["image"] });
        },
    });
}

const RUNS_KEY = ["collect", "runs"];
const ACTIVE_POLL_MS = 5000;

type RunPages = InfiniteData<CollectRunPage, string | undefined>;

// 대기·실행 중인 실행이 있는 동안만 다시 받는다
export function useCollectRuns() {
    return useInfiniteQuery({
        queryKey: RUNS_KEY,
        queryFn: ({ pageParam, signal }) => fetchCollectRuns(pageParam, signal),
        initialPageParam: undefined as string | undefined,
        getNextPageParam: (lastPage) => lastPage.nextCursor,
        refetchInterval: (query) =>
            query.state.data?.pages.some((page) =>
                page.content.some((run) => isActive(run.status)),
            )
                ? ACTIVE_POLL_MS
                : false,
    });
}

export function useCollectRunSkips(runId: number) {
    return useQuery({
        queryKey: [...RUNS_KEY, runId, "skips"],
        queryFn: ({ signal }) => fetchCollectRunSkips(runId, signal),
    });
}

// 등록 응답을 목록 맨 위에 먼저 넣어 둔다. 다시 받았을 때 이미 끝나 있어도 끝난 실행으로 알아챈다
export function useRunCollectTarget(targetId: number) {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: () => runCollectTarget(targetId),
        onSuccess: (run) => {
            queryClient.setQueryData<RunPages>(RUNS_KEY, (data) =>
                data
                    ? {
                          ...data,
                          pages: data.pages.map((page, index) =>
                              index === 0
                                  ? { ...page, content: [run, ...page.content] }
                                  : page,
                          ),
                      }
                    : data,
            );
            void queryClient.invalidateQueries({ queryKey: RUNS_KEY });
        },
    });
}

export function useCancelCollectRun(runId: number) {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: () => cancelCollectRun(runId),
        onSuccess: () => {
            void queryClient.invalidateQueries({ queryKey: RUNS_KEY });
        },
    });
}
