"use client";

import { useEffect, useState } from "react";

import { fetchFilterOptions } from "@/features/image/api";
import { ImageSearchForm } from "@/features/image/components/ImageSearchForm";
import type {
    ImageFilterOptionsResponse,
    ImageSearchParams,
} from "@/features/image/types";

export function ImageSearch() {
    const [options, setOptions] = useState<ImageFilterOptionsResponse | null>(
        null,
    );
    const [error, setError] = useState<string | null>(null);
    const [conditions, setConditions] = useState<ImageSearchParams>({});

    function updateConditions(changes: Partial<ImageSearchParams>) {
        setConditions((prev) => ({ ...prev, ...changes }));
    }

    useEffect(() => {
        let ignore = false;

        fetchFilterOptions()
            .then((data) => {
                if (!ignore) {
                    setOptions(data);
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
    }, []);

    if (error) {
        return (
            <p className="text-red-600">
                검색 조건을 불러오는 중 오류가 발생했습니다: {error}
            </p>
        );
    }

    if (!options) {
        return <p>검색 조건을 불러오는 중입니다...</p>;
    }

    if (options.satellites.length === 0) {
        return <p>등록된 영상이 없습니다.</p>;
    }

    return (
        <div className="flex flex-col gap-6">
            <ImageSearchForm
                options={options}
                conditions={conditions}
                onChange={updateConditions}
            />

            <pre className="rounded bg-zinc-100 p-4 text-sm">
                {JSON.stringify(conditions, null, 2)}
            </pre>
        </div>
    );
}
