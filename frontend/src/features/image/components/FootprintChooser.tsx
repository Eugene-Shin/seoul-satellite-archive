"use client";

import { XIcon } from "lucide-react";

import { acquiredAtText, satelliteText } from "@/features/image/imageText";
import type { ImageResponse } from "@/features/image/types";
import { IconButton } from "@/shared/components/IconButton";

type FootprintChooserProps = {
    images: ImageResponse[];
    x: number;
    y: number;
    flipX: boolean;
    flipY: boolean;
    onSelect: (image: ImageResponse) => void;
    onClose: () => void;
};

export function FootprintChooser({
    images,
    x,
    y,
    flipX,
    flipY,
    onSelect,
    onClose,
}: FootprintChooserProps) {
    const flipClassName = `${flipX ? "-translate-x-full" : ""} ${flipY ? "-translate-y-full" : ""}`;

    return (
        <div
            className={`absolute z-10 flex max-h-72 w-72 flex-col overflow-hidden rounded-lg border bg-popover text-popover-foreground shadow-lg ${flipClassName}`}
            style={{ left: x, top: y }}
        >
            <div className="flex shrink-0 items-center justify-between border-b py-0.5 pr-1 pl-3 text-sm">
                <span>영상 {images.length}개</span>
                <IconButton label="선택 목록 닫기" onClick={onClose}>
                    <XIcon />
                </IconButton>
            </div>
            <ul className="min-h-0 overflow-y-auto p-1">
                {images.map((image) => (
                    <li key={image.imageId}>
                        <button
                            type="button"
                            onClick={() => onSelect(image)}
                            className="flex w-full flex-col rounded-md px-2 py-1.5 text-left text-sm hover:bg-muted focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
                        >
                            <span className="truncate font-medium">
                                {image.title}
                            </span>
                            <span className="text-xs text-muted-foreground">
                                {satelliteText(image)} · {acquiredAtText(image)}
                            </span>
                        </button>
                    </li>
                ))}
            </ul>
        </div>
    );
}
