"use client";

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

type MultiSelectProps<T> = {
    label: string;
    options: MultiSelectOption<T>[];
    selected: T[];
    onChange: (selected: T[]) => void;
};

function summaryText(selected: unknown[]): string {
    return selected.length === 0 ? "전체" : `${selected.length}개 선택됨`;
}

export function MultiSelect<T>({
    label,
    options,
    selected,
    onChange,
}: MultiSelectProps<T>) {
    return (
        <label className="flex flex-col gap-2">
            <span className="text-sm font-medium">{label}</span>
            <Select
                multiple
                items={options}
                value={selected}
                onValueChange={onChange}
            >
                <SelectTrigger className="w-full">
                    <SelectValue>{summaryText}</SelectValue>
                </SelectTrigger>
                <SelectContent>
                    {options.map((option) => (
                        <SelectItem
                            key={String(option.value)}
                            value={option.value}
                        >
                            {option.label}
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>
        </label>
    );
}
