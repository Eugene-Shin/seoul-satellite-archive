"use client";

import { ScanIcon, SquareDashedIcon, XIcon } from "lucide-react";

import { MapToolbarButton } from "@/features/image/map/MapToolbarButton";

type AreaToolbarProps = {
    drawing: boolean;
    hasArea: boolean;
    onUseCurrentView: () => void;
    onStartDrawing: () => void;
    onCancel: () => void;
};

export function AreaToolbar({
    drawing,
    hasArea,
    onUseCurrentView,
    onStartDrawing,
    onCancel,
}: AreaToolbarProps) {
    if (drawing || hasArea) {
        return (
            <MapToolbarButton
                label={drawing ? "그리기 취소" : "영역 지우기"}
                onClick={() => onCancel()}
            >
                <XIcon />
            </MapToolbarButton>
        );
    }

    return (
        <>
            <MapToolbarButton
                label="현재 화면을 영역으로"
                onClick={() => onUseCurrentView()}
            >
                <ScanIcon />
            </MapToolbarButton>
            <MapToolbarButton
                label="영역 그리기"
                onClick={() => onStartDrawing()}
            >
                <SquareDashedIcon />
            </MapToolbarButton>
        </>
    );
}
