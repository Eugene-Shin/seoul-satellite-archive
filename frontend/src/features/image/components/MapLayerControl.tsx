"use client";

import { LayersIcon } from "lucide-react";

import { BASE_MAPS, type BaseMap } from "@/features/image/baseMap";
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

type MapLayerControlProps = {
    baseMap: BaseMap;
    footprintFillScale: number;
    onBaseMapChange: (baseMap: BaseMap) => void;
    onFootprintFillScaleChange: (scale: number) => void;
};

export function MapLayerControl({
    baseMap,
    footprintFillScale,
    onBaseMapChange,
    onFootprintFillScaleChange,
}: MapLayerControlProps) {
    return (
        <Popover>
            <PopoverTrigger
                render={
                    <Button
                        variant="secondary"
                        size="sm"
                        aria-label={LABEL}
                        title={LABEL}
                        className="rounded-full px-3 shadow-sm"
                    />
                }
            >
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
            </PopoverContent>
        </Popover>
    );
}
