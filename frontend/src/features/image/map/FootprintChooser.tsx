"use client";

import { XIcon } from "lucide-react";

import { acquiredAtText, satelliteText } from "@/features/image/imageText";
import type { ImageResponse } from "@/features/image/types";
import { IconButton } from "@/shared/components/IconButton";
import { Popover, PopoverContent } from "@/shared/components/ui/popover";

type FootprintChooserProps = {
    open: boolean;
    x: number;
    y: number;
    images: ImageResponse[];
    onOpenChange: (open: boolean) => void;
    onOpenChangeComplete: (open: boolean) => void;
    onSelect: (image: ImageResponse) => void;
    onHover: (imageIds: number[]) => void;
};

const EDGE_GAP = 16;

export function FootprintChooser({
    open,
    x,
    y,
    images,
    onOpenChange,
    onOpenChangeComplete,
    onSelect,
    onHover,
}: FootprintChooserProps) {
    const anchor = {
        getBoundingClientRect: () => DOMRect.fromRect({ x, y }),
    };

    return (
        <Popover
            open={open}
            onOpenChange={onOpenChange}
            onOpenChangeComplete={onOpenChangeComplete}
        >
            <PopoverContent
                anchor={anchor}
                collisionPadding={EDGE_GAP}
                className="max-h-(--spacing-chooser) w-(--spacing-chooser) gap-0 overflow-hidden p-0"
            >
                <div className="flex shrink-0 items-center justify-between border-b py-0.5 pr-1 pl-3">
                    <span>영상 {images.length}개</span>
                    <IconButton
                        label="선택 목록 닫기"
                        onClick={() => onOpenChange(false)}
                    >
                        <XIcon />
                    </IconButton>
                </div>
                <ul key={`${x},${y}`} className="min-h-0 overflow-y-auto p-1">
                    {images.map((image) => (
                        <li key={image.imageId}>
                            <button
                                type="button"
                                onClick={() => onSelect(image)}
                                onPointerEnter={() => onHover([image.imageId])}
                                onPointerLeave={() => onHover([])}
                                className="flex w-full flex-col rounded-md px-2 py-1.5 text-left hover:bg-muted focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
                            >
                                <span className="truncate font-medium">
                                    {image.title}
                                </span>
                                <span className="text-xs text-muted-foreground">
                                    {satelliteText(image)} ·{" "}
                                    {acquiredAtText(image)}
                                </span>
                            </button>
                        </li>
                    ))}
                </ul>
            </PopoverContent>
        </Popover>
    );
}
