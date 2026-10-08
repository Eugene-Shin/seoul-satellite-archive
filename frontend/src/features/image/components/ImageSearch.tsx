"use client";

import { useEffect, useState } from "react";

import { fetchFilterOptions } from "@/features/image/api";
import type { ImageFilterOptionsResponse } from "@/features/image/types";

export function ImageSearch() {
    const [options, setOptions] = useState<ImageFilterOptionsResponse | null>(
        null,
    );
    const [error, setError] = useState<string | null>(null);

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

    return (
        <dl className="grid grid-cols-[max-content_1fr] gap-x-6 gap-y-2">
            <dt className="font-semibold">위성</dt>
            <dd>{options.satellites.join(", ")}</dd>

            <dt className="font-semibold">센서 유형</dt>
            <dd>{options.sensorTypes.join(", ")}</dd>

            <dt className="font-semibold">센서</dt>
            <dd>{options.instruments.join(", ")}</dd>

            <dt className="font-semibold">영상 제공 기관</dt>
            <dd>{options.providers.map((p) => p.name).join(", ")}</dd>

            <dt className="font-semibold">궤도 방향</dt>
            <dd>{options.orbitDirections.join(", ")}</dd>

            <dt className="font-semibold">해상도(GSD)</dt>
            <dd>
                {options.gsdRange.min} ~ {options.gsdRange.max} m
            </dd>

            <dt className="font-semibold">촬영일</dt>
            <dd>
                {options.acquiredRange.min?.slice(0, 10)} ~{" "}
                {options.acquiredRange.max?.slice(0, 10)}
            </dd>
        </dl>
    );
}
