"use client";

import { toast } from "sonner";

import {
    IMAGE_FORM_DIALOG_CLASS,
    ImageForm,
} from "@/features/image/admin/ImageForm";
import { toFormValues } from "@/features/image/admin/imageFormValues";
import { useImageDetail, useUpdateImage } from "@/features/image/queries";
import { ErrorMessage } from "@/shared/components/ErrorMessage";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
} from "@/shared/components/ui/dialog";

type EditImageDialogProps = {
    imageId: number | null;
    onClose: () => void;
};

// 수정할 영상 id가 있으면 열린다
export function EditImageDialog({ imageId, onClose }: EditImageDialogProps) {
    return (
        <Dialog
            open={imageId !== null}
            onOpenChange={(open) => {
                if (!open) {
                    onClose();
                }
            }}
        >
            <DialogContent className={IMAGE_FORM_DIALOG_CLASS}>
                <DialogHeader>
                    <DialogTitle>영상 수정</DialogTitle>
                </DialogHeader>
                {imageId !== null && (
                    <EditImageForm imageId={imageId} onSaved={onClose} />
                )}
            </DialogContent>
        </Dialog>
    );
}

type EditImageFormProps = {
    imageId: number;
    onSaved: () => void;
};

// 상세가 온 뒤에 폼을 그려서, 그 값을 폼 state의 초기값으로 쓴다
function EditImageForm({ imageId, onSaved }: EditImageFormProps) {
    const detail = useImageDetail(imageId);
    const updateImage = useUpdateImage(imageId);

    if (detail.error) {
        return (
            <ErrorMessage
                error={detail.error}
                onRetry={() => detail.refetch()}
            />
        );
    }

    if (!detail.data) {
        return (
            <p className="text-sm text-muted-foreground">
                불러오는 중입니다...
            </p>
        );
    }

    return (
        <ImageForm
            initialValues={toFormValues(detail.data)}
            submitLabel="저장"
            pending={updateImage.isPending}
            error={updateImage.error}
            onSubmit={(form) =>
                updateImage.mutate(form, {
                    onSuccess: () => {
                        onSaved();
                        toast.success("영상을 수정했습니다.");
                    },
                })
            }
        />
    );
}
