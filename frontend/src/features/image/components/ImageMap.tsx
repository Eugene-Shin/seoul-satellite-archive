"use client";

import "ol/ol.css";

import OlMap from "ol/Map";
import { fromLonLat } from "ol/proj";
import View from "ol/View";
import { useEffect, useRef } from "react";

const SEOUL_LON_LAT = [126.978, 37.566];
const INITIAL_ZOOM = 11;

export function ImageMap() {
    const mapRef = useRef<HTMLDivElement | null>(null);

    useEffect(() => {
        const map = new OlMap({
            target: mapRef.current ?? undefined,
            view: new View({
                center: fromLonLat(SEOUL_LON_LAT),
                zoom: INITIAL_ZOOM,
            }),
        });

        return () => map.dispose();
    }, []);

    return <div ref={mapRef} className="absolute inset-0" />;
}
