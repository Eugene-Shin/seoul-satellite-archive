"use client";

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
        <label className="flex w-64 flex-col gap-1">
            <span className="font-semibold tabular-nums">
                최대 구름량: {current}%
            </span>
            <input
                type="range"
                min={0}
                max={MAX_CLOUD_COVER}
                step={1}
                value={current}
                disabled={disabled}
                onChange={(e) => {
                    const next = e.target.valueAsNumber;
                    onChange(next < MAX_CLOUD_COVER ? next : undefined);
                }}
            />
            <span className="min-h-5 text-sm text-zinc-500">{hint}</span>
        </label>
    );
}
