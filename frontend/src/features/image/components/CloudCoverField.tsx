"use client";

import { Slider } from "@/shared/components/ui/slider";

const MAX_CLOUD_COVER = 100;

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
    const current = value ?? MAX_CLOUD_COVER;
    const hint = disabled ? "SAR 영상은 구름량 정보가 없습니다." : null;

    return (
        <label className="flex w-full flex-col gap-3">
            <span className="font-semibold tabular-nums">
                최대 구름량: {current}%
            </span>
            <Slider
                min={0}
                max={MAX_CLOUD_COVER}
                step={1}
                value={[current]}
                disabled={disabled}
                onValueChange={([next]) =>
                    onChange(next < MAX_CLOUD_COVER ? next : undefined)
                }
                getAriaLabel={() => "최대 구름량"}
            />
            <span className="min-h-5 text-sm text-zinc-500">{hint}</span>
        </label>
    );
}
