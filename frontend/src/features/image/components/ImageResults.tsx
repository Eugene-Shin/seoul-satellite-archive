import { useEffect, useState } from "react";

import { searchImages } from "@/features/image/api";
import { ImageCard } from "@/features/image/components/ImageCard";
import type { ImageResponse, ImageSearchParams } from "@/features/image/types";
import type { SliceResponse } from "@/shared/api/types";

type ImageResultsProps = {
    conditions: ImageSearchParams;
};

export function ImageResults({ conditions }: ImageResultsProps) {
    const [result, setResult] = useState<SliceResponse<ImageResponse> | null>(
        null,
    );
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        let ignore = false;

        searchImages(conditions)
            .then((data) => {
                if (!ignore) {
                    setResult(data);
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
    }, [conditions]);

    if (error) {
        return (
            <p className="text-red-600">검색 중 오류가 발생했습니다: {error}</p>
        );
    }

    if (!result) {
        return <p>검색중입니다...</p>;
    }

    if (result.content.length === 0) {
        return <p>검색 결과가 없습니다.</p>;
    }

    return (
        <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {result.content.map((image) => (
                <ImageCard key={image.imageId} image={image} />
            ))}
        </ul>
    );
}
