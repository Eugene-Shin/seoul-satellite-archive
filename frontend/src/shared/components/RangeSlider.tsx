"use client";

import { Slider } from "@/shared/components/ui/slider";

type RangeSliderProps = {
    label: string;
    min: number;
    max: number;
    unit: string;
    step: number;
    value: [number, number];
    onChange: (value: [number, number]) => void;
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
    const [low, high] = value;

    function thumbLabel(index: number): string {
        return `${label} ${index === 0 ? "최소값" : "최대값"}`;
    }

    return (
        <fieldset className="flex w-full flex-col gap-3">
            <legend className="mb-3 font-semibold tabular-nums">
                {label}: {low} ~ {high} {unit}
            </legend>
            <Slider
                min={min}
                max={max}
                step={step}
                value={value}
                onValueChange={onChange}
                getAriaLabel={thumbLabel}
            />
        </fieldset>
    );
}
