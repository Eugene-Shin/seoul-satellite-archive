"use client";

import type { ReactNode } from "react";

import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/shared/components/ui/select";

type MultiSelectOption<T> = {
    value: T;
    label: string;
};

type MultiSelectValueProps<T> = {
    options: MultiSelectOption<T>[];
    selected: T[];
    onChange: (selected: T[]) => void;
};

type MultiSelectMenuProps<T> = MultiSelectValueProps<T> & {
    children: ReactNode;
};

export function MultiSelectMenu<T>({
    options,
    selected,
    onChange,
    children,
}: MultiSelectMenuProps<T>) {
    return (
        <Select
            multiple
            items={options}
            value={selected}
            onValueChange={onChange}
        >
            {children}
            <SelectContent>
                {options.map((option) => (
                    <SelectItem key={String(option.value)} value={option.value}>
                        {option.label}
                    </SelectItem>
                ))}
            </SelectContent>
        </Select>
    );
}

type MultiSelectProps<T> = MultiSelectValueProps<T> & { label: string };

function summaryText(selected: unknown[]): string {
    return selected.length === 0 ? "전체" : `${selected.length}개 선택됨`;
}

export function MultiSelect<T>({ label, ...menuProps }: MultiSelectProps<T>) {
    return (
        <label className="flex flex-col gap-2">
            <span className="text-sm font-medium">{label}</span>
            <MultiSelectMenu {...menuProps}>
                <SelectTrigger className="w-full">
                    <SelectValue>{summaryText}</SelectValue>
                </SelectTrigger>
            </MultiSelectMenu>
        </label>
    );
}
