"use client";

import { useEffect, useRef } from "react";

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
        isFetchingNextPage,
        isFetchNextPageError,
        refetch,
    } = useImageSearch(searchId, conditions);
    const sentinelRef = useRef<HTMLDivElement | null>(null);

    const images = data ? data.pages.flatMap((page) => page.content) : [];

    useEffect(() => {
        const sentinel = sentinelRef.current;
        if (!sentinel || !hasNextPage || isFetchingNextPage || error) {
            return;
        }

        const observer = new IntersectionObserver((entries) => {
            if (entries[0].isIntersecting) {
                fetchNextPage();
            }
        });

        observer.observe(sentinel);

        return () => observer.disconnect();
    }, [hasNextPage, isFetchingNextPage, error, fetchNextPage]);

    function statusMessage(): string | null {
        if (error) {
            return null;
        }

        if (isPending) {
            return "검색 중입니다...";
        }

        if (isFetchingNextPage) {
            return "더 불러오는 중입니다...";
        }

        if (images.length === 0) {
            return "검색 결과가 없습니다.";
        }

        return hasNextPage ? null : "마지막 결과입니다.";
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
        <div className="flex flex-col gap-3">
            <ul className="flex flex-col gap-3">
                {images.map((image) => (
                    <ImageCard
                        key={image.imageId}
                        image={image}
                        selected={image.imageId === selectedImageId}
                        onSelect={() => onSelect(image)}
                    />
                ))}
            </ul>
            <div ref={sentinelRef} className="h-px" />
            <ErrorMessage error={error} onRetry={retry} />
            <p
                hidden={status === null}
                className="text-center text-sm text-muted-foreground"
            >
                {status}
            </p>
        </div>
    );
}
