"use client";

import "ol/ol.css";

import { asArray } from "ol/color";
import MousePosition from "ol/control/MousePosition";
import OverviewMap from "ol/control/OverviewMap";
import ScaleLine from "ol/control/ScaleLine";
import { type Extent, intersects } from "ol/extent";
import Feature from "ol/Feature";
import LineString from "ol/geom/LineString";
import type MultiPolygon from "ol/geom/MultiPolygon";
import Point from "ol/geom/Point";
import Polygon, { fromExtent } from "ol/geom/Polygon";
import DragBox from "ol/interaction/DragBox";
import Draw from "ol/interaction/Draw";
import TileLayer from "ol/layer/Tile";
import VectorLayer from "ol/layer/Vector";
import OlMap from "ol/Map";
import type MapBrowserEvent from "ol/MapBrowserEvent";
import { unByKey } from "ol/Observable";
import { fromLonLat, transformExtent } from "ol/proj";
import VectorSource from "ol/source/Vector";
import XYZ from "ol/source/XYZ";
import { getArea, getLength } from "ol/sphere";
import { Circle as CircleStyle, Fill, Stroke, Style, Text } from "ol/style";
import type { StyleFunction } from "ol/style/Style";
import View from "ol/View";
import { useEffect, useEffectEvent, useRef, useState } from "react";

import {
    useStoredBaseMap,
    visibleVworldLayers,
    VWORLD_LAYER_NAMES,
    type VworldLayer,
} from "@/features/image/baseMap";
import { bboxToExtent, extentToBbox } from "@/features/image/bbox";
import { AreaToolbar } from "@/features/image/components/AreaToolbar";
import { FootprintChooser } from "@/features/image/components/FootprintChooser";
import { MapLayerControl } from "@/features/image/components/MapLayerControl";
import { MeasureToolbar } from "@/features/image/components/MeasureToolbar";
import { formatLatLon } from "@/features/image/coordinate";
import {
    largerFirst,
    pickImages,
    sameIds,
    toFeature,
} from "@/features/image/footprint";
import { formatArea, formatLength } from "@/features/image/measure";
import type { Bbox, ImageResponse, MapTool } from "@/features/image/types";

type ImageMapProps = {
    images: ImageResponse[];
    selectedImage: ImageResponse | null;
    hoveredImages: ImageResponse[];
    area: Bbox | undefined;
    areaApplied: boolean;
    allLoaded: boolean;
    tool: MapTool | null;
    onSelect: (image: ImageResponse) => void;
    onHover: (imageIds: number[]) => void;
    onAreaChange: (area: Bbox | undefined) => void;
    onToolStart: (tool: MapTool) => void;
    onToolStop: () => void;
};

type Chooser = {
    imageIds: number[];
    x: number;
    y: number;
};

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
// 최대 줌을 넘으면 VWorld가 이미지 대신 XML을 줘서 정확히 맞춘다(그 이상은 OpenLayers가 타일을 키워 보여줌)
const VWORLD_LAYERS: Record<
    VworldLayer,
    { format: "png" | "jpeg"; maxZoom: number }
> = {
    Base: { format: "png", maxZoom: 19 },
    midnight: { format: "png", maxZoom: 18 },
    Satellite: { format: "jpeg", maxZoom: 19 },
    Hybrid: { format: "png", maxZoom: 19 },
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

const HOVERED_STYLE_OPTIONS: VectorStyleOptions = {
    colorVariable: "--map-footprint",
    width: 3,
    fillOpacity: 0.02,
};

const SELECTED_STYLE_OPTIONS: VectorStyleOptions = {
    colorVariable: "--map-selected",
    width: 3,
    fillOpacity: 0.15,
};

const AREA_LINE_DASH = [8, 6];
const AREA_DASH_PERIOD = AREA_LINE_DASH[0] + AREA_LINE_DASH[1];
const AREA_DASH_SPEED = 0.03; // px/ms

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

const MEASURE_STYLE_OPTIONS: VectorStyleOptions = {
    colorVariable: "--map-measure",
    width: 2,
    fillOpacity: 0.1,
};

function createVworldLayer(layer: VworldLayer): TileLayer {
    const { format, maxZoom } = VWORLD_LAYERS[layer];
    return new TileLayer({
        visible: false,
        source: new XYZ({
            url: `https://api.vworld.kr/req/wmts/1.0.0/${VWORLD_KEY}/${layer}/{z}/{y}/{x}.${format}`,
            // 줌 5 이하는 타일이 없어서, 미니맵처럼 더 축소한 지도는 줌 6 타일을 줄여 그린다
            minZoom: VWORLD_MIN_ZOOM,
            maxZoom,
        }),
    });
}

function createVworldLayers(): Record<VworldLayer, TileLayer> {
    return {
        Base: createVworldLayer("Base"),
        midnight: createVworldLayer("midnight"),
        Satellite: createVworldLayer("Satellite"),
        Hybrid: createVworldLayer("Hybrid"),
    };
}

// 레이어는 지도 하나에만 넣을 수 있어서 미니맵용을 따로 만든다. 미니맵은 위치 파악용이라 배경 선택과 관계없이 일반 지도
function createOverviewMap() {
    const lightLayer = createVworldLayer("Base");
    const darkLayer = createVworldLayer("midnight");
    const control = new OverviewMap({
        layers: [lightLayer, darkLayer],
        collapsed: false,
        label: "‹",
        collapseLabel: "›",
        tipLabel: "미니맵",
    });

    function showTheme(dark: boolean) {
        lightLayer.setVisible(!dark);
        darkLayer.setVisible(dark);
    }

    return { control, showTheme };
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

function prefersReducedMotion(): boolean {
    return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

function areaDashOffset(): number {
    return prefersReducedMotion()
        ? 0
        : -(performance.now() * AREA_DASH_SPEED) % AREA_DASH_PERIOD;
}

// 도형과 함께 실제 지구 위 길이·넓이를 글자로 그린다. 선은 끝점, 면은 안쪽 점에
function createMeasureStyle(): StyleFunction {
    const color = readCssVariable("--map-measure");
    const halo = readCssVariable("--background");
    const shapeStyle = createVectorStyle(MEASURE_STYLE_OPTIONS);
    const vertexStyle = new Style({
        image: new CircleStyle({ radius: 4, fill: new Fill({ color }) }),
    });

    function labelStyle(text: string, at: Point): Style {
        return new Style({
            geometry: at,
            text: new Text({
                text,
                font: "bold 13px sans-serif",
                offsetY: -12,
                fill: new Fill({ color }),
                stroke: new Stroke({ color: halo, width: 4 }),
            }),
        });
    }

    return (feature) => {
        const geometry = feature.getGeometry();
        if (geometry instanceof Polygon) {
            return [
                shapeStyle,
                labelStyle(
                    formatArea(getArea(geometry)),
                    geometry.getInteriorPoint(),
                ),
            ];
        }
        if (geometry instanceof LineString) {
            return [
                shapeStyle,
                labelStyle(
                    formatLength(getLength(geometry)),
                    new Point(geometry.getLastCoordinate()),
                ),
            ];
        }
        return vertexStyle;
    };
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

function fitView(map: OlMap, extent: Extent, maxZoom = FIT_MAX_ZOOM) {
    map.getView().fit(extent, {
        padding: [FIT_PADDING, FIT_PADDING, FIT_PADDING, FIT_PADDING],
        maxZoom,
        duration: prefersReducedMotion() ? 0 : FIT_DURATION,
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
    hoveredImages,
    area,
    areaApplied,
    allLoaded,
    tool,
    onSelect,
    onHover,
    onAreaChange,
    onToolStart,
    onToolStop,
}: ImageMapProps) {
    const containerRef = useRef<HTMLDivElement | null>(null);
    const mapRef = useRef<OlMap | null>(null);
    const dragBoxRef = useRef<DragBox | null>(null);
    const statusRef = useRef<HTMLDivElement | null>(null);
    // OpenLayers 소스·레이어는 여러 effect가 함께 써서 컴포넌트와 함께 한 번 만든다
    const [footprintSource] = useState(() => new VectorSource());
    const [selectionSource] = useState(() => new VectorSource());
    const [hoverSource] = useState(() => new VectorSource());
    const [areaSource] = useState(() => new VectorSource());
    const [measureSource] = useState(() => new VectorSource());
    const [measureLayer] = useState(
        () => new VectorLayer({ source: measureSource }),
    );
    const [vworldLayers] = useState(() =>
        VWORLD_KEY ? createVworldLayers() : null,
    );
    const [footprintLayer] = useState(
        () =>
            new VectorLayer({
                source: footprintSource,
                renderOrder: largerFirst,
            }),
    );
    const [baseMap, changeBaseMap] = useStoredBaseMap();
    const [footprintFillScale, setFootprintFillScale] = useState(1);
    const [chooser, setChooser] = useState<Chooser | null>(null);
    const [chooserOpen, setChooserOpen] = useState(false);

    const drawing = tool === "area";
    const measuring = tool === "distance" || tool === "surface";
    const toolActive = tool !== null;
    const chooserImages = chooser ? pickImages(images, chooser.imageIds) : [];

    // 그 지점에 겹쳐 있는 footprint들의 영상
    function imagesAt(event: MapBrowserEvent): ImageResponse[] {
        const hitIds = event.map
            .getFeaturesAtPixel(event.pixel, {
                layerFilter: (layer) => layer.getSource() === footprintSource,
            })
            .map((feature) => feature.getId());
        return pickImages(images, hitIds);
    }

    const handleMapClick = useEffectEvent((event: MapBrowserEvent) => {
        const hits = imagesAt(event);

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

    const handlePointerMove = useEffectEvent((event: MapBrowserEvent) => {
        if (event.dragging) {
            return;
        }
        const hitIds = imagesAt(event).map((image) => image.imageId);
        // 같은 footprint들 위에서 움직일 땐 state를 바꾸지 않는다
        const hoveredIds = hoveredImages.map((image) => image.imageId);
        if (!sameIds(hitIds, hoveredIds)) {
            onHover(hitIds);
        }
    });

    const showBaseMap = useEffectEvent(() => {
        if (!vworldLayers) {
            return;
        }
        const dark = window.matchMedia("(prefers-color-scheme: dark)").matches;
        const visible = visibleVworldLayers(baseMap, dark);
        for (const name of VWORLD_LAYER_NAMES) {
            vworldLayers[name].setVisible(visible.includes(name));
        }
    });

    const applyFootprintStyle = useEffectEvent(() => {
        footprintLayer.setStyle(
            createVectorStyle({
                ...FOOTPRINT_STYLE_OPTIONS,
                fillOpacity:
                    FOOTPRINT_STYLE_OPTIONS.fillOpacity * footprintFillScale,
            }),
        );
    });

    const stopActiveTool = useEffectEvent(() => onToolStop());

    const handleBoxEnd = useEffectEvent((extent: Extent) => {
        onToolStop();
        const bbox = extentToBbox(extent);
        if (bbox) {
            onAreaChange(bbox);
        }
    });

    useEffect(() => {
        const baseLayers = vworldLayers
            ? VWORLD_LAYER_NAMES.map((name) => vworldLayers[name])
            : [];
        const areaLayer = new VectorLayer({ source: areaSource });
        const hoverLayer = new VectorLayer({ source: hoverSource });
        const selectionLayer = new VectorLayer({ source: selectionSource });

        const map = new OlMap({
            target: containerRef.current ?? undefined,
            layers: [
                ...baseLayers,
                footprintLayer,
                areaLayer,
                measureLayer,
                hoverLayer,
                selectionLayer,
            ],
            view: new View({
                center: fromLonLat(SEOUL_LON_LAT),
                zoom: INITIAL_ZOOM,
                minZoom: VWORLD_MIN_ZOOM,
                extent: VWORLD_EXTENT,
                smoothExtentConstraint: false,
            }),
        });
        mapRef.current = map;

        const status = statusRef.current ?? undefined;
        map.addControl(
            new MousePosition({
                target: status,
                projection: "EPSG:4326",
                coordinateFormat: formatLatLon,
                placeholder: "",
            }),
        );
        map.addControl(new ScaleLine({ target: status }));

        const overviewMap = VWORLD_KEY ? createOverviewMap() : null;
        if (overviewMap) {
            map.addControl(overviewMap.control);
        }

        const darkMode = window.matchMedia("(prefers-color-scheme: dark)");

        function applyTheme() {
            overviewMap?.showTheme(darkMode.matches);
            showBaseMap();
            applyFootprintStyle();
            const areaStyle = createVectorStyle(AREA_STYLE_OPTIONS);
            const unappliedAreaStyle = createVectorStyle(
                UNAPPLIED_AREA_STYLE_OPTIONS,
            );
            areaLayer.setStyle((feature) => {
                const style = feature.get("applied")
                    ? areaStyle
                    : unappliedAreaStyle;
                style.getStroke()?.setLineDashOffset(areaDashOffset());
                return style;
            });
            measureLayer.setStyle(createMeasureStyle());
            hoverLayer.setStyle(createVectorStyle(HOVERED_STYLE_OPTIONS));
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
    }, [
        footprintSource,
        selectionSource,
        hoverSource,
        areaSource,
        vworldLayers,
        footprintLayer,
        measureLayer,
    ]);

    useEffect(() => {
        showBaseMap();
    }, [baseMap]);

    useEffect(() => {
        applyFootprintStyle();
    }, [footprintFillScale]);

    useEffect(() => {
        dragBoxRef.current?.setActive(drawing);
    }, [drawing]);

    useEffect(() => {
        if (!drawing) {
            return;
        }
        function cancelOnEscape(event: KeyboardEvent) {
            if (event.key === "Escape") {
                stopActiveTool();
            }
        }
        document.addEventListener("keydown", cancelOnEscape);
        return () => document.removeEventListener("keydown", cancelOnEscape);
    }, [drawing]);

    useEffect(() => {
        const map = mapRef.current;
        if (!map || (tool !== "distance" && tool !== "surface")) {
            return;
        }
        const draw = new Draw({
            source: measureSource,
            type: tool === "distance" ? "LineString" : "Polygon",
            // 면적을 그릴 땐 테두리를 따라가는 보조선에 길이 글자가 붙지 않게 면만 그린다
            style: (feature, resolution) =>
                tool === "surface" &&
                feature.getGeometry() instanceof LineString
                    ? undefined
                    : measureLayer.getStyleFunction()?.(feature, resolution),
        });
        let sketching = false;
        draw.on("drawstart", () => {
            // 측정은 하나만 보인다
            measureSource.clear();
            sketching = true;
        });
        draw.on(["drawend", "drawabort"], () => {
            sketching = false;
        });
        // 그리던 도형이 있으면 그것만 취소하고, 없으면 도구를 끈다
        function cancelOnEscape(event: KeyboardEvent) {
            if (event.key !== "Escape") {
                return;
            }
            if (sketching) {
                draw.abortDrawing();
            } else {
                stopActiveTool();
            }
        }
        map.addInteraction(draw);
        document.addEventListener("keydown", cancelOnEscape);
        return () => {
            document.removeEventListener("keydown", cancelOnEscape);
            map.removeInteraction(draw);
            measureSource.clear();
        };
    }, [tool, measureSource, measureLayer]);

    useEffect(() => {
        const map = mapRef.current;
        if (!map || toolActive) {
            return;
        }
        // footprint 클릭(선택)과 마우스 올리기(강조)는 도구를 쓰지 않을 때만
        const keys = [
            map.on("singleclick", (event) => handleMapClick(event)),
            map.on("pointermove", (event) => handlePointerMove(event)),
        ];
        return () => unByKey(keys);
    }, [toolActive]);

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
        hoverSource.addFeatures(hoveredImages.map(toFeature));
        return () => hoverSource.clear();
    }, [hoverSource, hoveredImages]);

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

    // 영역이 있는 동안 매 프레임 영역을 다시 그려, 스타일 함수가 그 시점의 점선 위치를 계산하게 한다
    useEffect(() => {
        if (!area || prefersReducedMotion()) {
            return;
        }
        let frame = requestAnimationFrame(function redraw() {
            areaSource.changed();
            frame = requestAnimationFrame(redraw);
        });
        return () => cancelAnimationFrame(frame);
    }, [areaSource, area]);

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
        const extent = map.getView().calculateExtent(size);
        const bbox = extentToBbox(extent);
        if (bbox) {
            onAreaChange(bbox);
            // 테두리가 지도 가장자리에 걸려 반만 보이지 않게, 여백만큼만 축소한다(지금 줌보다 확대하지 않음)
            fitView(map, extent, map.getView().getZoom());
        }
    }

    function cancelArea() {
        if (drawing) {
            onToolStop();
        } else {
            onAreaChange(undefined);
        }
    }

    return (
        <>
            {/* 지도의 pointermove는 지도 밖으로 나가는 순간을 알려주지 않아서 여기서 강조를 지운다 */}
            <div
                ref={containerRef}
                className={`absolute inset-0 ${hoveredImages.length > 0 ? "cursor-pointer" : ""}`}
                onPointerLeave={() => onHover([])}
            />
            <div
                ref={statusRef}
                className="map-status absolute bottom-2 left-2 flex flex-col items-start gap-1"
            />
            <div className="absolute top-2 right-2 flex gap-2">
                <AreaToolbar
                    drawing={drawing}
                    hasArea={area !== undefined}
                    onUseCurrentView={selectCurrentView}
                    onStartDrawing={() => onToolStart("area")}
                    onCancel={cancelArea}
                />
                <MeasureToolbar
                    measuring={measuring}
                    onStartDistance={() => onToolStart("distance")}
                    onStartSurface={() => onToolStart("surface")}
                    onStop={onToolStop}
                />
                <MapLayerControl
                    baseMap={baseMap}
                    footprintFillScale={footprintFillScale}
                    onBaseMapChange={changeBaseMap}
                    onFootprintFillScaleChange={setFootprintFillScale}
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
                    onHover={onHover}
                />
            )}
        </>
    );
}
