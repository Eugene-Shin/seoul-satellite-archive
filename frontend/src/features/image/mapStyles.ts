import { asArray } from "ol/color";
import LineString from "ol/geom/LineString";
import Point from "ol/geom/Point";
import Polygon from "ol/geom/Polygon";
import { getArea, getLength } from "ol/sphere";
import { Circle as CircleStyle, Fill, Stroke, Style, Text } from "ol/style";
import type { StyleFunction } from "ol/style/Style";

import { formatArea, formatLength } from "@/features/image/measure";

type VectorStyleOptions = {
    colorVariable: string;
    width: number;
    fillOpacity: number;
    lineDash?: number[];
};

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

export function prefersReducedMotion(): boolean {
    return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

function areaDashOffset(): number {
    return prefersReducedMotion()
        ? 0
        : -(performance.now() * AREA_DASH_SPEED) % AREA_DASH_PERIOD;
}

export function createFootprintStyle(fillScale: number): Style {
    return createVectorStyle({
        ...FOOTPRINT_STYLE_OPTIONS,
        fillOpacity: FOOTPRINT_STYLE_OPTIONS.fillOpacity * fillScale,
    });
}

export function createHoveredStyle(): Style {
    return createVectorStyle(HOVERED_STYLE_OPTIONS);
}

export function createSelectedStyle(): Style {
    return createVectorStyle(SELECTED_STYLE_OPTIONS);
}

export function createAreaStyle(): StyleFunction {
    const areaStyle = createVectorStyle(AREA_STYLE_OPTIONS);
    const unappliedAreaStyle = createVectorStyle(UNAPPLIED_AREA_STYLE_OPTIONS);

    return (feature) => {
        const style = feature.get("applied") ? areaStyle : unappliedAreaStyle;
        style.getStroke()?.setLineDashOffset(areaDashOffset());
        return style;
    };
}

// 도형과 함께 실제 지구 위 길이·넓이를 글자로 그린다. 선은 끝점, 면은 안쪽 점에
export function createMeasureStyle(): StyleFunction {
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
