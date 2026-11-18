import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import {
    createCollectTarget,
    deleteCollectTarget,
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
import type { CollectTarget } from "@/features/collect/types";

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

export function useRunCollectTarget(targetId: number) {
    return useMutation({
        mutationFn: () => runCollectTarget(targetId),
    });
}
