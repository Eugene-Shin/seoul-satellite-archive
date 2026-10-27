"use client";

import { useState } from "react";

import type { SensorType } from "@/features/image/types";
import { Slider } from "@/shared/components/ui/slider";

const MAX_CLOUD_COVER = 100;

export function isSarOnly(sensorTypes: SensorType[] | undefined): boolean {
    return sensorTypes ? sensorTypes.every((type) => type === "SAR") : false;
}

type CloudCoverFieldProps = {
    value: number | undefined;
    disabled: boolean;
    onChange: (cloudMax: number | undefined) => void;
};

export function CloudCoverField({
    value,
    disabled,
    onChange,
}: CloudCoverFieldProps) {
    // 끄는 동안의 값. 놓을 때만 onChange로 알려 조회가 한 번만 일어나게 한다
    const [dragging, setDragging] = useState<number | null>(null);
    const current = dragging ?? value ?? MAX_CLOUD_COVER;
    const hint = disabled ? "SAR 영상은 구름량 정보가 없습니다." : null;

    return (
        <label className="flex w-full flex-col gap-2">
            <span className="text-sm font-medium tabular-nums">
                최대 구름량: {current}%
            </span>
            <Slider
                min={0}
                max={MAX_CLOUD_COVER}
                step={1}
                value={[current]}
                disabled={disabled}
                onValueChange={([next]) => setDragging(next)}
                onValueCommitted={([next]) => {
                    onChange(next < MAX_CLOUD_COVER ? next : undefined);
                    setDragging(null);
                }}
                getAriaLabel={() => "최대 구름량"}
            />
            <span className="min-h-5 text-sm text-muted-foreground">
                {hint}
            </span>
        </label>
    );
}
