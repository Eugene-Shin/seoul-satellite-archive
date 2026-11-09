"use client";

import { ImageOffIcon, RadarIcon } from "lucide-react";
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
import { isHttpUrl } from "@/features/image/search/fileLinks";
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
        ? "border-foreground ring-foreground bg-accent"
        : "border-ring ring-ring bg-muted";
    const stateClassName =
        selected || hovered
            ? `ring-1 ${emphasisClassName}`
            : "border-border bg-card";

    return (
        <button
            type="button"
            aria-pressed={selected}
            onClick={onSelect}
            onPointerEnter={() => onHoverStart()}
            onPointerLeave={() => onHoverEnd()}
            className={`flex h-[292px] w-full flex-col gap-2 overflow-hidden rounded-xl border p-3 text-left text-card-foreground focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none ${stateClassName}`}
        >
            <Thumbnail
                key={image.thumbnailUrl}
                url={image.thumbnailUrl}
                sar={image.sensorType === "SAR"}
            />
            <div className="flex min-w-0 items-center justify-between gap-2">
                <span
                    className="truncate text-xs text-muted-foreground"
                    title={satelliteText(image)}
                >
                    {satelliteText(image)}
                </span>
                <span className="shrink-0 rounded-full bg-muted px-2 py-0.5 text-xs font-medium">
                    {sensorTypeText(image)}
                </span>
            </div>
            <h2 className="truncate font-semibold" title={image.title}>
                {image.title}
            </h2>
            <dl className="grid grid-cols-[max-content_1fr] gap-x-3 gap-y-1 text-xs">
                <dt className="text-muted-foreground">촬영일(KST)</dt>
                <dd className="truncate">{acquiredAtText(image)}</dd>
                <dt className="text-muted-foreground">해상도</dt>
                <dd>{resolutionText(image)}</dd>
            </dl>
            <div className="mt-auto flex w-full min-w-0 items-center justify-between gap-2 border-t pt-2 text-xs text-muted-foreground">
                <span className="truncate" title={providerText(image)}>
                    {image.provider.name}
                </span>
                <span className="shrink-0">
                    {image.sensorType === "OPTICAL"
                        ? `구름 ${cloudCoverText(image)}`
                        : `궤도 ${orbitDirectionText(image)}`}
                </span>
            </div>
        </button>
    );
}

function Thumbnail({ url, sar }: { url: string | null; sar: boolean }) {
    const [failed, setFailed] = useState(false);

    return (
        <div className="flex h-24 w-full shrink-0 items-center justify-center overflow-hidden rounded bg-muted text-muted-foreground">
            {isHttpUrl(url) && !failed ? (
                // Provider URLs are dynamic; load directly without a server image proxy.
                // eslint-disable-next-line @next/next/no-img-element
                <img
                    src={url}
                    alt="위성영상 썸네일"
                    width={320}
                    height={96}
                    loading="lazy"
                    referrerPolicy="no-referrer"
                    onError={() => setFailed(true)}
                    className="h-full w-full object-contain"
                />
            ) : (
                <span className="flex items-center gap-2 text-sm">
                    {sar ? (
                        <RadarIcon aria-hidden="true" className="size-7" />
                    ) : (
                        <ImageOffIcon aria-hidden="true" className="size-5" />
                    )}
                    썸네일 없음
                </span>
            )}
        </div>
    );
}
