"use client";

import "ol/ol.css";

import Feature from "ol/Feature";
import MultiPolygon from "ol/geom/MultiPolygon";
import TileLayer from "ol/layer/Tile";
import VectorLayer from "ol/layer/Vector";
import OlMap from "ol/Map";
import type MapBrowserEvent from "ol/MapBrowserEvent";
import { fromLonLat, transformExtent } from "ol/proj";
import type { OrderFunction } from "ol/render";
import VectorSource from "ol/source/Vector";
import XYZ from "ol/source/XYZ";
import { Fill, Stroke, Style } from "ol/style";
import View from "ol/View";
import { useEffect, useEffectEvent, useRef, useState } from "react";

import { FootprintChooser } from "@/features/image/components/FootprintChooser";
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
    flipX: boolean;
    flipY: boolean;
};

const SEOUL_LON_LAT = [126.978, 37.566];
const INITIAL_ZOOM = 11;
const VWORLD_KEY = process.env.NEXT_PUBLIC_VWORLD_KEY;
const VWORLD_MIN_ZOOM = 6;
const VWORLD_MAX_ZOOM = 19;
const VWORLD_EXTENT = transformExtent(
    [111.1, 25, 145, 47],
    "EPSG:4326",
    "EPSG:3857",
);

const FOOTPRINT_STYLE = new Style({
    stroke: new Stroke({ color: "#2563eb", width: 1.5 }),
    fill: new Fill({ color: "rgba(37, 99, 235, 0.04)" }),
});

const SELECTED_STYLE = new Style({
    stroke: new Stroke({ color: "#f97316", width: 3 }),
    fill: new Fill({ color: "rgba(249, 115, 22, 0.15)" }),
});

const largerFirst: OrderFunction = (a, b) => b.get("area") - a.get("area");

function toFeature(image: ImageResponse): Feature {
    const geometry = new MultiPolygon(image.footprint.coordinates).transform(
        "EPSG:4326",
        "EPSG:3857",
    );
    const feature = new Feature({ geometry, area: geometry.getArea() });
    feature.setId(image.imageId);
    return feature;
}

export function ImageMap({ images, selectedImage, onSelect }: ImageMapProps) {
    const mapRef = useRef<HTMLDivElement | null>(null);
    const [footprintSource] = useState(() => new VectorSource());
    const [selectionSource] = useState(() => new VectorSource());
    const [chooser, setChooser] = useState<Chooser | null>(null);

    const chooserImages = chooser
        ? images.filter((image) => chooser.imageIds.includes(image.imageId))
        : [];

    const handleMapClick = useEffectEvent((event: MapBrowserEvent) => {
        const hitIds = event.map
            .getFeaturesAtPixel(event.pixel, {
                layerFilter: (layer) => layer.getSource() === footprintSource,
            })
            .map((feature) => feature.getId());
        const hits = images.filter((image) => hitIds.includes(image.imageId));

        if (hits.length === 1) {
            setChooser(null);
            onSelect(hits[0]);
        } else if (hits.length > 1) {
            const [width, height] = event.map.getSize() ?? [0, 0];
            const [x, y] = event.pixel;
            setChooser({
                imageIds: hits.map((image) => image.imageId),
                x,
                y,
                flipX: x > width / 2,
                flipY: y > height / 2,
            });
        } else {
            setChooser(null);
        }
    });

    useEffect(() => {
        const baseLayers = VWORLD_KEY
            ? [
                  new TileLayer({
                      source: new XYZ({
                          url: `https://api.vworld.kr/req/wmts/1.0.0/${VWORLD_KEY}/Base/{z}/{y}/{x}.png`,
                          maxZoom: VWORLD_MAX_ZOOM,
                      }),
                  }),
              ]
            : [];

        const map = new OlMap({
            target: mapRef.current ?? undefined,
            layers: [
                ...baseLayers,
                new VectorLayer({
                    source: footprintSource,
                    style: FOOTPRINT_STYLE,
                    renderOrder: largerFirst,
                }),
                new VectorLayer({
                    source: selectionSource,
                    style: SELECTED_STYLE,
                }),
            ],
            view: new View({
                center: fromLonLat(SEOUL_LON_LAT),
                zoom: INITIAL_ZOOM,
                minZoom: VWORLD_MIN_ZOOM,
                extent: VWORLD_EXTENT,
                smoothExtentConstraint: false,
            }),
        });

        map.on("singleclick", (event) => handleMapClick(event));
        map.on("movestart", () => setChooser(null));

        return () => map.dispose();
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
            {chooser && chooserImages.length > 0 && (
                <FootprintChooser
                    images={chooserImages}
                    x={chooser.x}
                    y={chooser.y}
                    flipX={chooser.flipX}
                    flipY={chooser.flipY}
                    onSelect={(image) => {
                        setChooser(null);
                        onSelect(image);
                    }}
                    onClose={() => setChooser(null)}
                />
            )}
        </>
    );
}
