"use client";

import { PlusIcon } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import {
    IMAGE_FORM_DIALOG_CLASS,
    ImageForm,
} from "@/features/image/admin/ImageForm";
import { EMPTY_IMAGE_FORM } from "@/features/image/admin/imageFormValues";
import { useCreateImage } from "@/features/image/queries";
import { Button } from "@/shared/components/ui/button";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/shared/components/ui/dialog";

export function CreateImageDialog() {
    const [open, setOpen] = useState(false);
    const createImage = useCreateImage();

    return (
        <Dialog
            open={open}
            onOpenChange={(next) => {
                setOpen(next);
                if (!next) {
                    createImage.reset();
                }
            }}
        >
            <DialogTrigger render={<Button variant="outline" size="sm" />}>
                <PlusIcon />
                추가
            </DialogTrigger>
            <DialogContent className={IMAGE_FORM_DIALOG_CLASS}>
                <DialogHeader>
                    <DialogTitle>영상 추가</DialogTitle>
                </DialogHeader>
                <ImageForm
                    initialValues={EMPTY_IMAGE_FORM}
                    submitLabel="등록"
                    pending={createImage.isPending}
                    error={createImage.error}
                    onSubmit={(form) =>
                        createImage.mutate(form, {
                            onSuccess: () => {
                                setOpen(false);
                                toast.success("영상을 등록했습니다.");
                            },
                        })
                    }
                />
            </DialogContent>
        </Dialog>
    );
}
