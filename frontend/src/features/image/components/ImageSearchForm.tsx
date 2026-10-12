"use client";

import type {
    ImageSearchParams,
    ImageFilterOptionsResponse,
} from "@/features/image/types";
import { toKstDate } from "@/shared/utils/date";

type ImageSearchFormProps = {
    options: ImageFilterOptionsResponse;
    conditions: ImageSearchParams;
    onChange: (change: Partial<ImageSearchParams>) => void;
};

export function ImageSearchForm({
    options,
    conditions,
    onChange,
}: ImageSearchFormProps) {
    const { min, max } = options.acquiredRange;
    const firstDate = min ? toKstDate(min) : undefined;
    const lastDate = max ? toKstDate(max) : undefined;

    return (
        <form
            className="flex flex-col gap-4"
            onSubmit={(e) => e.preventDefault()}
        >
            <label className="flex flex-col gap-1">
                <span className="font-semibold">제목</span>
                <input
                    type="text"
                    value={conditions.keyword ?? ""}
                    onChange={(e) => onChange({ keyword: e.target.value })}
                    placeholder="영상 제목을 입력하세요"
                    className="rounded border px-3 py-2"
                />
            </label>

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
                            onChange({ acquiredFrom: e.target.value })
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
                            onChange({ acquiredTo: e.target.value })
                        }
                        className="rounded border px-3 py-2"
                    />
                </div>
            </fieldset>
        </form>
    );
}
