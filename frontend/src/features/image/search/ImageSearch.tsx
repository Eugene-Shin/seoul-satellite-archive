"use client";

import { SatelliteIcon, Undo2Icon } from "lucide-react";
import dynamic from "next/dynamic";
import { useMemo, useState } from "react";

import { pickImages } from "@/features/image/map/footprint";
import {
    useCollections,
    useFilterOptions,
    useImageSearch,
} from "@/features/image/queries";
import { ImageDetail } from "@/features/image/search/ImageDetail";
import { ImageResults } from "@/features/image/search/ImageResults";
import { ImageSearchForm } from "@/features/image/search/ImageSearchForm";
import { useCurrentSearch } from "@/features/image/search/searchParams";
import { useSearchScopedState } from "@/features/image/search/useSearchScopedState";
import type {
    Bbox,
    ImageResponse,
    ImageSearchParams,
    ImageSelection,
    MapTool,
} from "@/features/image/types";
import { ErrorMessage } from "@/shared/components/ErrorMessage";
import { IconButton } from "@/shared/components/IconButton";
import { Button } from "@/shared/components/ui/button";
const ImageMap = dynamic(
    () =>
        import("@/features/image/map/ImageMap").then(
            (module) => module.ImageMap,
        ),
    { ssr: false },
);

type PanelTab = "map" | "results";

type Hover = {
    imageIds: number[];
    // 지도(footprint·겹침 목록)에서 온 hover만 결과 목록을 스크롤한다
    fromMap: boolean;
};

export function ImageSearch() {
    const { data: options, error, refetch } = useFilterOptions();
    const collectionsQuery = useCollections();
    const {
        conditions: appliedConditions,
        id: searchId,
        key: currentSearchKey,
        searchWith,
    } = useCurrentSearch();
    const [activeTab, setActiveTab] = useState<PanelTab>("results");
    const [selection, setSelection] = useState<ImageSelection | null>(null);
    // 지도 도구와 적용 전 편집은 시작한 검색에서만 유효하다
    const [activeTool, startTool, stopTool] =
        useSearchScopedState<MapTool>(currentSearchKey);
    const [editedConditions, setEditedConditions, discardEdit] =
        useSearchScopedState<ImageSearchParams>(currentSearchKey);
    const [hover, setHover] = useState<Hover>({
        imageIds: [],
        fromMap: false,
    });

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
        selection !== null &&
        (!allLoaded ||
            images.some((image) => image.imageId === selection.image.imageId));
    const shownSelection = selectionInResults ? selection : null;
    const shownSelectedImage = shownSelection ? shownSelection.image : null;

    // 도구를 쓰는 중엔 강조하지 않는다. 결과에 없는 id(검색이 바뀐 경우)는 pickImages가 걸러낸다
    const hoveredImages = activeTool ? [] : pickImages(images, hover.imageIds);
    // 지도에서 한 영상만 가리킬 때만 그 카드로 스크롤한다. 겹친 여러 개면 강조만
    const scrollTargetImageId =
        hover.fromMap && hoveredImages.length === 1
            ? hoveredImages[0].imageId
            : null;

    // 입력값과 영역은 검색 버튼을 눌러야 적용된다
    // 조건 탭과 지도에 보이는 조건
    const inputConditions = editedConditions ?? appliedConditions;
    // 지도의 영역이 결과에 적용된 영역인지. 적용된 영역만 색으로 표시한다
    const areaApplied =
        JSON.stringify(inputConditions.bbox) ===
        JSON.stringify(appliedConditions.bbox);

    function updateConditions(changes: Partial<ImageSearchParams>) {
        setEditedConditions({ ...inputConditions, ...changes });
    }

    function runSearch() {
        searchWith(inputConditions);
        setActiveTab("results");
        setSelection(null);
    }

    function changeArea(area: Bbox | undefined) {
        updateConditions({ bbox: area });
    }

    // 같은 영상을 다시 눌러도 켜 둔 겹치기는 유지한다
    function selectImage(image: ImageResponse) {
        setSelection((current) => ({
            image,
            move: "ifOutOfView",
            showOverlay:
                current?.image.imageId === image.imageId && current.showOverlay,
        }));
    }

    function selectFromMap(image: ImageResponse) {
        selectImage(image);
        setActiveTab("results");
    }

    function fitToImage(image: ImageResponse) {
        setSelection({ image, move: "always", showOverlay: false });
    }

    function toggleOverlay(image: ImageResponse) {
        setSelection(
            shownSelection?.showOverlay
                ? { image, move: "none", showOverlay: false }
                : { image, move: "always", showOverlay: true },
        );
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

    return (
        <div className="catalog-shell">
            <header className="catalog-search border-b bg-background">
                <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                    <div className="flex items-center gap-3">
                        <span className="rounded-lg bg-primary p-2 text-primary-foreground">
                            <SatelliteIcon className="size-5" />
                        </span>
                        <div>
                            <h1 className="text-xl font-semibold tracking-tight">
                                서울시 위성영상 카탈로그
                            </h1>
                            <p className="text-xs text-muted-foreground">
                                촬영영역과 메타데이터로 원하는 영상을
                                찾아보세요.
                            </p>
                        </div>
                    </div>
                    {editedConditions && (
                        <IconButton
                            label="검색조건 되돌리기"
                            title="검색조건 되돌리기"
                            onClick={() => discardEdit()}
                        >
                            <Undo2Icon />
                        </IconButton>
                    )}
                </div>
                <ImageSearchForm
                    options={options}
                    collections={collectionsQuery.data ?? []}
                    collectionsError={collectionsQuery.error}
                    onRetryCollections={() => void collectionsQuery.refetch()}
                    conditions={inputConditions}
                    onChange={updateConditions}
                    onSubmit={runSearch}
                />
                {editedConditions && (
                    <p
                        role="status"
                        className="mt-2 text-xs text-muted-foreground"
                    >
                        변경한 조건은 검색 버튼을 누르면 적용됩니다.
                    </p>
                )}
            </header>
            <nav
                className="catalog-mobile-switch border-b p-2"
                aria-label="탐색 화면"
            >
                <Button
                    variant={activeTab === "results" ? "default" : "outline"}
                    onClick={() => setActiveTab("results")}
                    aria-pressed={activeTab === "results"}
                >
                    검색 결과
                </Button>
                <Button
                    variant={activeTab === "map" ? "default" : "outline"}
                    onClick={() => setActiveTab("map")}
                    aria-pressed={activeTab === "map"}
                >
                    촬영영역 지도
                </Button>
            </nav>
            <div className="catalog-workspace" data-view={activeTab}>
                <aside
                    className="catalog-results flex min-h-0 min-w-0 flex-col border-r bg-muted/20"
                    aria-label="위성영상 검색 결과"
                >
                    <div className="border-b px-4 py-3">
                        <h2 className="text-sm font-semibold">검색 결과</h2>
                        <p className="mt-1 text-xs text-muted-foreground">
                            영상을 선택하면 지도와 상세정보를 함께 확인할 수
                            있습니다.
                        </p>
                    </div>
                    <ImageResults
                        searchId={searchId}
                        conditions={appliedConditions}
                        selectedImageId={
                            shownSelectedImage
                                ? shownSelectedImage.imageId
                                : null
                        }
                        hoveredImageIds={hoveredImages.map(
                            (image) => image.imageId,
                        )}
                        scrollTargetImageId={scrollTargetImageId}
                        onSelect={selectImage}
                        onHover={(imageIds) =>
                            setHover({ imageIds, fromMap: false })
                        }
                    />
                </aside>
                <section
                    className="catalog-map relative min-h-0 min-w-0 bg-muted/40"
                    aria-label="촬영영역 지도"
                >
                    <ImageMap
                        images={images}
                        selection={shownSelection}
                        hoveredImages={hoveredImages}
                        area={inputConditions.bbox}
                        areaApplied={areaApplied}
                        allLoaded={allLoaded}
                        tool={activeTool}
                        onSelect={selectFromMap}
                        onHover={(imageIds) =>
                            setHover({ imageIds, fromMap: true })
                        }
                        onAreaChange={changeArea}
                        onToolStart={startTool}
                        onToolStop={stopTool}
                    />
                </section>
                <ImageDetail
                    image={shownSelectedImage}
                    overlayShown={shownSelection?.showOverlay ?? false}
                    onPreview={fitToImage}
                    onToggleOverlay={toggleOverlay}
                    onClose={() => setSelection(null)}
                />
            </div>
        </div>
    );
}
