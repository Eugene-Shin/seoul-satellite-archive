"use client";

import { useEffect, useRef, useState } from "react";

import { searchImages } from "@/features/image/api";
import { ImageCard } from "@/features/image/components/ImageCard";
import type { ImageResponse, ImageSearchParams } from "@/features/image/types";

const PAGE_SIZE = 20;

type ImageResultsProps = {
    conditions: ImageSearchParams;
};

export function ImageResults({ conditions }: ImageResultsProps) {
    const [images, setImages] = useState<ImageResponse[]>([]);
    const [page, setPage] = useState(0);
    const [loadedPages, setLoadedPages] = useState(0);
    const [hasNext, setHasNext] = useState(true);
    const [error, setError] = useState<string | null>(null);
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
                    setError(e instanceof Error ? e.message : String(e));
                }
            });

        return () => {
            ignore = true;
        };
    }, [conditions, page]);

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
            return `검색 중 오류가 발생했습니다: ${error}`;
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

    return (
        <div className="flex flex-col gap-3">
            <ul className="flex flex-col gap-3">
                {images.map((image) => (
                    <ImageCard key={image.imageId} image={image} />
                ))}
            </ul>
            <div ref={sentinelRef} className="h-px" />
            <p className="text-center text-sm text-zinc-500">
                {statusMessage()}
            </p>
        </div>
    );
}
