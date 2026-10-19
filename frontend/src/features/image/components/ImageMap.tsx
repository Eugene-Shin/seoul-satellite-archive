"use client";

import "ol/ol.css";

import TileLayer from "ol/layer/Tile";
import OlMap from "ol/Map";
import { fromLonLat } from "ol/proj";
import XYZ from "ol/source/XYZ";
import View from "ol/View";
import { useEffect, useRef } from "react";

const SEOUL_LON_LAT = [126.978, 37.566];
const INITIAL_ZOOM = 11;
const VWORLD_KEY = process.env.NEXT_PUBLIC_VWORLD_KEY;
const VWORLD_MIN_ZOOM = 6;
const VWORLD_MAX_ZOOM = 19;

export function ImageMap() {
    const mapRef = useRef<HTMLDivElement | null>(null);

    useEffect(() => {
        const layers = VWORLD_KEY
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
            layers,
            view: new View({
                center: fromLonLat(SEOUL_LON_LAT),
                zoom: INITIAL_ZOOM,
                minZoom: VWORLD_MIN_ZOOM,
            }),
        });

        return () => map.dispose();
    }, []);

    return <div ref={mapRef} className="absolute inset-0" />;
}
