"use client";

import { useState } from "react";

import {
    acquiredAtText,
    cloudCoverText,
    orbitDirectionText,
    providerText,
    resolutionText,
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
    const [thumbnailFailed, setThumbnailFailed] = useState(false);
    const hasThumbnail = image.thumbnailUrl !== null && !thumbnailFailed;
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
            <div className="flex items-center gap-3">
                {hasThumbnail && (
                    // 출처 서버의 이미지를 그대로 보여주므로 next/image의 최적화 서버를 거치지 않는다
                    // eslint-disable-next-line @next/next/no-img-element
                    <img
                        src={image.thumbnailUrl ?? undefined}
                        alt=""
                        loading="lazy"
                        onError={() => setThumbnailFailed(true)}
                        className="size-20 shrink-0 rounded border border-border object-cover"
                    />
                )}
                <h2
                    className={`min-w-0 font-semibold ${hasThumbnail ? "line-clamp-3 break-all" : "truncate"}`}
                    title={image.title}
                >
                    {image.title}
                </h2>
            </div>
            <dl className="grid grid-cols-[max-content_1fr] gap-x-3 gap-y-1 text-sm">
                <dt className="text-muted-foreground">위성</dt>
                <dd>{satelliteText(image)}</dd>
                <dt className="text-muted-foreground">센서 유형</dt>
                <dd>{sensorTypeText(image)}</dd>
                <dt className="text-muted-foreground">촬영일시(KST)</dt>
                <dd>{acquiredAtText(image)}</dd>
                <dt className="text-muted-foreground">해상도</dt>
                <dd>{resolutionText(image)}</dd>
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
