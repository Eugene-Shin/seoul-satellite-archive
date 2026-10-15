"use client";

import type { AcquiredRange, ImageSearchParams } from "@/features/image/types";
import { Input } from "@/shared/components/ui/input";
import { toKstDate } from "@/shared/utils/date";

type AcquiredDateFieldProps = {
    range: AcquiredRange;
    conditions: ImageSearchParams;
    onChange: (change: Partial<ImageSearchParams>) => void;
};

export function AcquiredDateField({
    range,
    conditions,
    onChange,
}: AcquiredDateFieldProps) {
    const firstDate = range.min ? toKstDate(range.min) : undefined;
    const lastDate = range.max ? toKstDate(range.max) : undefined;

    return (
        <fieldset className="flex flex-col gap-1">
            <legend className="font-semibold">촬영일</legend>
            <div className="flex items-center gap-2">
                <Input
                    type="date"
                    aria-label="촬영일 시작"
                    value={conditions.acquiredFrom ?? ""}
                    min={firstDate}
                    max={conditions.acquiredTo || lastDate}
                    onChange={(e) =>
                        onChange({ acquiredFrom: e.target.value || undefined })
                    }
                />
                <span>~</span>
                <Input
                    type="date"
                    aria-label="촬영일 종료"
                    value={conditions.acquiredTo ?? ""}
                    min={conditions.acquiredFrom || firstDate}
                    max={lastDate}
                    onChange={(e) =>
                        onChange({ acquiredTo: e.target.value || undefined })
                    }
                />
            </div>
        </fieldset>
    );
}
