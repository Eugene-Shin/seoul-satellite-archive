"use client";

import { ArrowUpIcon, XIcon } from "lucide-react";

import { imageSortSchema } from "@/features/image/schemas";
import type { ImageSort } from "@/features/image/types";
import { IconButton } from "@/shared/components/IconButton";
import { Button } from "@/shared/components/ui/button";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/shared/components/ui/select";

const SORT_OPTIONS: Record<ImageSort, { label: string; field: string }> = {
    ACQUIRED_AT_DESC: { label: "촬영일 최신순", field: "acquiredAt" },
    ACQUIRED_AT_ASC: { label: "촬영일 오래된순", field: "acquiredAt" },
    CLOUD_COVER_ASC: { label: "구름량 적은순", field: "cloudCover" },
    CLOUD_COVER_DESC: { label: "구름량 많은순", field: "cloudCover" },
};

const SORTS = imageSortSchema.options;

const SORT_ITEMS = SORTS.map((sort) => ({
    value: sort,
    label: SORT_OPTIONS[sort].label,
}));

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
                        <Select
                            items={SORT_ITEMS}
                            value={sort}
                            onValueChange={(next) => {
                                if (next) {
                                    replace(index, next);
                                }
                            }}
                        >
                            <SelectTrigger
                                aria-label={`${index + 1}순위 정렬 기준`}
                                className="w-40"
                            >
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                {availableSorts(sort).map((option) => (
                                    <SelectItem key={option} value={option}>
                                        {SORT_OPTIONS[option].label}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                        <IconButton
                            label="우선순위 올리기"
                            disabled={index === 0}
                            onClick={() => moveUp(index)}
                        >
                            <ArrowUpIcon />
                        </IconButton>
                        <IconButton
                            label="정렬 기준 삭제"
                            onClick={() => remove(index)}
                        >
                            <XIcon />
                        </IconButton>
                    </li>
                ))}
            </ol>
            <Button
                variant="outline"
                disabled={!nextSort}
                onClick={add}
                className="self-start"
            >
                + 정렬 기준 추가
            </Button>
        </fieldset>
    );
}
