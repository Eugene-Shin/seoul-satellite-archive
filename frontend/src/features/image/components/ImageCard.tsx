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
    onSelect: () => void;
};

export function ImageCard({ image, selected, onSelect }: ImageCardProps) {
    const stateClassName = selected
        ? "border-foreground ring-1 ring-foreground"
        : "border-zinc-300 hover:border-zinc-500 hover:bg-zinc-50 dark:hover:bg-zinc-900";

    return (
        <li>
            <button
                type="button"
                aria-pressed={selected}
                onClick={onSelect}
                className={`flex w-full flex-col gap-2 rounded border p-4 text-left focus-visible:ring-2 focus-visible:ring-zinc-500 focus-visible:outline-none ${stateClassName}`}
            >
                <h2 className="truncate font-semibold" title={image.title}>
                    {image.title}
                </h2>
                <dl className="grid grid-cols-[max-content_1fr] gap-x-3 gap-y-1 text-sm">
                    <dt className="text-zinc-500">위성</dt>
                    <dd>{satelliteText(image)}</dd>
                    <dt className="text-zinc-500">센서 유형</dt>
                    <dd>{sensorTypeText(image)}</dd>
                    <dt className="text-zinc-500">촬영일시(KST)</dt>
                    <dd>{acquiredAtText(image)}</dd>
                    <dt className="text-zinc-500">해상도</dt>
                    <dd>{gsdText(image)}</dd>
                    <dt className="text-zinc-500">구름량</dt>
                    <dd>{cloudCoverText(image)}</dd>
                    <dt className="text-zinc-500">궤도 방향</dt>
                    <dd>{orbitDirectionText(image)}</dd>
                    <dt className="text-zinc-500">제공 기관</dt>
                    <dd>{providerText(image)}</dd>
                </dl>
            </button>
        </li>
    );
}
