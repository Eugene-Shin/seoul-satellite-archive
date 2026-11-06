"use client";

import { LayersIcon } from "lucide-react";

import { BASE_MAPS, type BaseMap } from "@/features/image/map/baseMap";
import { MapToolbarPillButton } from "@/features/image/map/MapToolbarPillButton";
import { Button } from "@/shared/components/ui/button";
import {
    Popover,
    PopoverContent,
    PopoverTrigger,
} from "@/shared/components/ui/popover";
import { Slider } from "@/shared/components/ui/slider";

const BASE_MAP_NAMES: Record<BaseMap, string> = {
    map: "일반 지도",
    satellite: "위성 영상",
    hybrid: "위성 & 지명",
};

const LABEL = "배경 지도 및 위성 영상 범위 설정";
const FOOTPRINT_OPACITY = "위성 영상 범위 투명도";
const OVERLAY_OPACITY = "위성 영상 미리보기 투명도";

type MapLayerControlProps = {
    baseMap: BaseMap;
    footprintFillScale: number;
    overlayOpacity: number;
    onBaseMapChange: (baseMap: BaseMap) => void;
    onFootprintFillScaleChange: (scale: number) => void;
    onOverlayOpacityChange: (opacity: number) => void;
};

export function MapLayerControl({
    baseMap,
    footprintFillScale,
    overlayOpacity,
    onBaseMapChange,
    onFootprintFillScaleChange,
    onOverlayOpacityChange,
}: MapLayerControlProps) {
    return (
        <Popover>
            <PopoverTrigger render={<MapToolbarPillButton label={LABEL} />}>
                <LayersIcon />
            </PopoverTrigger>
            <PopoverContent align="end" className="w-auto gap-4">
                <div role="group" aria-label="배경 지도" className="flex gap-1">
                    {BASE_MAPS.map((option) => (
                        <Button
                            key={option}
                            size="sm"
                            variant={
                                option === baseMap ? "default" : "secondary"
                            }
                            aria-pressed={option === baseMap}
                            onClick={() => onBaseMapChange(option)}
                        >
                            {BASE_MAP_NAMES[option]}
                        </Button>
                    ))}
                </div>
                <label className="flex flex-col gap-2">
                    <span>{FOOTPRINT_OPACITY}</span>
                    <Slider
                        min={0}
                        max={2}
                        step={0.1}
                        value={[footprintFillScale]}
                        onValueChange={([scale]) =>
                            onFootprintFillScaleChange(scale)
                        }
                        getAriaLabel={() => FOOTPRINT_OPACITY}
                    />
                </label>
                <label className="flex flex-col gap-2">
                    <span>{OVERLAY_OPACITY}</span>
                    <Slider
                        min={0}
                        max={1}
                        step={0.05}
                        value={[overlayOpacity]}
                        onValueChange={([opacity]) =>
                            onOverlayOpacityChange(opacity)
                        }
                        getAriaLabel={() => OVERLAY_OPACITY}
                    />
                </label>
            </PopoverContent>
        </Popover>
    );
}
