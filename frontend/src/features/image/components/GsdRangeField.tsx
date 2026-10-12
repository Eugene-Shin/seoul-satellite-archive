import type { ImageSearchParams, Range } from "@/features/image/types";
import { RangeSlider } from "@/shared/components/RangeSlider";

type GsdRangeFieldProps = {
    range: Range<number>;
    conditions: ImageSearchParams;
    onChange: (change: Partial<ImageSearchParams>) => void;
};

export function GsdRangeField({
    range,
    conditions,
    onChange,
}: GsdRangeFieldProps) {
    const { min: floor, max: ceiling } = range;

    if (floor === null || ceiling === null) {
        return null;
    }

    return (
        <RangeSlider
            label="해상도(GSD)"
            min={floor}
            max={ceiling}
            step={0.01}
            unit="m"
            value={[conditions.gsdMin ?? floor, conditions.gsdMax ?? ceiling]}
            onChange={([low, high]) =>
                onChange({
                    gsdMin: low > floor ? low : undefined,
                    gsdMax: high < ceiling ? high : undefined,
                })
            }
        />
    );
}
