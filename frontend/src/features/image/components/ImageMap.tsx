"use client";

import "ol/ol.css";

import Feature from "ol/Feature";
import MultiPolygon from "ol/geom/MultiPolygon";
import TileLayer from "ol/layer/Tile";
import VectorLayer from "ol/layer/Vector";
import OlMap from "ol/Map";
import { fromLonLat, transformExtent } from "ol/proj";
import type { OrderFunction } from "ol/render";
import VectorSource from "ol/source/Vector";
import XYZ from "ol/source/XYZ";
import { Fill, Stroke, Style } from "ol/style";
import View from "ol/View";
import { useEffect, useRef, useState } from "react";

import type { ImageResponse } from "@/features/image/types";

type ImageMapProps = {
    images: ImageResponse[];
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

export function ImageMap({ images }: ImageMapProps) {
    const mapRef = useRef<HTMLDivElement | null>(null);
    const [footprintSource] = useState(() => new VectorSource());

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
            ],
            view: new View({
                center: fromLonLat(SEOUL_LON_LAT),
                zoom: INITIAL_ZOOM,
                minZoom: VWORLD_MIN_ZOOM,
                extent: VWORLD_EXTENT,
                smoothExtentConstraint: false,
            }),
        });

        return () => map.dispose();
    }, [footprintSource]);

    useEffect(() => {
        footprintSource.clear();
        footprintSource.addFeatures(images.map(toFeature));
    }, [footprintSource, images]);

    return <div ref={mapRef} className="absolute inset-0" />;
}
