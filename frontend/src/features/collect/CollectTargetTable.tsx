"use client";

import { PencilIcon, PlayIcon, PlusIcon, Trash2Icon } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import {
    type CollectTargetEditing,
    CollectTargetFormDialog,
} from "@/features/collect/CollectTargetFormDialog";
import { DeleteCollectTargetDialog } from "@/features/collect/DeleteCollectTargetDialog";
import {
    useCollectTargets,
    useRunCollectTarget,
    useToggleCollectTarget,
} from "@/features/collect/queries";
import { describeCron } from "@/features/collect/schedule";
import type { CollectTarget } from "@/features/collect/types";
import { ErrorMessage } from "@/shared/components/ErrorMessage";
import { IconButton } from "@/shared/components/IconButton";
import { Button } from "@/shared/components/ui/button";
import { Switch } from "@/shared/components/ui/switch";
import { formatKstDateTime } from "@/shared/utils/date";

const CELL = "px-3 py-2 whitespace-nowrap";
const HEADER_CELL = `${CELL} text-left font-medium`;
const MANAGE_CELL = `${CELL} sticky right-0 text-center`;

export function CollectTargetTable() {
    const { data: targets, error, refetch } = useCollectTargets();
    const [editing, setEditing] = useState<CollectTargetEditing>(null);
    const [deleting, setDeleting] = useState<CollectTarget | null>(null);

    return (
        <div className="flex max-h-[40%] min-h-0 shrink-0 flex-col gap-3">
            <div className="flex justify-end">
                <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setEditing("new")}
                >
                    <PlusIcon />
                    추가
                </Button>
            </div>
            <ErrorMessage error={error} onRetry={() => refetch()} />
            <div className="min-h-0 flex-1 overflow-auto rounded-lg border">
                <table className="w-full text-sm">
                    <thead className="sticky top-0 z-10 bg-muted">
                        <tr>
                            <th className={HEADER_CELL}>컬렉션</th>
                            <th className={HEADER_CELL}>일정</th>
                            <th className={HEADER_CELL}>다음 실행</th>
                            <th className={HEADER_CELL}>재조회 기간</th>
                            <th className={HEADER_CELL}>정기 수집</th>
                            <th
                                className={`${MANAGE_CELL} bg-muted font-medium`}
                            >
                                관리
                            </th>
                        </tr>
                    </thead>
                    <tbody>
                        {(targets ?? []).map((target) => (
                            <CollectTargetRow
                                key={target.targetId}
                                target={target}
                                onEdit={() => setEditing(target)}
                                onDelete={() => setDeleting(target)}
                            />
                        ))}
                    </tbody>
                </table>
                <EmptyNotice targets={targets} />
            </div>
            <CollectTargetFormDialog
                editing={editing}
                onClose={() => setEditing(null)}
            />
            <DeleteCollectTargetDialog
                target={deleting}
                onClose={() => setDeleting(null)}
            />
        </div>
    );
}

function EmptyNotice({ targets }: { targets: CollectTarget[] | undefined }) {
    if (!targets || targets.length > 0) {
        return null;
    }
    return (
        <p className="p-4 text-center text-sm text-muted-foreground">
            수집 대상이 없습니다.
        </p>
    );
}

type CollectTargetRowProps = {
    target: CollectTarget;
    onEdit: () => void;
    onDelete: () => void;
};

function CollectTargetRow({ target, onEdit, onDelete }: CollectTargetRowProps) {
    const toggle = useToggleCollectTarget(target);
    const run = useRunCollectTarget(target.targetId);
    const title = target.collection.title;
    const schedule = describeCron(target.scheduleCron) ?? target.scheduleCron;
    const nextRun = target.nextRunAt
        ? formatKstDateTime(target.nextRunAt)
        : "꺼짐";

    function runNow() {
        run.mutate(undefined, {
            onSuccess: () => toast.success("수집을 등록했습니다."),
            onError: (error) => toast.error(error.message),
        });
    }

    function changeEnabled(enabled: boolean) {
        toggle.mutate(enabled, {
            onError: (error) => toast.error(error.message),
        });
    }

    return (
        <tr className="border-b">
            <td className={CELL}>
                <span className="block max-w-60 truncate" title={title}>
                    {title}
                </span>
            </td>
            <td className={CELL} title={target.scheduleCron}>
                {schedule}
            </td>
            <td className={CELL}>{nextRun}</td>
            <td className={CELL}>{target.lookbackDays}일</td>
            <td className={CELL}>
                <Switch
                    aria-label={`${title} 정기 수집`}
                    checked={target.enabled}
                    disabled={toggle.isPending}
                    onCheckedChange={changeEnabled}
                />
            </td>
            <td className={`${MANAGE_CELL} bg-background`}>
                <IconButton
                    label={`${title} 지금 실행`}
                    variant="ghost"
                    size="icon-xs"
                    disabled={run.isPending}
                    onClick={runNow}
                >
                    <PlayIcon />
                </IconButton>
                <IconButton
                    label={`${title} 수정`}
                    variant="ghost"
                    size="icon-xs"
                    onClick={onEdit}
                >
                    <PencilIcon />
                </IconButton>
                <IconButton
                    label={`${title} 삭제`}
                    variant="ghost"
                    size="icon-xs"
                    onClick={onDelete}
                >
                    <Trash2Icon />
                </IconButton>
            </td>
        </tr>
    );
}
