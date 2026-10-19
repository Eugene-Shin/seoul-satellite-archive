"use client";

import "ol/ol.css";

import { asArray } from "ol/color";
import TileLayer from "ol/layer/Tile";
import VectorLayer from "ol/layer/Vector";
import OlMap from "ol/Map";
import type MapBrowserEvent from "ol/MapBrowserEvent";
import { fromLonLat, transformExtent } from "ol/proj";
import VectorSource from "ol/source/Vector";
import XYZ from "ol/source/XYZ";
import { Fill, Stroke, Style } from "ol/style";
import View from "ol/View";
import { useEffect, useEffectEvent, useRef, useState } from "react";

import { FootprintChooser } from "@/features/image/components/FootprintChooser";
import { largerFirst, pickImages, toFeature } from "@/features/image/footprint";
import type { ImageResponse } from "@/features/image/types";

type ImageMapProps = {
    images: ImageResponse[];
    selectedImage: ImageResponse | null;
    onSelect: (image: ImageResponse) => void;
};

type Chooser = {
    imageIds: number[];
    x: number;
    y: number;
};

type VworldLayer = "Base" | "midnight";

type FootprintStyleOptions = {
    colorVariable: string;
    width: number;
    fillOpacity: number;
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

const FOOTPRINT_STYLE_OPTIONS: FootprintStyleOptions = {
    colorVariable: "--map-footprint",
    width: 1.5,
    fillOpacity: 0.04,
};

const SELECTED_STYLE_OPTIONS: FootprintStyleOptions = {
    colorVariable: "--map-selected",
    width: 3,
    fillOpacity: 0.15,
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

function createFootprintStyle({
    colorVariable,
    width,
    fillOpacity,
}: FootprintStyleOptions): Style {
    const color = readCssVariable(colorVariable);
    const [red, green, blue] = asArray(color);

    return new Style({
        stroke: new Stroke({ color, width }),
        fill: new Fill({ color: [red, green, blue, fillOpacity] }),
    });
}

export function ImageMap({ images, selectedImage, onSelect }: ImageMapProps) {
    const mapRef = useRef<HTMLDivElement | null>(null);
    const [footprintSource] = useState(() => new VectorSource());
    const [selectionSource] = useState(() => new VectorSource());
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
        const selectionLayer = new VectorLayer({ source: selectionSource });

        const map = new OlMap({
            target: mapRef.current ?? undefined,
            layers: [...baseLayers, footprintLayer, selectionLayer],
            view: new View({
                center: fromLonLat(SEOUL_LON_LAT),
                zoom: INITIAL_ZOOM,
                minZoom: VWORLD_MIN_ZOOM,
                extent: VWORLD_EXTENT,
                smoothExtentConstraint: false,
            }),
        });

        const darkMode = window.matchMedia("(prefers-color-scheme: dark)");

        function applyTheme() {
            lightBaseLayer?.setVisible(!darkMode.matches);
            darkBaseLayer?.setVisible(darkMode.matches);
            footprintLayer.setStyle(
                createFootprintStyle(FOOTPRINT_STYLE_OPTIONS),
            );
            selectionLayer.setStyle(
                createFootprintStyle(SELECTED_STYLE_OPTIONS),
            );
        }

        applyTheme();
        darkMode.addEventListener("change", applyTheme);

        map.on("singleclick", (event) => handleMapClick(event));
        map.on("movestart", () => setChooserOpen(false));

        return () => {
            darkMode.removeEventListener("change", applyTheme);
            map.dispose();
        };
    }, [footprintSource, selectionSource]);

    useEffect(() => {
        footprintSource.clear();
        footprintSource.addFeatures(images.map(toFeature));
    }, [footprintSource, images]);

    useEffect(() => {
        selectionSource.clear();
        if (selectedImage) {
            selectionSource.addFeature(toFeature(selectedImage));
        }
    }, [selectionSource, selectedImage]);

    return (
        <>
            <div ref={mapRef} className="absolute inset-0" />
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
