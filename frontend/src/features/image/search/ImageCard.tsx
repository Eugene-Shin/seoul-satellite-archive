"use client";

import {
    acquiredAtText,
    cloudCoverText,
    gsdText,
    orbitDirectionText,
    providerText,
    satelliteText,
    sensorTypeText,
} from "@/features/image/imageText";
import type { ImageResponse } from "@/features/image/types";

type ImageCardProps = {
    image: ImageResponse;
    selected: boolean;
    hovered: boolean;
    onSelect: () => void;
    onHoverStart: () => void;
    onHoverEnd: () => void;
};

export function ImageCard({
    image,
    selected,
    hovered,
    onSelect,
    onHoverStart,
    onHoverEnd,
}: ImageCardProps) {
    const emphasisClassName = selected
        ? "border-foreground ring-foreground"
        : "border-ring ring-ring bg-muted";
    const stateClassName =
        selected || hovered ? `ring-1 ${emphasisClassName}` : "border-border";

    return (
        <button
            type="button"
            aria-pressed={selected}
            onClick={onSelect}
            onPointerEnter={() => onHoverStart()}
            onPointerLeave={() => onHoverEnd()}
            className={`flex w-full flex-col gap-2 rounded-lg border bg-card p-4 text-left text-card-foreground focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none ${stateClassName}`}
        >
            <h2 className="truncate font-semibold" title={image.title}>
                {image.title}
            </h2>
            <dl className="grid grid-cols-[max-content_1fr] gap-x-3 gap-y-1 text-sm">
                <dt className="text-muted-foreground">위성</dt>
                <dd>{satelliteText(image)}</dd>
                <dt className="text-muted-foreground">센서 유형</dt>
                <dd>{sensorTypeText(image)}</dd>
                <dt className="text-muted-foreground">촬영일시(KST)</dt>
                <dd>{acquiredAtText(image)}</dd>
                <dt className="text-muted-foreground">해상도</dt>
                <dd>{gsdText(image)}</dd>
                <dt className="text-muted-foreground">구름량</dt>
                <dd>{cloudCoverText(image)}</dd>
                <dt className="text-muted-foreground">궤도 방향</dt>
                <dd>{orbitDirectionText(image)}</dd>
                <dt className="text-muted-foreground">제공 기관</dt>
                <dd>{providerText(image)}</dd>
            </dl>
        </button>
    );
}
