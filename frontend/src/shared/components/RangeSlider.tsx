"use client";

import { useState } from "react";

import { Slider } from "@/shared/components/ui/slider";

type Range = [low: number, high: number];

type RangeSliderProps = {
    label: string;
    min: number;
    max: number;
    unit: string;
    step: number;
    value: Range;
    onChange: (value: Range) => void;
};

export function RangeSlider({
    label,
    min,
    max,
    step,
    unit,
    value,
    onChange,
}: RangeSliderProps) {
    // 끄는 동안의 값. 놓을 때만 onChange로 알려 조회가 한 번만 일어나게 한다
    const [dragging, setDragging] = useState<Range | null>(null);
    const current = dragging ?? value;
    const [low, high] = current;

    function thumbLabel(index: number): string {
        return `${label} ${index === 0 ? "최소값" : "최대값"}`;
    }

    return (
        <fieldset className="flex w-full flex-col gap-2">
            <legend className="mb-2 text-sm font-medium tabular-nums">
                {label}: {low} ~ {high} {unit}
            </legend>
            <Slider
                min={min}
                max={max}
                step={step}
                value={current}
                onValueChange={(next) => setDragging(next)}
                onValueCommitted={(next) => {
                    onChange(next);
                    setDragging(null);
                }}
                getAriaLabel={thumbLabel}
            />
        </fieldset>
    );
}
