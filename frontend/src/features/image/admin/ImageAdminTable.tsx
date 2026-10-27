"use client";

import { useVirtualizer } from "@tanstack/react-virtual";
import { useEffect, useRef } from "react";

import {
    acquiredAtText,
    cloudCoverText,
    gsdText,
    orbitDirectionText,
    sensorTypeText,
} from "@/features/image/imageText";
import { useAdminImages } from "@/features/image/queries";
import type { ImageResponse } from "@/features/image/types";
import { ErrorMessage } from "@/shared/components/ErrorMessage";

const COLUMNS = [
    "제목",
    "위성",
    "센서",
    "유형",
    "촬영일시",
    "해상도",
    "구름량",
    "궤도",
    "제공 기관",
    "유료",
];

// 한 줄 고정: py-2(16px) + text-sm 줄 높이(20px) + 아래 테두리(1px)
const ROW_HEIGHT = 37;
const CELL = "px-3 py-2 whitespace-nowrap";
const NUMBER_CELL = `${CELL} text-right tabular-nums`;

export function ImageAdminTable() {
    const {
        data,
        error,
        fetchNextPage,
        hasNextPage,
        isPending,
        isFetching,
        isFetchNextPageError,
        refetch,
    } = useAdminImages();
    const scrollRef = useRef<HTMLDivElement | null>(null);

    const images = data ? data.pages.flatMap((page) => page.content) : [];

    const virtualizer = useVirtualizer({
        count: images.length,
        getScrollElement: () => scrollRef.current,
        estimateSize: () => ROW_HEIGHT,
        overscan: 10,
    });
    const virtualRows = virtualizer.getVirtualItems();
    const paddingTop = virtualRows[0]?.start ?? 0;
    const paddingBottom =
        virtualizer.getTotalSize() - (virtualRows.at(-1)?.end ?? 0);

    useEffect(() => {
        if (hasNextPage && !isFetching && !error) {
            fetchNextPage();
        }
    }, [hasNextPage, isFetching, error, fetchNextPage]);

    function statusMessage(): string | null {
        if (error) {
            return null;
        }

        if (isPending) {
            return "불러오는 중입니다...";
        }

        if (images.length === 0) {
            return "영상이 없습니다.";
        }

        return hasNextPage
            ? `${images.length}건 불러오는 중...`
            : `총 ${images.length}건`;
    }

    function retry() {
        if (isFetchNextPageError) {
            fetchNextPage();
        } else {
            refetch();
        }
    }

    const status = statusMessage();

    return (
        <div className="flex min-h-0 flex-1 flex-col gap-3">
            <ErrorMessage error={error} onRetry={retry} />
            <p
                hidden={status === null}
                className="text-sm text-muted-foreground"
            >
                {status}
            </p>
            <div
                ref={scrollRef}
                className="min-h-0 flex-1 overflow-auto rounded-lg border"
            >
                <table className="w-full text-sm">
                    <thead className="sticky top-0 bg-muted">
                        <tr>
                            {COLUMNS.map((column) => (
                                <th
                                    key={column}
                                    className={`${CELL} text-left font-medium`}
                                >
                                    {column}
                                </th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        <tr aria-hidden style={{ height: paddingTop }} />
                        {virtualRows.map((row) => (
                            <ImageAdminRow
                                key={images[row.index].imageId}
                                image={images[row.index]}
                            />
                        ))}
                        <tr aria-hidden style={{ height: paddingBottom }} />
                    </tbody>
                </table>
            </div>
        </div>
    );
}

type ImageAdminRowProps = {
    image: ImageResponse;
};

function ImageAdminRow({ image }: ImageAdminRowProps) {
    return (
        <tr className="border-b">
            <td className={CELL}>
                <span className="block max-w-60 truncate" title={image.title}>
                    {image.title}
                </span>
            </td>
            <td className={CELL}>{image.satellite}</td>
            <td className={CELL}>
                {image.instruments ? image.instruments.join(", ") : "-"}
            </td>
            <td className={CELL}>{sensorTypeText(image)}</td>
            <td className={CELL}>{acquiredAtText(image)}</td>
            <td className={NUMBER_CELL}>{gsdText(image)}</td>
            <td className={NUMBER_CELL}>{cloudCoverText(image)}</td>
            <td className={CELL}>{orbitDirectionText(image)}</td>
            <td className={CELL}>{image.provider.name}</td>
            <td className={CELL}>{image.provider.isPaid ? "유료" : "무료"}</td>
        </tr>
    );
}
