"use client";

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

type SelectProps<T> = {
    label: string;
    options: SelectOption<T>[];
    value: T | null;
    onChange: (value: T | null) => void;
};

export function Select<T>({ label, options, value, onChange }: SelectProps<T>) {
    const items = [{ value: null, label: "전체" }, ...options];

    return (
        <label className="flex flex-col gap-2">
            <span className="text-sm font-medium">{label}</span>
            <SelectRoot items={items} value={value} onValueChange={onChange}>
                <SelectTrigger className="w-full">
                    <SelectValue />
                </SelectTrigger>
                <SelectContent>
                    {items.map((item) => (
                        <SelectItem key={String(item.value)} value={item.value}>
                            {item.label}
                        </SelectItem>
                    ))}
                </SelectContent>
            </SelectRoot>
        </label>
    );
}
