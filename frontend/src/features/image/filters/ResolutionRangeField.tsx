"use client";

import type {
    ImageSearchParams,
    ResolutionRange,
} from "@/features/image/types";
import { RangeSlider } from "@/shared/components/RangeSlider";

type ResolutionRangeFieldProps = {
    range: ResolutionRange;
    conditions: ImageSearchParams;
    onChange: (change: Partial<ImageSearchParams>) => void;
};

export function ResolutionRangeField({
    range,
    conditions,
    onChange,
}: ResolutionRangeFieldProps) {
    const { min: floor, max: ceiling } = range;

    if (floor === null || ceiling === null) {
        return null;
    }

    return (
        <RangeSlider
            label="해상도"
            hint="파일의 픽셀 간격 (없는 영상은 센서 해상도로 대체)"
            min={floor}
            max={ceiling}
            step={0.01}
            unit="m"
            value={[
                conditions.resolutionMin ?? floor,
                conditions.resolutionMax ?? ceiling,
            ]}
            onChange={([low, high]) =>
                onChange({
                    resolutionMin: low > floor ? low : undefined,
                    resolutionMax: high < ceiling ? high : undefined,
                })
            }
        />
    );
}
