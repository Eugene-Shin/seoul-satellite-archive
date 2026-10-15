"use client";

import { XIcon } from "lucide-react";

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

export function ImageDetail({ image, onClose }: ImageDetailProps) {
    if (!image) {
        return null;
    }

    return (
        <aside className="absolute right-4 bottom-4 h-64 w-card rounded-lg border bg-popover text-popover-foreground shadow-lg">
            <IconButton
                label="상세 정보 닫기"
                onClick={onClose}
                className="absolute top-3 right-3"
            >
                <XIcon />
            </IconButton>

            <div className="flex h-full flex-col gap-4 overflow-y-auto p-4">
                <h2 className="pr-10 font-semibold break-all">{image.title}</h2>

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
        </aside>
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
