"use client";

import { useQueryClient } from "@tanstack/react-query";
import { CalendarClockIcon } from "lucide-react";
import { useEffect, useMemo, useRef } from "react";
import { toast } from "sonner";

import { CollectRunTable } from "@/features/collect/CollectRunTable";
import { CollectTargetTable } from "@/features/collect/CollectTargetTable";
import { useCollectRuns } from "@/features/collect/queries";
import { finishedMessage, newlyFinished } from "@/features/collect/runText";
import type { CollectRunStatus } from "@/features/collect/types";
import { MapToolbarPillButton } from "@/features/image/map/MapToolbarPillButton";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/shared/components/ui/dialog";

const LABEL = "수집 관리";

export function CollectAdminDialog() {
    useRunFinishedNotice();

    return (
        <Dialog>
            <DialogTrigger render={<MapToolbarPillButton label={LABEL} />}>
                <CalendarClockIcon />
            </DialogTrigger>
            {/* 대상 표는 높이를 제한하고 남은 높이를 실행 이력이 쓴다 */}
            <DialogContent className="flex h-[85vh] w-[90vw] max-w-5xl flex-col sm:max-w-5xl">
                <DialogHeader>
                    <DialogTitle>{LABEL}</DialogTitle>
                </DialogHeader>
                <CollectTargetTable />
                <CollectRunTable />
            </DialogContent>
        </Dialog>
    );
}

// 팝업을 닫아도 대기·실행 중인 실행을 지켜보다가, 끝나면 알리고 수집한 영상이 보이도록 영상 목록을 다시 받는다.
// 나머지 페이지는 실행 이력 표가 받으므로, 팝업을 한 번도 열지 않았으면 첫 페이지만 지켜본다.
// 진행 중인 실행은 최근 등록이라 첫 페이지에 있다
function useRunFinishedNotice() {
    const queryClient = useQueryClient();
    const { data } = useCollectRuns();
    const previousStatuses = useRef(new Map<number, CollectRunStatus>());

    // 다른 이유로 다시 그릴 때마다 새 배열이 되지 않도록, 받은 데이터가 바뀔 때만 만든다
    const runs = useMemo(
        () => (data ? data.pages.flatMap((page) => page.content) : []),
        [data],
    );

    useEffect(() => {
        const finished = newlyFinished(previousStatuses.current, runs);
        for (const run of finished) {
            const { title, description } = finishedMessage(run);
            if (run.status === "SUCCEEDED") {
                toast.success(title, { description });
            } else {
                toast.error(title, { description });
            }
        }
        if (finished.length > 0) {
            void queryClient.invalidateQueries({ queryKey: ["image"] });
        }
        previousStatuses.current = new Map(
            runs.map((run) => [run.runId, run.status]),
        );
    }, [runs, queryClient]);
}
