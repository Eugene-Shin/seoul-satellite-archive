"use client";

import { GripHorizontalIcon, XIcon } from "lucide-react";
import { type PointerEvent, useEffect, useRef } from "react";

import {
    acquiredAtText,
    cloudCoverText,
    gsdText,
    orbitDirectionText,
    providerText,
    satelliteText,
    sensorTypeText,
    sourceUpdatedAtText,
} from "@/features/image/imageText";
import type { ImageResponse } from "@/features/image/types";
import { IconButton } from "@/shared/components/IconButton";

type ImageDetailProps = {
    image: ImageResponse | null;
    onClose: () => void;
};

type Position = { x: number; y: number };

function clamp(value: number, max: number): number {
    return Math.max(0, Math.min(value, max));
}

function moveTo(panel: HTMLElement, x: number, y: number) {
    const parent = panel.parentElement;
    if (!parent) {
        return;
    }
    panel.style.left = `${clamp(x, parent.clientWidth - panel.offsetWidth)}px`;
    panel.style.top = `${clamp(y, parent.clientHeight - panel.offsetHeight)}px`;
    panel.style.right = "auto";
    panel.style.bottom = "auto";
}

export function ImageDetail({ image, onClose }: ImageDetailProps) {
    const panelRef = useRef<HTMLElement>(null);
    const dragOffset = useRef<Position | null>(null);

    useEffect(() => {
        const panel = panelRef.current;
        const parent = panel?.parentElement;
        if (!panel || !parent) {
            return;
        }
        const observer = new ResizeObserver(() => {
            if (panel.style.left && !panel.hidden) {
                moveTo(panel, panel.offsetLeft, panel.offsetTop);
            }
        });
        observer.observe(parent);
        observer.observe(panel);
        return () => observer.disconnect();
    }, []);

    function pinPosition() {
        const panel = panelRef.current;
        if (panel) {
            moveTo(panel, panel.offsetLeft, panel.offsetTop);
        }
    }

    function startDrag(event: PointerEvent<HTMLDivElement>) {
        const panel = panelRef.current;
        if (!panel) {
            return;
        }
        event.preventDefault();
        event.currentTarget.setPointerCapture(event.pointerId);
        dragOffset.current = {
            x: event.clientX - panel.offsetLeft,
            y: event.clientY - panel.offsetTop,
        };
    }

    function drag(event: PointerEvent<HTMLDivElement>) {
        const panel = panelRef.current;
        if (!panel || !dragOffset.current) {
            return;
        }
        moveTo(
            panel,
            event.clientX - dragOffset.current.x,
            event.clientY - dragOffset.current.y,
        );
    }

    function endDrag() {
        dragOffset.current = null;
    }

    return (
        <aside
            ref={panelRef}
            hidden={!image}
            onPointerDownCapture={pinPosition}
            className="absolute right-4 bottom-4 flex h-64 max-h-full min-h-40 w-card max-w-full min-w-64 resize flex-col overflow-hidden rounded-lg border bg-popover text-popover-foreground shadow-lg"
        >
            <div className="flex shrink-0 items-center border-b py-0.5 pr-1">
                <div
                    onPointerDown={startDrag}
                    onPointerMove={drag}
                    onPointerUp={endDrag}
                    onPointerCancel={endDrag}
                    className="flex flex-1 cursor-move touch-none justify-center self-stretch select-none"
                >
                    <GripHorizontalIcon className="size-4 self-center text-muted-foreground" />
                </div>
                <IconButton label="상세 정보 닫기" onClick={onClose}>
                    <XIcon />
                </IconButton>
            </div>

            {image && <ImageDetailContent key={image.imageId} image={image} />}
        </aside>
    );
}

function ImageDetailContent({ image }: { image: ImageResponse }) {
    return (
        <div className="flex min-h-0 flex-1 flex-col gap-4 overflow-y-auto p-4">
            <h2 className="font-semibold break-all">{image.title}</h2>

            <dl className="grid grid-cols-[max-content_1fr] gap-x-3 gap-y-1 text-sm">
                <dt className="text-muted-foreground">영상 ID</dt>
                <dd>{image.imageId}</dd>
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
                <dt className="text-muted-foreground">원본 갱신(KST)</dt>
                <dd>{sourceUpdatedAtText(image)}</dd>
            </dl>

            <section className="flex flex-col gap-2">
                <h3 className="font-semibold">
                    다운로드 ({image.downloadUrls.length})
                </h3>
                <ul className="flex flex-col gap-1 text-sm">
                    {image.downloadUrls.map((url) => (
                        <li key={url}>
                            <DownloadLink url={url} />
                        </li>
                    ))}
                </ul>
            </section>
        </div>
    );
}

function DownloadLink({ url }: { url: string }) {
    const fileName = url.split("/").pop() || url;

    if (!url.startsWith("http")) {
        return (
            <span className="break-all text-muted-foreground" title={url}>
                {fileName}
            </span>
        );
    }

    return (
        <a
            href={url}
            target="_blank"
            rel="noopener noreferrer"
            className="break-all underline"
            title={url}
        >
            {fileName}
        </a>
    );
}
