"use client";

import { useVirtualizer } from "@tanstack/react-virtual";
import { useEffect, useEffectEvent, useRef } from "react";

import { ImageCard } from "@/features/image/components/ImageCard";
import { useImageSearch } from "@/features/image/queries";
import type { ImageResponse, ImageSearchParams } from "@/features/image/types";
import { ErrorMessage } from "@/shared/components/ErrorMessage";

type ImageResultsProps = {
    searchId: number;
    conditions: ImageSearchParams;
    selectedImageId: number | null;
    onSelect: (image: ImageResponse) => void;
};

const CARD_HEIGHT_ESTIMATE = 250;
const CARD_GAP = 12;

export function ImageResults({
    searchId,
    conditions,
    selectedImageId,
    onSelect,
}: ImageResultsProps) {
    const {
        data,
        error,
        fetchNextPage,
        hasNextPage,
        isPending,
        isFetching,
        isFetchNextPageError,
        refetch,
    } = useImageSearch(searchId, conditions);
    const scrollRef = useRef<HTMLDivElement | null>(null);

    const images = data ? data.pages.flatMap((page) => page.content) : [];

    const virtualizer = useVirtualizer({
        count: images.length,
        getScrollElement: () => scrollRef.current,
        estimateSize: () => CARD_HEIGHT_ESTIMATE,
        gap: CARD_GAP,
    });

    const scrollToImage = useEffectEvent((imageId: number | null) => {
        const index = images.findIndex((image) => image.imageId === imageId);
        if (index !== -1) {
            virtualizer.scrollToIndex(index);
        }
    });

    useEffect(() => {
        scrollToImage(selectedImageId);
    }, [selectedImageId]);

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
            return "검색 중입니다...";
        }

        if (images.length === 0) {
            return "검색 결과가 없습니다.";
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
        <div className="flex min-h-0 flex-1 flex-col">
            <div className="flex shrink-0 flex-col gap-3 p-4">
                <ErrorMessage error={error} onRetry={retry} />
                <p
                    hidden={status === null}
                    className="text-sm text-muted-foreground"
                >
                    {status}
                </p>
            </div>
            <div
                ref={scrollRef}
                className="min-h-0 flex-1 overflow-y-auto px-4 pb-4"
            >
                <ul
                    className="relative"
                    style={{ height: virtualizer.getTotalSize() }}
                >
                    {virtualizer.getVirtualItems().map((item) => {
                        const image = images[item.index];
                        return (
                            <li
                                key={image.imageId}
                                ref={virtualizer.measureElement}
                                data-index={item.index}
                                className="absolute top-0 left-0 w-full"
                                style={{
                                    transform: `translateY(${item.start}px)`,
                                }}
                            >
                                <ImageCard
                                    image={image}
                                    selected={image.imageId === selectedImageId}
                                    onSelect={() => onSelect(image)}
                                />
                            </li>
                        );
                    })}
                </ul>
            </div>
        </div>
    );
}
