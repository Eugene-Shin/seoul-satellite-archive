"use client";

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

    return (
        <fieldset className="flex w-64 flex-col gap-1">
            <legend className="font-semibold tabular-nums">
                {label}: {low} ~ {high} {unit}
            </legend>
            <input
                type="range"
                aria-label={`${label} 최소값`}
                min={min}
                max={max}
                step={step}
                value={low}
                onChange={(e) =>
                    onChange([Math.min(e.target.valueAsNumber, high), high])
                }
            />
            <input
                type="range"
                aria-label={`${label} 최대값`}
                min={min}
                max={max}
                step={step}
                value={high}
                onChange={(e) =>
                    onChange([low, Math.max(e.target.valueAsNumber, low)])
                }
            />
        </fieldset>
    );
}
