"use client";

import type { ImageSearchParams, Range } from "@/features/image/types";
import { toKstDate } from "@/shared/utils/date";

type AcquiredDateFieldProps = {
    range: Range<string>;
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
                <input
                    type="date"
                    aria-label="촬영일 시작"
                    value={conditions.acquiredFrom ?? ""}
                    min={firstDate}
                    max={conditions.acquiredTo || lastDate}
                    onChange={(e) =>
                        onChange({ acquiredFrom: e.target.value || undefined })
                    }
                    className="rounded border px-3 py-2"
                />
                <span>~</span>
                <input
                    type="date"
                    aria-label="촬영일 종료"
                    value={conditions.acquiredTo ?? ""}
                    min={conditions.acquiredFrom || firstDate}
                    max={lastDate}
                    onChange={(e) =>
                        onChange({ acquiredTo: e.target.value || undefined })
                    }
                    className="rounded border px-3 py-2"
                />
            </div>
        </fieldset>
    );
}
