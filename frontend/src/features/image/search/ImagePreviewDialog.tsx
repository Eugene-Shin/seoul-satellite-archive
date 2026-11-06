"use client";

import { ImageIcon, XIcon } from "lucide-react";
import { useState } from "react";

import { IconButton } from "@/shared/components/IconButton";
import {
    Dialog,
    DialogClose,
    DialogContent,
    DialogTrigger,
} from "@/shared/components/ui/dialog";

export const PREVIEW_LABEL = "위성 영상 미리보기";

type ImagePreviewDialogProps = {
    title: string;
    thumbnailUrl: string;
    onOpen: () => void;
};

export function ImagePreviewDialog({
    title,
    thumbnailUrl,
    onOpen,
}: ImagePreviewDialogProps) {
    const [failed, setFailed] = useState(false);

    return (
        <Dialog>
            <DialogTrigger
                onClick={onOpen}
                render={
                    <IconButton
                        label={PREVIEW_LABEL}
                        title={PREVIEW_LABEL}
                        className="shrink-0"
                    />
                }
            >
                <ImageIcon />
            </DialogTrigger>
            <DialogContent
                aria-label={PREVIEW_LABEL}
                showCloseButton={false}
                className="w-fit overflow-hidden p-0 sm:max-w-[calc(100%-2rem)]"
            >
                {failed ? (
                    <p className="p-4 pr-12 text-muted-foreground">
                        미리보기를 불러오지 못했습니다.
                    </p>
                ) : (
                    // 출처 서버의 이미지를 그대로 보여주므로 next/image의 최적화 서버를 거치지 않는다
                    // eslint-disable-next-line @next/next/no-img-element
                    <img
                        src={thumbnailUrl}
                        alt={title}
                        onError={() => setFailed(true)}
                        className="block max-h-[85vh] max-w-full object-contain"
                    />
                )}
                <DialogClose
                    render={
                        <IconButton
                            label="미리보기 닫기"
                            className="absolute top-2 right-2"
                        />
                    }
                >
                    <XIcon />
                </DialogClose>
            </DialogContent>
        </Dialog>
    );
}
