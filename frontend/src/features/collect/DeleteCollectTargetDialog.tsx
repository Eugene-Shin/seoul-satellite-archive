"use client";

import { toast } from "sonner";

import { useDeleteCollectTarget } from "@/features/collect/queries";
import type { CollectTarget } from "@/features/collect/types";
import { ErrorMessage } from "@/shared/components/ErrorMessage";
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from "@/shared/components/ui/alert-dialog";

type DeleteCollectTargetDialogProps = {
    target: CollectTarget | null;
    onClose: () => void;
};

export function DeleteCollectTargetDialog({
    target,
    onClose,
}: DeleteCollectTargetDialogProps) {
    // 삭제 훅은 대상이 있어야 부를 수 있어서, 대상이 있을 때만 내용을 그린다
    const confirm = target ? (
        <DeleteConfirm target={target} onDeleted={onClose} />
    ) : null;

    return (
        // 여는 버튼이 없어서 onOpenChange는 닫을 때만 온다
        <AlertDialog open={target !== null} onOpenChange={onClose}>
            <AlertDialogContent>{confirm}</AlertDialogContent>
        </AlertDialog>
    );
}

type DeleteConfirmProps = {
    target: CollectTarget;
    onDeleted: () => void;
};

function DeleteConfirm({ target, onDeleted }: DeleteConfirmProps) {
    const deleteTarget = useDeleteCollectTarget(target.targetId);

    return (
        <>
            <AlertDialogHeader className="gap-3 break-keep sm:group-data-[size=default]/alert-dialog-content:place-items-center sm:group-data-[size=default]/alert-dialog-content:text-center">
                <AlertDialogTitle>수집 대상 삭제</AlertDialogTitle>
                <AlertDialogDescription>
                    ‘{target.collection.title}’ 수집 대상을 삭제하시겠습니까?
                </AlertDialogDescription>
                <p className="text-sm text-muted-foreground">
                    실행 이력은 함께 지워지고, 수집한 영상은 남아서 일반 영상이
                    됩니다.
                </p>
            </AlertDialogHeader>
            <ErrorMessage error={deleteTarget.error} />
            <AlertDialogFooter className="py-2.5">
                <AlertDialogCancel>취소</AlertDialogCancel>
                <AlertDialogAction
                    variant="destructive"
                    disabled={deleteTarget.isPending}
                    onClick={() =>
                        deleteTarget.mutate(undefined, {
                            onSuccess: () => {
                                onDeleted();
                                toast.success("수집 대상을 삭제했습니다.");
                            },
                        })
                    }
                >
                    {deleteTarget.isPending ? "삭제 중..." : "삭제"}
                </AlertDialogAction>
            </AlertDialogFooter>
        </>
    );
}
