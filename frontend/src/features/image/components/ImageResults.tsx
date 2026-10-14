"use client";

import { useEffect, useRef, useState } from "react";

import { searchImages } from "@/features/image/api";
import { ImageCard } from "@/features/image/components/ImageCard";
import type { ImageResponse, ImageSearchParams } from "@/features/image/types";
import { ErrorMessage } from "@/shared/components/ErrorMessage";

const PAGE_SIZE = 20;

type ImageResultsProps = {
    conditions: ImageSearchParams;
    selectedImageId: number | null;
    onSelect: (image: ImageResponse) => void;
};

export function ImageResults({
    conditions,
    selectedImageId,
    onSelect,
}: ImageResultsProps) {
    const [images, setImages] = useState<ImageResponse[]>([]);
    const [page, setPage] = useState(0);
    const [loadedPages, setLoadedPages] = useState(0);
    const [hasNext, setHasNext] = useState(true);
    const [error, setError] = useState<unknown>(null);
    const [attempt, setAttempt] = useState(0);
    const sentinelRef = useRef<HTMLDivElement | null>(null);

    const loading = loadedPages <= page;

    useEffect(() => {
        let ignore = false;

        searchImages({ ...conditions, page, size: PAGE_SIZE })
            .then((data) => {
                if (!ignore) {
                    setImages((prev) => [...prev, ...data.content]);
                    setHasNext(data.hasNext);
                    setLoadedPages((prev) => prev + 1);
                }
            })
            .catch((e) => {
                if (!ignore) {
                    setError(e);
                }
            });

        return () => {
            ignore = true;
        };
    }, [conditions, page, attempt]);

    useEffect(() => {
        const sentinel = sentinelRef.current;
        if (!sentinel || loading || !hasNext || error) {
            return;
        }

        const observer = new IntersectionObserver((entries) => {
            if (entries[0].isIntersecting) {
                setPage((prev) => prev + 1);
            }
        });

        observer.observe(sentinel);

        return () => observer.disconnect();
    }, [loading, hasNext, error]);

    function statusMessage(): string | null {
        if (error) {
            return null;
        }

        if (loading) {
            return images.length === 0
                ? "검색 중입니다..."
                : "더 불러오는 중입니다...";
        }

        if (images.length === 0) {
            return "검색 결과가 없습니다.";
        }

        return hasNext ? null : "마지막 결과입니다.";
    }

    function retry() {
        setError(null);
        setAttempt((prev) => prev + 1);
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
                className="text-center text-sm text-zinc-500"
            >
                {status}
            </p>
        </div>
    );
}
