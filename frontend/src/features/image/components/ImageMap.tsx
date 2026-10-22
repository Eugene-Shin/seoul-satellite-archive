"use client";

import "ol/ol.css";

import { asArray } from "ol/color";
import { type Extent, intersects } from "ol/extent";
import Feature from "ol/Feature";
import type MultiPolygon from "ol/geom/MultiPolygon";
import { fromExtent } from "ol/geom/Polygon";
import DragBox from "ol/interaction/DragBox";
import TileLayer from "ol/layer/Tile";
import VectorLayer from "ol/layer/Vector";
import OlMap from "ol/Map";
import type MapBrowserEvent from "ol/MapBrowserEvent";
import { unByKey } from "ol/Observable";
import { fromLonLat, transformExtent } from "ol/proj";
import VectorSource from "ol/source/Vector";
import XYZ from "ol/source/XYZ";
import { Fill, Stroke, Style } from "ol/style";
import View from "ol/View";
import { useEffect, useEffectEvent, useRef, useState } from "react";

import { bboxToExtent, extentToBbox } from "@/features/image/bbox";
import { AreaToolbar } from "@/features/image/components/AreaToolbar";
import { FootprintChooser } from "@/features/image/components/FootprintChooser";
import { largerFirst, pickImages, toFeature } from "@/features/image/footprint";
import type { Bbox, ImageResponse } from "@/features/image/types";

type ImageMapProps = {
    images: ImageResponse[];
    selectedImage: ImageResponse | null;
    area: Bbox | undefined;
    areaApplied: boolean;
    allLoaded: boolean;
    drawing: boolean;
    onSelect: (image: ImageResponse) => void;
    onAreaChange: (area: Bbox | undefined) => void;
    onDrawingStart: () => void;
    onDrawingStop: () => void;
};

type Chooser = {
    imageIds: number[];
    x: number;
    y: number;
};

type VworldLayer = "Base" | "midnight";

type VectorStyleOptions = {
    colorVariable: string;
    width: number;
    fillOpacity: number;
    lineDash?: number[];
};

const SEOUL_LON_LAT = [126.978, 37.566];
const INITIAL_ZOOM = 11;
const VWORLD_KEY = process.env.NEXT_PUBLIC_VWORLD_KEY;
const VWORLD_MIN_ZOOM = 6;
const VWORLD_MAX_ZOOM: Record<VworldLayer, number> = {
    Base: 19,
    midnight: 18,
};
const VWORLD_EXTENT = transformExtent(
    [111.1, 25, 145, 47],
    "EPSG:4326",
    "EPSG:3857",
);
const FIT_PADDING = 32;
const FIT_MAX_ZOOM = 14;
const FIT_DURATION = 300;

const FOOTPRINT_STYLE_OPTIONS: VectorStyleOptions = {
    colorVariable: "--map-footprint",
    width: 1.5,
    fillOpacity: 0.04,
};

const SELECTED_STYLE_OPTIONS: VectorStyleOptions = {
    colorVariable: "--map-selected",
    width: 3,
    fillOpacity: 0.15,
};

const AREA_LINE_DASH = [8, 6];

const AREA_STYLE_OPTIONS: VectorStyleOptions = {
    colorVariable: "--map-area",
    width: 2,
    fillOpacity: 0.06,
    lineDash: AREA_LINE_DASH,
};

const UNAPPLIED_AREA_STYLE_OPTIONS: VectorStyleOptions = {
    colorVariable: "--map-area-unapplied",
    width: 2,
    fillOpacity: 0,
    lineDash: AREA_LINE_DASH,
};

function createVworldLayer(layer: VworldLayer): TileLayer {
    return new TileLayer({
        source: new XYZ({
            url: `https://api.vworld.kr/req/wmts/1.0.0/${VWORLD_KEY}/${layer}/{z}/{y}/{x}.png`,
            maxZoom: VWORLD_MAX_ZOOM[layer],
        }),
    });
}

function readCssVariable(name: string): string {
    return getComputedStyle(document.documentElement)
        .getPropertyValue(name)
        .trim();
}

function createVectorStyle({
    colorVariable,
    width,
    fillOpacity,
    lineDash,
}: VectorStyleOptions): Style {
    const color = readCssVariable(colorVariable);
    const [red, green, blue] = asArray(color);

    return new Style({
        stroke: new Stroke({ color, width, lineDash }),
        fill: new Fill({ color: [red, green, blue, fillOpacity] }),
    });
}

function isOutOfView(map: OlMap, geometry: MultiPolygon): boolean {
    const size = map.getSize();
    if (!size) {
        return false;
    }
    return !intersects(
        map.getView().calculateExtent(size),
        geometry.getExtent(),
    );
}

function fitView(map: OlMap, extent: Extent) {
    const reduceMotion = window.matchMedia(
        "(prefers-reduced-motion: reduce)",
    ).matches;
    map.getView().fit(extent, {
        padding: [FIT_PADDING, FIT_PADDING, FIT_PADDING, FIT_PADDING],
        maxZoom: FIT_MAX_ZOOM,
        duration: reduceMotion ? 0 : FIT_DURATION,
    });
}

function revealIfOutOfView(map: OlMap, image: ImageResponse) {
    const geometry = toFeature(image).getGeometry();
    if (geometry && isOutOfView(map, geometry)) {
        fitView(map, geometry.getExtent());
    }
}

export function ImageMap({
    images,
    selectedImage,
    area,
    areaApplied,
    allLoaded,
    drawing,
    onSelect,
    onAreaChange,
    onDrawingStart,
    onDrawingStop,
}: ImageMapProps) {
    const containerRef = useRef<HTMLDivElement | null>(null);
    const mapRef = useRef<OlMap | null>(null);
    const dragBoxRef = useRef<DragBox | null>(null);
    const [footprintSource] = useState(() => new VectorSource());
    const [selectionSource] = useState(() => new VectorSource());
    const [areaSource] = useState(() => new VectorSource());
    const [chooser, setChooser] = useState<Chooser | null>(null);
    const [chooserOpen, setChooserOpen] = useState(false);

    const chooserImages = chooser ? pickImages(images, chooser.imageIds) : [];

    const handleMapClick = useEffectEvent((event: MapBrowserEvent) => {
        const hitIds = event.map
            .getFeaturesAtPixel(event.pixel, {
                layerFilter: (layer) => layer.getSource() === footprintSource,
            })
            .map((feature) => feature.getId());
        const hits = pickImages(images, hitIds);

        if (hits.length > 1 && event.originalEvent instanceof MouseEvent) {
            setChooser({
                imageIds: hits.map((image) => image.imageId),
                x: event.originalEvent.clientX,
                y: event.originalEvent.clientY,
            });
            setChooserOpen(true);
            return;
        }

        setChooserOpen(false);
        if (hits.length === 1) {
            onSelect(hits[0]);
        }
    });

    const cancelDrawing = useEffectEvent(() => onDrawingStop());

    const handleBoxEnd = useEffectEvent((extent: Extent) => {
        onDrawingStop();
        const bbox = extentToBbox(extent);
        if (bbox) {
            onAreaChange(bbox);
        }
    });

    useEffect(() => {
        const lightBaseLayer = VWORLD_KEY ? createVworldLayer("Base") : null;
        const darkBaseLayer = VWORLD_KEY ? createVworldLayer("midnight") : null;
        const baseLayers =
            lightBaseLayer && darkBaseLayer
                ? [lightBaseLayer, darkBaseLayer]
                : [];
        const footprintLayer = new VectorLayer({
            source: footprintSource,
            renderOrder: largerFirst,
        });
        const areaLayer = new VectorLayer({ source: areaSource });
        const selectionLayer = new VectorLayer({ source: selectionSource });

        const map = new OlMap({
            target: containerRef.current ?? undefined,
            layers: [...baseLayers, footprintLayer, areaLayer, selectionLayer],
            view: new View({
                center: fromLonLat(SEOUL_LON_LAT),
                zoom: INITIAL_ZOOM,
                minZoom: VWORLD_MIN_ZOOM,
                extent: VWORLD_EXTENT,
                smoothExtentConstraint: false,
            }),
        });
        mapRef.current = map;

        const darkMode = window.matchMedia("(prefers-color-scheme: dark)");

        function applyTheme() {
            lightBaseLayer?.setVisible(!darkMode.matches);
            darkBaseLayer?.setVisible(darkMode.matches);
            footprintLayer.setStyle(createVectorStyle(FOOTPRINT_STYLE_OPTIONS));
            const areaStyle = createVectorStyle(AREA_STYLE_OPTIONS);
            const unappliedAreaStyle = createVectorStyle(
                UNAPPLIED_AREA_STYLE_OPTIONS,
            );
            areaLayer.setStyle((feature) =>
                feature.get("applied") ? areaStyle : unappliedAreaStyle,
            );
            selectionLayer.setStyle(createVectorStyle(SELECTED_STYLE_OPTIONS));
        }

        applyTheme();
        darkMode.addEventListener("change", applyTheme);

        const dragBox = new DragBox();
        dragBox.setActive(false);
        dragBox.on("boxend", () =>
            handleBoxEnd(dragBox.getGeometry().getExtent()),
        );
        map.addInteraction(dragBox);
        dragBoxRef.current = dragBox;

        map.on("movestart", () => setChooserOpen(false));

        return () => {
            darkMode.removeEventListener("change", applyTheme);
            map.dispose();
            mapRef.current = null;
            dragBoxRef.current = null;
        };
    }, [footprintSource, selectionSource, areaSource]);

    useEffect(() => {
        dragBoxRef.current?.setActive(drawing);
    }, [drawing]);

    useEffect(() => {
        if (!drawing) {
            return;
        }
        function cancelOnEscape(event: KeyboardEvent) {
            if (event.key === "Escape") {
                cancelDrawing();
            }
        }
        document.addEventListener("keydown", cancelOnEscape);
        return () => document.removeEventListener("keydown", cancelOnEscape);
    }, [drawing]);

    useEffect(() => {
        const map = mapRef.current;
        if (!map || drawing) {
            return;
        }
        const clickKey = map.on("singleclick", (event) =>
            handleMapClick(event),
        );
        return () => unByKey(clickKey);
    }, [drawing]);

    useEffect(() => {
        footprintSource.addFeatures(images.map(toFeature));
        return () => footprintSource.clear();
    }, [footprintSource, images]);

    // 마지막 페이지가 오면 images와 allLoaded가 같이 바뀌므로, footprint를 넣는 effect보다 아래에 둬야 채워진 범위로 맞춘다
    useEffect(() => {
        const map = mapRef.current;
        const extent = footprintSource.getExtent();
        if (!map || !allLoaded || !extent || footprintSource.isEmpty()) {
            return;
        }
        fitView(map, extent);
    }, [allLoaded, footprintSource]);

    useEffect(() => {
        if (!selectedImage) {
            return;
        }
        selectionSource.addFeature(toFeature(selectedImage));
        return () => selectionSource.clear();
    }, [selectionSource, selectedImage]);

    useEffect(() => {
        if (!area) {
            return;
        }
        areaSource.addFeature(
            new Feature({
                geometry: fromExtent(bboxToExtent(area)),
                applied: areaApplied,
            }),
        );
        return () => areaSource.clear();
    }, [areaSource, area, areaApplied]);

    useEffect(() => {
        const map = mapRef.current;
        if (map && selectedImage) {
            revealIfOutOfView(map, selectedImage);
        }
    }, [selectedImage]);

    function selectCurrentView() {
        const map = mapRef.current;
        const size = map?.getSize();
        if (!map || !size) {
            return;
        }
        const bbox = extentToBbox(map.getView().calculateExtent(size));
        if (bbox) {
            onAreaChange(bbox);
        }
    }

    function cancelArea() {
        if (drawing) {
            onDrawingStop();
        } else {
            onAreaChange(undefined);
        }
    }

    return (
        <>
            <div ref={containerRef} className="absolute inset-0" />
            <div className="absolute top-2 right-2 flex gap-2">
                <AreaToolbar
                    drawing={drawing}
                    hasArea={area !== undefined}
                    onUseCurrentView={selectCurrentView}
                    onStartDrawing={onDrawingStart}
                    onCancel={cancelArea}
                />
            </div>
            {chooser && (
                <FootprintChooser
                    open={chooserOpen && chooserImages.length > 0}
                    x={chooser.x}
                    y={chooser.y}
                    images={chooserImages}
                    onOpenChange={setChooserOpen}
                    onOpenChangeComplete={(open) => {
                        if (!open) {
                            setChooser(null);
                        }
                    }}
                    onSelect={(image) => {
                        setChooserOpen(false);
                        onSelect(image);
                    }}
                />
            )}
        </>
    );
}
