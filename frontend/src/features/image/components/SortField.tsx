"use client";

import type { ImageSort } from "@/features/image/types";

const SORT_OPTIONS: Record<ImageSort, { label: string; field: string }> = {
    ACQUIRED_AT_DESC: { label: "촬영일 최신순", field: "acquiredAt" },
    ACQUIRED_AT_ASC: { label: "촬영일 오래된순", field: "acquiredAt" },
    CLOUD_COVER_ASC: { label: "구름량 적은순", field: "cloudCover" },
    CLOUD_COVER_DESC: { label: "구름량 많은순", field: "cloudCover" },
};

const SORTS = Object.keys(SORT_OPTIONS) as ImageSort[];

type SortFieldProps = {
    value: ImageSort[];
    onChange: (sorts: ImageSort[]) => void;
};

export function SortField({ value, onChange }: SortFieldProps) {
    const usedFields = value.map((sort) => SORT_OPTIONS[sort].field);
    const nextSort = SORTS.find(
        (sort) => !usedFields.includes(SORT_OPTIONS[sort].field),
    );

    function replace(index: number, sort: ImageSort) {
        onChange(value.map((current, i) => (i === index ? sort : current)));
    }

    function remove(index: number) {
        onChange(value.filter((_, i) => i !== index));
    }

    function moveUp(index: number) {
        const next = [...value];
        [next[index - 1], next[index]] = [next[index], next[index - 1]];
        onChange(next);
    }

    function add() {
        if (nextSort) {
            onChange([...value, nextSort]);
        }
    }

    function availableSorts(current: ImageSort): ImageSort[] {
        const currentField = SORT_OPTIONS[current].field;
        return SORTS.filter((option) => {
            const field = SORT_OPTIONS[option].field;
            return field === currentField || !usedFields.includes(field);
        });
    }

    function changeAt(index: number, selectedValue: string) {
        const selected = SORTS.find((option) => option === selectedValue);
        if (selected) {
            replace(index, selected);
        }
    }

    return (
        <fieldset className="flex flex-col gap-2">
            <legend className="font-semibold">정렬</legend>
            <p className="text-sm text-zinc-500">
                정렬 기준이 없으면 촬영일 최신순으로 정렬됩니다.
            </p>
            <ol className="flex flex-col gap-2">
                {value.map((sort, index) => (
                    <li key={sort} className="flex items-center gap-2">
                        <span className="w-12">{index + 1}순위</span>
                        <select
                            aria-label={`${index + 1}순위 정렬 기준`}
                            value={sort}
                            onChange={(e) => changeAt(index, e.target.value)}
                            className="w-40 rounded border px-2 py-2"
                        >
                            {availableSorts(sort).map((option) => (
                                <option key={option} value={option}>
                                    {SORT_OPTIONS[option].label}
                                </option>
                            ))}
                        </select>
                        <button
                            type="button"
                            aria-label="우선순위 올리기"
                            disabled={index === 0}
                            onClick={() => moveUp(index)}
                            className="rounded border px-2 py-1 disabled:opacity-40"
                        >
                            ↑
                        </button>
                        <button
                            type="button"
                            aria-label="정렬 기준 삭제"
                            onClick={() => remove(index)}
                            className="rounded border px-2 py-1"
                        >
                            ✕
                        </button>
                    </li>
                ))}
            </ol>
            <button
                type="button"
                disabled={!nextSort}
                onClick={add}
                className="self-start rounded border px-3 py-2 disabled:opacity-40"
            >
                + 정렬 기준 추가
            </button>
        </fieldset>
    );
}
