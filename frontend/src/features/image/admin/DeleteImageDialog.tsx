"use client";

import { toast } from "sonner";

import { useDeleteImage } from "@/features/image/queries";
import type { ImageResponse } from "@/features/image/types";
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

type DeleteImageDialogProps = {
    image: ImageResponse | null;
    onClose: () => void;
};

export function DeleteImageDialog({ image, onClose }: DeleteImageDialogProps) {
    return (
        <AlertDialog
            open={image !== null}
            onOpenChange={(open) => {
                if (!open) {
                    onClose();
                }
            }}
        >
            <AlertDialogContent>
                {image !== null && (
                    <DeleteImageConfirm image={image} onDeleted={onClose} />
                )}
            </AlertDialogContent>
        </AlertDialog>
    );
}

type DeleteImageConfirmProps = {
    image: ImageResponse;
    onDeleted: () => void;
};

function DeleteImageConfirm({ image, onDeleted }: DeleteImageConfirmProps) {
    const deleteImage = useDeleteImage(image.imageId);

    return (
        <>
            <AlertDialogHeader className="gap-3">
                <AlertDialogTitle>영상 삭제</AlertDialogTitle>
                <AlertDialogDescription>
                    ‘{image.title}’ 영상을 삭제하시겠습니까?
                </AlertDialogDescription>
            </AlertDialogHeader>
            <ErrorMessage error={deleteImage.error} />
            <AlertDialogFooter className="py-2.5">
                <AlertDialogCancel>취소</AlertDialogCancel>
                <AlertDialogAction
                    variant="destructive"
                    disabled={deleteImage.isPending}
                    onClick={() =>
                        deleteImage.mutate(undefined, {
                            onSuccess: () => {
                                onDeleted();
                                toast.success("영상을 삭제했습니다.");
                            },
                        })
                    }
                >
                    {deleteImage.isPending ? "삭제 중..." : "삭제"}
                </AlertDialogAction>
            </AlertDialogFooter>
        </>
    );
}
