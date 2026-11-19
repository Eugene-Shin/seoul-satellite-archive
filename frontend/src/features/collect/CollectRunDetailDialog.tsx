"use client";

import { useCollectRunSkips } from "@/features/collect/queries";
import {
    isActive,
    RUN_TYPE_NAMES,
    runStatusText,
    runWindowText,
} from "@/features/collect/runText";
import type { CollectRun } from "@/features/collect/types";
import { ErrorMessage } from "@/shared/components/ErrorMessage";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
} from "@/shared/components/ui/dialog";
import { formatKstDateTime } from "@/shared/utils/date";

type CollectRunDetailDialogProps = {
    run: CollectRun | null;
    onClose: () => void;
};

export function CollectRunDetailDialog({
    run,
    onClose,
}: CollectRunDetailDialogProps) {
    const detail = run ? <RunDetail run={run} /> : null;

    return (
        // 여는 버튼이 없어서 onOpenChange는 닫을 때만 온다
        <Dialog open={run !== null} onOpenChange={onClose}>
            <DialogContent className="flex max-h-[80vh] flex-col sm:max-w-2xl">
                <DialogHeader>
                    <DialogTitle>실행 상세</DialogTitle>
                </DialogHeader>
                {detail}
            </DialogContent>
        </Dialog>
    );
}

function timeText(value: string | null): string {
    return value ? formatKstDateTime(value) : "-";
}

function RunDetail({ run }: { run: CollectRun }) {
    const rows: [string, string][] = [
        ["컬렉션", run.collectionTitle],
        ["종류", RUN_TYPE_NAMES[run.runType]],
        ["상태", runStatusText(run)],
        ["조회 구간", runWindowText(run)],
        [
            "발견 / 추가 / 갱신 / 건너뜀",
            `${run.found} / ${run.inserted} / ${run.updated} / ${run.skipped}`,
        ],
        ["등록 시각", timeText(run.createdAt)],
        ["시작 시각", timeText(run.startedAt)],
        ["종료 시각", timeText(run.finishedAt)],
    ];

    return (
        <div className="flex min-h-0 flex-col gap-3 text-sm">
            <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1">
                {rows.map(([label, value]) => (
                    <div key={label} className="contents">
                        <dt className="text-muted-foreground">{label}</dt>
                        <dd className="break-all">{value}</dd>
                    </div>
                ))}
            </dl>
            {run.failureReason && (
                <p className="break-all text-destructive">
                    실패 사유: {run.failureReason}
                </p>
            )}
            {/* 건너뛴 영상은 실행이 끝날 때 한꺼번에 기록된다 */}
            {!isActive(run.status) && run.skipped > 0 && (
                <SkipList runId={run.runId} />
            )}
        </div>
    );
}

function SkipList({ runId }: { runId: number }) {
    const { data: skips, error, refetch } = useCollectRunSkips(runId);

    return (
        <div className="flex min-h-0 flex-col gap-2">
            <h3 className="font-medium">건너뛴 영상</h3>
            <ErrorMessage error={error} onRetry={() => refetch()} />
            <ul className="min-h-0 divide-y overflow-auto rounded-lg border">
                {/* 원본 id가 없는 영상은 sourceId가 겹칠 수 있다 */}
                {(skips ?? []).map((skip, index) => (
                    <li key={index} className="px-3 py-2">
                        <p className="font-medium break-all">{skip.sourceId}</p>
                        <p className="break-all text-muted-foreground">
                            {skip.reason}
                        </p>
                    </li>
                ))}
            </ul>
        </div>
    );
}
