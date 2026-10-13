"use client";

import { useEffect, useState } from "react";

import { fetchFilterOptions } from "@/features/image/api";
import { ImageResults } from "@/features/image/components/ImageResults";
import { ImageSearchForm } from "@/features/image/components/ImageSearchForm";
import type {
    ImageFilterOptionsResponse,
    ImageSearchParams,
} from "@/features/image/types";

type PanelTab = "conditions" | "results";

function tabClassName(selected: boolean): string {
    return selected
        ? "flex-1 border-b-2 border-foreground py-3 font-semibold"
        : "flex-1 border-b-2 border-transparent py-3 text-zinc-500";
}

export function ImageSearch() {
    const [options, setOptions] = useState<ImageFilterOptionsResponse | null>(
        null,
    );
    const [error, setError] = useState<string | null>(null);
    const [conditions, setConditions] = useState<ImageSearchParams>({});
    const [search, setSearch] = useState<{
        id: number;
        conditions: ImageSearchParams;
    }>({ id: 0, conditions: {} });
    const [activeTab, setActiveTab] = useState<PanelTab>("results");

    function updateConditions(changes: Partial<ImageSearchParams>) {
        setConditions((prev) => ({ ...prev, ...changes }));
    }

    function runSearch() {
        setSearch((prev) => ({ id: prev.id + 1, conditions }));
        setActiveTab("results");
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
        <div className="grid min-h-0 flex-1 grid-cols-[28rem_1fr]">
            <aside className="flex min-h-0 flex-col border-r">
                <div role="tablist" className="flex shrink-0 border-b">
                    <button
                        type="button"
                        role="tab"
                        aria-selected={activeTab === "conditions"}
                        onClick={() => setActiveTab("conditions")}
                        className={tabClassName(activeTab === "conditions")}
                    >
                        검색 조건
                    </button>
                    <button
                        type="button"
                        role="tab"
                        aria-selected={activeTab === "results"}
                        onClick={() => setActiveTab("results")}
                        className={tabClassName(activeTab === "results")}
                    >
                        검색 결과
                    </button>
                </div>

                <section
                    role="tabpanel"
                    hidden={activeTab !== "conditions"}
                    className="min-h-0 flex-1 overflow-y-auto p-4"
                >
                    <ImageSearchForm
                        options={options}
                        conditions={conditions}
                        onChange={updateConditions}
                        onSubmit={runSearch}
                    />
                </section>

                <section
                    role="tabpanel"
                    hidden={activeTab !== "results"}
                    className="min-h-0 flex-1 overflow-y-auto p-4"
                >
                    <ImageResults
                        key={search.id}
                        conditions={search.conditions}
                    />
                </section>
            </aside>

            <section className="flex min-h-0 items-center justify-center text-zinc-500">
                지도 영역
            </section>
        </div>
    );
}
