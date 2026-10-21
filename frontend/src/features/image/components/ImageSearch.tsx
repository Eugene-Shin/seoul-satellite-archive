"use client";

import dynamic from "next/dynamic";
import { useMemo, useState } from "react";

import { ImageDetail } from "@/features/image/components/ImageDetail";
import { ImageResults } from "@/features/image/components/ImageResults";
import { ImageSearchForm } from "@/features/image/components/ImageSearchForm";
import { useFilterOptions, useImageSearch } from "@/features/image/queries";
import { useAppliedConditions } from "@/features/image/searchParams";
import type {
    Bbox,
    ImageResponse,
    ImageSearchParams,
} from "@/features/image/types";
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
    const [searchId, setSearchId] = useState(0);
    const [activeTab, setActiveTab] = useState<PanelTab>("results");
    const [selectedImage, setSelectedImage] = useState<ImageResponse | null>(
        null,
    );
    const [drawingStartedSearchKey, setDrawingStartedSearchKey] = useState<
        string | null
    >(null);

    const {
        data: searchData,
        hasNextPage,
        isFetching,
    } = useImageSearch(searchId, appliedConditions);
    const allLoaded = searchData !== undefined && !hasNextPage && !isFetching;
    const images = useMemo(
        () =>
            searchData ? searchData.pages.flatMap((page) => page.content) : [],
        [searchData],
    );

    // 선택한 영상이 지금 결과에 없으면(뒤로 가기 등으로 결과가 바뀐 경우) 카드 강조·지도 선택 표시·상세 패널에 넘기지 않는다.
    // 렌더링마다 계산하되, 받는 중엔 아직 안 온 페이지에 있을 수 있어 결과에 있는 것으로 보고 다 받은 뒤에만 실제로 찾아본다
    const selectionInResults =
        selectedImage !== null &&
        (!allLoaded ||
            images.some((image) => image.imageId === selectedImage.imageId));
    const shownSelectedImage = selectionInResults ? selectedImage : null;

    // 그리기는 시작한 검색에서만 유효하다. 검색이 어떤 경로로 바뀌든(검색 버튼, 영역 변경, 뒤로 가기) 그리기는 끝난다.
    // searchId는 우리 코드로 검색할 때, appliedConditions는 URL이 바뀔 때 달라져서 둘을 함께 쓴다
    const shownSearchKey = JSON.stringify([searchId, appliedConditions]);
    const drawing = drawingStartedSearchKey === shownSearchKey;

    function startDrawing() {
        setDrawingStartedSearchKey(shownSearchKey);
    }

    function stopDrawing() {
        setDrawingStartedSearchKey(null);
    }

    // 적용된 조건이 바뀌면(뒤로 가기 포함) 폼을 새로 만들어 입력값을 맞춘다. 영역은 폼에 없어서 영역만 바뀔 땐 입력 중인 값을 지키려 키에서 뺀다
    const formKey = JSON.stringify({ ...appliedConditions, bbox: undefined });

    function applyConditions(next: ImageSearchParams) {
        setAppliedConditions(next);
        setSearchId((prev) => prev + 1);
        setActiveTab("results");
        setSelectedImage(null);
    }

    function runSearch(conditions: ImageSearchParams) {
        applyConditions({ ...conditions, bbox: appliedConditions.bbox });
    }

    function changeArea(area: Bbox | undefined) {
        applyConditions({ ...appliedConditions, bbox: area });
    }

    function selectFromMap(image: ImageResponse) {
        setSelectedImage(image);
        setActiveTab("results");
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
                            key={formKey}
                            options={options}
                            initialConditions={appliedConditions}
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
                                shownSelectedImage
                                    ? shownSelectedImage.imageId
                                    : null
                            }
                            onSelect={setSelectedImage}
                        />
                    </TabsContent>
                </Tabs>
            </aside>

            <section className="area-map relative min-h-0 bg-muted/40">
                <ImageMap
                    images={images}
                    selectedImage={shownSelectedImage}
                    area={appliedConditions.bbox}
                    allLoaded={allLoaded}
                    drawing={drawing}
                    onSelect={selectFromMap}
                    onAreaChange={changeArea}
                    onDrawingStart={startDrawing}
                    onDrawingStop={stopDrawing}
                />
                <ImageDetail
                    image={shownSelectedImage}
                    onClose={() => setSelectedImage(null)}
                />
            </section>
        </div>
    );
}
