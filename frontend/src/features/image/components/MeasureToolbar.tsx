"use client";

import { LandPlotIcon, RulerIcon, XIcon } from "lucide-react";

import { MapToolbarButton } from "@/features/image/components/MapToolbarButton";

type MeasureToolbarProps = {
    measuring: boolean;
    onStartDistance: () => void;
    onStartSurface: () => void;
    onStop: () => void;
};

export function MeasureToolbar({
    measuring,
    onStartDistance,
    onStartSurface,
    onStop,
}: MeasureToolbarProps) {
    if (measuring) {
        return (
            <MapToolbarButton label="측정 끄기" onClick={() => onStop()}>
                <XIcon />
            </MapToolbarButton>
        );
    }

    return (
        <>
            <MapToolbarButton
                label="거리 측정"
                onClick={() => onStartDistance()}
            >
                <RulerIcon />
            </MapToolbarButton>
            <MapToolbarButton
                label="면적 측정"
                onClick={() => onStartSurface()}
            >
                <LandPlotIcon />
            </MapToolbarButton>
        </>
    );
}
