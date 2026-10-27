"use client";

import { Select as SelectPrimitive } from "@base-ui/react/select";
import { FunnelIcon } from "lucide-react";

import { emptyToUndefined } from "@/features/image/filters/emptyToUndefined";
import { MultiSelectMenu } from "@/shared/components/MultiSelect";
import { SelectMenu } from "@/shared/components/Select";

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
            <ColumnFilterTrigger
                label={label}
                active={selected !== undefined}
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
            <ColumnFilterTrigger label={label} active={value !== undefined} />
        </SelectMenu>
    );
}

type ColumnFilterTriggerProps = {
    label: string;
    active: boolean;
};

// 글꼴·정렬은 머리 칸(th)을 따른다
function ColumnFilterTrigger({ label, active }: ColumnFilterTriggerProps) {
    return (
        <SelectPrimitive.Trigger
            aria-label={active ? `${label} 필터 적용됨` : `${label} 필터`}
            className="inline-flex items-center gap-1 rounded-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
        >
            {label}
            <FunnelIcon
                className={`size-3.5 ${active ? "fill-current text-foreground" : "text-muted-foreground"}`}
            />
        </SelectPrimitive.Trigger>
    );
}
