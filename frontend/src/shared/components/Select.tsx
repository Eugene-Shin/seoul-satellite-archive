"use client";

import type { ReactNode } from "react";

import {
    SelectContent,
    SelectItem,
    Select as SelectRoot,
    SelectTrigger,
    SelectValue,
} from "@/shared/components/ui/select";

type SelectOption<T> = {
    value: T;
    label: string;
};

type SelectValueProps<T> = {
    options: SelectOption<T>[];
    value: T | null;
    onChange: (value: T | null) => void;
};

type SelectMenuProps<T> = SelectValueProps<T> & { children: ReactNode };

export function SelectMenu<T>({
    options,
    value,
    onChange,
    children,
}: SelectMenuProps<T>) {
    const items = [{ value: null, label: "전체" }, ...options];

    return (
        <SelectRoot items={items} value={value} onValueChange={onChange}>
            {children}
            <SelectContent>
                {items.map((item) => (
                    <SelectItem key={String(item.value)} value={item.value}>
                        {item.label}
                    </SelectItem>
                ))}
            </SelectContent>
        </SelectRoot>
    );
}

type SelectProps<T> = SelectValueProps<T> & { label: string };

export function Select<T>({ label, ...menuProps }: SelectProps<T>) {
    return (
        <label className="flex flex-col gap-2">
            <span className="text-sm font-medium">{label}</span>
            <SelectMenu {...menuProps}>
                <SelectTrigger className="w-full">
                    <SelectValue />
                </SelectTrigger>
            </SelectMenu>
        </label>
    );
}
