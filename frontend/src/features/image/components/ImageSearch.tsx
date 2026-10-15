"use client";

import { useState } from "react";

import { ImageDetail } from "@/features/image/components/ImageDetail";
import { ImageResults } from "@/features/image/components/ImageResults";
import { ImageSearchForm } from "@/features/image/components/ImageSearchForm";
import { useFilterOptions } from "@/features/image/queries";
import { useAppliedConditions } from "@/features/image/searchParams";
import type { ImageResponse, ImageSearchParams } from "@/features/image/types";
import { ErrorMessage } from "@/shared/components/ErrorMessage";
import {
    Tabs,
    TabsContent,
    TabsList,
    TabsTrigger,
} from "@/shared/components/ui/tabs";

type PanelTab = "conditions" | "results";

export function ImageSearch() {
    const { data: options, error, refetch } = useFilterOptions();
    const [appliedConditions, setAppliedConditions] = useAppliedConditions();
    const [conditions, setConditions] =
        useState<ImageSearchParams>(appliedConditions);
    const [searchId, setSearchId] = useState(0);
    const [activeTab, setActiveTab] = useState<PanelTab>("results");
    const [selectedImage, setSelectedImage] = useState<ImageResponse | null>(
        null,
    );

    function updateConditions(changes: Partial<ImageSearchParams>) {
        setConditions((prev) => ({ ...prev, ...changes }));
    }

    function runSearch() {
        setAppliedConditions(conditions);
        setSearchId((prev) => prev + 1);
        setActiveTab("results");
        setSelectedImage(null);
    }

    if (error) {
        return (
            <div className="p-6">
                <ErrorMessage error={error} onRetry={() => refetch()} />
            </div>
        );
    }

    if (!options) {
        return <p>검색 조건을 불러오는 중입니다...</p>;
    }

    if (options.satellites.length === 0) {
        return <p>등록된 영상이 없습니다.</p>;
    }

    return (
        <div className="grid min-h-0 flex-1 grid-cols-[var(--spacing-panel)_1fr]">
            <aside className="flex min-h-0 flex-col border-r">
                <Tabs
                    value={activeTab}
                    onValueChange={setActiveTab}
                    className="min-h-0 flex-1 gap-0"
                >
                    <TabsList
                        variant="line"
                        className="w-full shrink-0 border-b"
                    >
                        <TabsTrigger value="conditions" className="py-3">
                            검색 조건
                        </TabsTrigger>
                        <TabsTrigger value="results" className="py-3">
                            검색 결과
                        </TabsTrigger>
                    </TabsList>

                    <TabsContent
                        value="conditions"
                        keepMounted
                        className="min-h-0 overflow-y-auto p-4"
                    >
                        <ImageSearchForm
                            options={options}
                            conditions={conditions}
                            onChange={updateConditions}
                            onSubmit={runSearch}
                        />
                    </TabsContent>

                    <TabsContent
                        value="results"
                        keepMounted
                        className="min-h-0 overflow-y-auto p-4"
                    >
                        <ImageResults
                            searchId={searchId}
                            conditions={appliedConditions}
                            selectedImageId={
                                selectedImage ? selectedImage.imageId : null
                            }
                            onSelect={setSelectedImage}
                        />
                    </TabsContent>
                </Tabs>
            </aside>

            <section className="relative flex min-h-0 items-center justify-center text-zinc-500">
                지도 영역
                <ImageDetail
                    key={selectedImage ? selectedImage.imageId : undefined}
                    image={selectedImage}
                    onClose={() => setSelectedImage(null)}
                />
            </section>
        </div>
    );
}
