"use client";

import { ScanIcon, SquareDashedIcon, XIcon } from "lucide-react";
import type { ComponentProps } from "react";

import { IconButton } from "@/shared/components/IconButton";

type AreaToolbarProps = {
    drawing: boolean;
    hasArea: boolean;
    onUseCurrentView: () => void;
    onStartDrawing: () => void;
    onCancel: () => void;
};

function ToolbarButton(props: ComponentProps<typeof IconButton>) {
    return (
        <IconButton
            variant="secondary"
            className="shadow-sm"
            title={props.label}
            {...props}
        />
    );
}

export function AreaToolbar({
    drawing,
    hasArea,
    onUseCurrentView,
    onStartDrawing,
    onCancel,
}: AreaToolbarProps) {
    if (drawing || hasArea) {
        return (
            <ToolbarButton
                label={drawing ? "그리기 취소" : "영역 지우기"}
                onClick={onCancel}
            >
                <XIcon />
            </ToolbarButton>
        );
    }

    return (
        <>
            <ToolbarButton
                label="현재 화면 범위로 검색"
                onClick={onUseCurrentView}
            >
                <ScanIcon />
            </ToolbarButton>
            <ToolbarButton label="영역 그리기" onClick={onStartDrawing}>
                <SquareDashedIcon />
            </ToolbarButton>
        </>
    );
}
