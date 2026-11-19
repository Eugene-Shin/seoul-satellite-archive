"use client";

import { CircleStopIcon, InfoIcon } from "lucide-react";
import { useEffect, useState } from "react";
import { toast } from "sonner";

import { CollectRunDetailDialog } from "@/features/collect/CollectRunDetailDialog";
import {
    useCancelCollectRun,
    useCollectRuns,
} from "@/features/collect/queries";
import {
    isActive,
    RUN_TYPE_NAMES,
    runStatusText,
    runWindowText,
} from "@/features/collect/runText";
import type { CollectRun } from "@/features/collect/types";
import { ErrorMessage, toastError } from "@/shared/components/ErrorMessage";
import { IconButton } from "@/shared/components/IconButton";
import { formatKstDateTime } from "@/shared/utils/date";

const CELL = "px-3 py-2 whitespace-nowrap";
const HEADER_CELL = `${CELL} text-left font-medium`;
const MANAGE_CELL = `${CELL} sticky right-0 text-center`;

// 끝난 실행 알림은 팝업을 닫아도 동작하도록 CollectAdminDialog에서 한다
export function CollectRunTable() {
    const { data, error, fetchNextPage, hasNextPage, isFetching, refetch } =
        useCollectRuns();
    const [detailRunId, setDetailRunId] = useState<number | null>(null);

    const runs = data ? data.pages.flatMap((page) => page.content) : [];
    const detailRun = runs.find((run) => run.runId === detailRunId) ?? null;

    useEffect(() => {
        if (hasNextPage && !isFetching && !error) {
            fetchNextPage();
        }
    }, [hasNextPage, isFetching, error, fetchNextPage]);

    return (
        <div className="mt-3 flex min-h-0 flex-1 flex-col gap-3">
            <h3 className="font-heading text-base leading-none font-medium">
                실행 이력
            </h3>
            <ErrorMessage error={error} onRetry={() => refetch()} />
            <div className="min-h-0 flex-1 overflow-auto rounded-lg border">
                <table className="w-full text-sm">
                    <thead className="sticky top-0 z-10 bg-muted">
                        <tr>
                            <th className={HEADER_CELL}>등록 시각</th>
                            <th className={HEADER_CELL}>컬렉션</th>
                            <th className={HEADER_CELL}>종류</th>
                            <th className={HEADER_CELL}>상태</th>
                            <th className={HEADER_CELL}>조회 구간</th>
                            <th className={HEADER_CELL}>
                                발견 / 추가 / 갱신 / 건너뜀
                            </th>
                            <th className={HEADER_CELL}>종료 시각</th>
                            <th
                                className={`${MANAGE_CELL} bg-muted font-medium`}
                            >
                                관리
                            </th>
                        </tr>
                    </thead>
                    <tbody>
                        {runs.map((run) => (
                            <CollectRunRow
                                key={run.runId}
                                run={run}
                                onDetail={() => setDetailRunId(run.runId)}
                            />
                        ))}
                    </tbody>
                </table>
                <EmptyNotice runs={data ? runs : undefined} />
            </div>
            <CollectRunDetailDialog
                run={detailRun}
                onClose={() => setDetailRunId(null)}
            />
        </div>
    );
}

function EmptyNotice({ runs }: { runs: CollectRun[] | undefined }) {
    if (!runs || runs.length > 0) {
        return null;
    }
    return (
        <p className="p-4 text-center text-sm text-muted-foreground">
            실행 이력이 없습니다.
        </p>
    );
}

type CollectRunRowProps = {
    run: CollectRun;
    onDetail: () => void;
};

function CollectRunRow({ run, onDetail }: CollectRunRowProps) {
    const cancel = useCancelCollectRun(run.runId);
    const title = run.collectionTitle;
    const counts = `${run.found} / ${run.inserted} / ${run.updated} / ${run.skipped}`;
    const finishedAt = run.finishedAt ? formatKstDateTime(run.finishedAt) : "-";
    const cancelable = isActive(run.status) && run.cancelRequestedAt === null;

    function requestCancel() {
        cancel.mutate(undefined, {
            onSuccess: () => toast.success("취소를 요청했습니다."),
            onError: toastError,
        });
    }

    return (
        <tr className="border-b">
            <td className={CELL}>{formatKstDateTime(run.createdAt)}</td>
            <td className={CELL}>
                <span className="block max-w-60 truncate" title={title}>
                    {title}
                </span>
            </td>
            <td className={CELL}>{RUN_TYPE_NAMES[run.runType]}</td>
            <td className={CELL}>{runStatusText(run)}</td>
            <td className={CELL}>{runWindowText(run)}</td>
            <td className={CELL}>{counts}</td>
            <td className={CELL}>{finishedAt}</td>
            <td className={`${MANAGE_CELL} bg-background`}>
                <IconButton
                    label={`${title} 실행 상세`}
                    variant="ghost"
                    size="icon-xs"
                    onClick={onDetail}
                >
                    <InfoIcon />
                </IconButton>
                {cancelable && (
                    <IconButton
                        label={`${title} 실행 취소`}
                        variant="ghost"
                        size="icon-xs"
                        disabled={cancel.isPending}
                        onClick={requestCancel}
                    >
                        <CircleStopIcon />
                    </IconButton>
                )}
            </td>
        </tr>
    );
}
