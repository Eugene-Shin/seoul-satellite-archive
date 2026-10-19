"use client";

import dynamic from "next/dynamic";
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

const ImageMap = dynamic(
    () =>
        import("@/features/image/components/ImageMap").then(
            (module) => module.ImageMap,
        ),
    { ssr: false },
);

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
            <div className="area-panel p-4">
                <ErrorMessage error={error} onRetry={() => refetch()} />
            </div>
        );
    }

    if (!options) {
        return (
            <p className="area-panel p-4">검색 조건을 불러오는 중입니다...</p>
        );
    }

    if (options.satellites.length === 0) {
        return <p className="area-panel p-4">등록된 영상이 없습니다.</p>;
    }

    return (
        <div className="contents">
            <aside className="area-panel flex min-h-0 flex-col border-r">
                <Tabs
                    value={activeTab}
                    onValueChange={setActiveTab}
                    className="min-h-0 flex-1 gap-0"
                >
                    <TabsList
                        variant="line"
                        className="w-full shrink-0 border-b"
                    >
                        <TabsTrigger value="conditions">검색 조건</TabsTrigger>
                        <TabsTrigger value="results">검색 결과</TabsTrigger>
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
                        className="flex min-h-0 flex-col"
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

            <section className="area-map relative min-h-0 bg-muted/40">
                <ImageMap />
                <ImageDetail
                    image={selectedImage}
                    onClose={() => setSelectedImage(null)}
                />
            </section>
        </div>
    );
}
