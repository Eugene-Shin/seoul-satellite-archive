"use client";

import Feature from "ol/Feature";
import MultiPolygon from "ol/geom/MultiPolygon";
import TileLayer from "ol/layer/Tile";
import VectorLayer from "ol/layer/Vector";
import OlMap from "ol/Map";
import VectorSource from "ol/source/Vector";
import XYZ from "ol/source/XYZ";
import View from "ol/View";
import { type RefObject, useEffect, useRef, useState } from "react";

import { parseAoi } from "@/features/collect/collectTargetFormValues";
import type { CollectTargetRequest } from "@/features/collect/types";
import { createSelectedStyle } from "@/features/image/map/mapStyles";
import { Popover, PopoverContent } from "@/shared/components/ui/popover";

const VWORLD_KEY = process.env.NEXT_PUBLIC_VWORLD_KEY;
const FIT_PADDING = [16, 16, 16, 16];

// 미리보기는 일반 지도 한 장만 쓴다. 타일 주소 규칙은 검색 지도(ImageMap)와 같다
function createBaseLayer(): TileLayer | null {
    if (!VWORLD_KEY) {
        return null;
    }
    return new TileLayer({
        source: new XYZ({
            url: `https://api.vworld.kr/req/wmts/1.0.0/${VWORLD_KEY}/Base/{z}/{y}/{x}.png`,
            minZoom: 6,
            maxZoom: 19,
        }),
    });
}

type AoiPreviewPopoverProps = {
    // 영역 입력칸. 커서가 이 칸에 있는 동안 오른쪽에 띄운다
    anchor: RefObject<HTMLElement | null>;
    open: boolean;
    aoiText: string;
    // 기본 영역(행정구역 경계)을 그릴 때만 출처를 표시한다
    source: string | null;
};

export function AoiPreviewPopover({
    anchor,
    open,
    aoiText,
    source,
}: AoiPreviewPopoverProps) {
    return (
        <Popover open={open}>
            <PopoverContent
                anchor={anchor}
                side="right"
                sideOffset={12}
                initialFocus={false}
                finalFocus={false}
                className="w-80"
            >
                <AoiPreview aoiText={aoiText} source={source} />
            </PopoverContent>
        </Popover>
    );
}

function AoiPreview({
    aoiText,
    source,
}: Omit<AoiPreviewPopoverProps, "anchor" | "open">) {
    const aoi = parseAoi(aoiText);
    if (!aoi) {
        return (
            <p className="text-xs text-destructive">
                GeoJSON MultiPolygon으로 읽을 수 없습니다.
            </p>
        );
    }
    return (
        <>
            <AoiMap aoi={aoi} />
            <SourceText source={source} />
        </>
    );
}

function SourceText({ source }: { source: string | null }) {
    if (source === null) {
        return null;
    }
    return <p className="text-xs text-muted-foreground">{source}</p>;
}

// 팝업이 열릴 때 만들고 닫히면 없앤다
function AoiMap({ aoi }: { aoi: CollectTargetRequest["aoi"] }) {
    const containerRef = useRef<HTMLDivElement>(null);
    const mapRef = useRef<OlMap | null>(null);
    const [source] = useState(() => new VectorSource());

    useEffect(() => {
        const baseLayer = createBaseLayer();
        const aoiLayer = new VectorLayer({
            source,
            style: createSelectedStyle(),
        });
        const map = new OlMap({
            target: containerRef.current ?? undefined,
            layers: baseLayer ? [baseLayer, aoiLayer] : [aoiLayer],
            controls: [],
            view: new View(),
        });
        mapRef.current = map;
        return () => {
            map.setTarget(undefined);
            mapRef.current = null;
        };
    }, [source]);

    // 입력이 바뀔 때마다 도형을 다시 그리고 화면을 맞춘다
    useEffect(() => {
        const geometry = new MultiPolygon(aoi.coordinates).transform(
            "EPSG:4326",
            "EPSG:3857",
        );
        source.clear();
        source.addFeature(new Feature(geometry));
        mapRef.current
            ?.getView()
            .fit(geometry.getExtent(), { padding: FIT_PADDING });
    }, [aoi, source]);

    return (
        <div
            ref={containerRef}
            className="h-56 w-full overflow-hidden rounded-md border"
        />
    );
}
