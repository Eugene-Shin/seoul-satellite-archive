"use client";

import { Select as SelectPrimitive } from "@base-ui/react/select";
import {
    ArrowDownIcon,
    ArrowUpIcon,
    FunnelIcon,
    FunnelXIcon,
} from "lucide-react";
import type { ComponentProps, ReactNode } from "react";

import { emptyToUndefined } from "@/features/image/filters/emptyToUndefined";
import { imageSortSchema } from "@/features/image/schemas";
import {
    type ImageSortField,
    SORT_OPTIONS,
    type SortDirection,
} from "@/features/image/sortOptions";
import type { ImageSort } from "@/features/image/types";
import { MultiSelectMenu } from "@/shared/components/MultiSelect";
import { SelectMenu } from "@/shared/components/Select";
import { Button } from "@/shared/components/ui/button";
import {
    Popover,
    PopoverContent,
    PopoverTrigger,
} from "@/shared/components/ui/popover";

type FilterOption<T> = {
    value: T;
    label: string;
};

export function toOptions(
    values: string[] | undefined,
): FilterOption<string>[] {
    return (values ?? []).map((value) => ({ value, label: value }));
}

type MultiColumnFilterProps<T> = {
    label: string;
    options: FilterOption<T>[];
    selected: T[] | undefined;
    onChange: (selected: T[] | undefined) => void;
};

export function MultiColumnFilter<T>({
    label,
    options,
    selected,
    onChange,
}: MultiColumnFilterProps<T>) {
    return (
        <MultiSelectMenu
            options={options}
            selected={selected ?? []}
            onChange={(values) => onChange(emptyToUndefined(values))}
        >
            <SelectPrimitive.Trigger
                render={
                    <ColumnHeaderButton
                        label={label}
                        active={selected !== undefined}
                    />
                }
            />
        </MultiSelectMenu>
    );
}

type SelectColumnFilterProps<T> = {
    label: string;
    options: FilterOption<T>[];
    value: T | undefined;
    onChange: (value: T | undefined) => void;
};

export function SelectColumnFilter<T>({
    label,
    options,
    value,
    onChange,
}: SelectColumnFilterProps<T>) {
    return (
        <SelectMenu
            options={options}
            value={value ?? null}
            onChange={(next) => onChange(next ?? undefined)}
        >
            <SelectPrimitive.Trigger
                render={
                    <ColumnHeaderButton
                        label={label}
                        active={value !== undefined}
                    />
                }
            />
        </SelectMenu>
    );
}

// default: 정렬을 고르지 않았을 때 서버가 쓰는 기본 정렬
type SortMark = SortDirection | "default";

type PopoverColumnFilterProps = {
    label: string;
    active: boolean;
    sortMark?: SortMark;
    onReset: () => void;
    children: ReactNode;
};

export function PopoverColumnFilter({
    label,
    active,
    sortMark,
    onReset,
    children,
}: PopoverColumnFilterProps) {
    const sorted = sortMark === "asc" || sortMark === "desc";

    return (
        <Popover>
            <PopoverTrigger
                render={
                    <ColumnHeaderButton
                        label={label}
                        active={active}
                        sortMark={sortMark}
                    />
                }
            />
            <PopoverContent align="start" className="w-80 gap-4 p-4">
                {children}
                <Button
                    variant="outline"
                    size="sm"
                    disabled={!active && !sorted}
                    onClick={onReset}
                    className="self-end"
                >
                    <FunnelXIcon />이 열 초기화
                </Button>
            </PopoverContent>
        </Popover>
    );
}

type SortToggleProps = {
    field: ImageSortField;
    value: ImageSort | undefined;
    disabled?: boolean;
    onChange: (sorts: ImageSort[] | undefined) => void;
};

// 한 번에 한 열만 정렬한다. 고른 것을 다시 누르면 정렬을 푼다
export function SortToggle({
    field,
    value,
    disabled,
    onChange,
}: SortToggleProps) {
    const sorts = imageSortSchema.options.filter(
        (sort) => SORT_OPTIONS[sort].field === field,
    );

    return (
        <fieldset disabled={disabled} className="flex flex-col gap-2">
            <legend className="mb-2 text-sm font-medium">정렬</legend>
            <div className="flex gap-2">
                {sorts.map((sort) => {
                    const selected = sort === value;
                    return (
                        <Button
                            key={sort}
                            variant={selected ? "secondary" : "outline"}
                            size="sm"
                            aria-pressed={selected}
                            onClick={() =>
                                onChange(selected ? undefined : [sort])
                            }
                        >
                            {SORT_OPTIONS[sort].label}
                        </Button>
                    );
                })}
            </div>
        </fieldset>
    );
}

type ColumnHeaderButtonProps = ComponentProps<"button"> & {
    label: string;
    active: boolean;
    sortMark?: SortMark;
};

// 모양만 담당한다. 여닫는 동작은 감싸는 Trigger가 render로 얹는다
// 글꼴·정렬은 머리 칸(th)을 따른다
function ColumnHeaderButton({
    label,
    active,
    sortMark,
    ...props
}: ColumnHeaderButtonProps) {
    const SortIcon = sortMark === "asc" ? ArrowUpIcon : ArrowDownIcon;

    return (
        <button
            type="button"
            aria-label={active ? `${label} 필터 적용됨` : `${label} 필터`}
            className="inline-flex items-center gap-1 rounded-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
            {...props}
        >
            {label}
            {sortMark && (
                <SortIcon
                    className={`size-3.5 ${sortMark === "default" ? "text-muted-foreground" : "text-foreground"}`}
                />
            )}
            <FunnelIcon
                className={`size-3.5 ${active ? "fill-current text-foreground" : "text-muted-foreground"}`}
            />
        </button>
    );
}
