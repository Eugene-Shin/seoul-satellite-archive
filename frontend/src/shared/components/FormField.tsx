"use client";

import type { ReactNode } from "react";

import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/shared/components/ui/select";

// 등록·수정 폼 팝업의 크기. 입력칸만 스크롤하고 제목·버튼은 고정한다
export const FORM_DIALOG_CLASS =
    "flex max-h-[80vh] flex-col shadow-2xl sm:max-w-2xl";

// 초점을 옮길 입력 요소 (Select는 트리거 버튼)
const FOCUSABLE =
    '[data-slot="input"], [data-slot="textarea"], [data-slot="select-trigger"]';

// 제출할 때 첫 번째 빈 필수 칸으로 커서를 옮긴다. 칸은 Field의 data-field로 찾는다
export function focusField(form: HTMLFormElement, field: string) {
    form.querySelector(`[data-field="${field}"]`)
        ?.querySelector<HTMLElement>(FOCUSABLE)
        ?.focus();
}

// F는 폼의 칸 이름 타입. 폼마다 자기 칸 이름으로 고정해 쓰면 오타를 컴파일할 때 잡는다
export type FieldProps<F extends string> = {
    // label에 data-field로 붙는다. 폼이 제출할 때 빈 필수 칸을 이 이름으로 찾아 커서를 옮긴다
    field?: F;
    label: string;
    required?: boolean;
    invalid?: boolean;
    hint?: string;
    children: ReactNode;
};

export function Field<F extends string>({
    field,
    label,
    required,
    invalid,
    hint,
    children,
}: FieldProps<F>) {
    return (
        <label data-field={field} className="flex flex-col gap-2">
            <span className="flex items-baseline justify-between gap-2 text-sm font-medium">
                <span>
                    {label}
                    {required && <span className="text-destructive"> *</span>}
                </span>
                {hint && (
                    <span className="text-xs font-normal text-muted-foreground">
                        {hint}
                    </span>
                )}
            </span>
            {children}
            {invalid && (
                <span className="text-xs text-destructive">
                    필수 입력입니다.
                </span>
            )}
        </label>
    );
}

export type FormSelectProps<T extends string | number, F extends string> = {
    field?: F;
    label: string;
    required?: boolean;
    invalid?: boolean;
    disabled?: boolean;
    emptyLabel?: string;
    options: { value: T; label: string }[];
    value: T | null;
    onChange: (value: T | null) => void;
};

// 공용 Select는 [전체]가 붙어 있어서 폼에선 쓰지 않는다
export function FormSelect<T extends string | number, F extends string>({
    field,
    label,
    required,
    invalid,
    disabled,
    emptyLabel,
    options,
    value,
    onChange,
}: FormSelectProps<T, F>) {
    const items = emptyLabel
        ? [{ value: null, label: emptyLabel }, ...options]
        : options;

    return (
        <Field
            field={field}
            label={label}
            required={required}
            invalid={invalid}
        >
            <Select
                items={items}
                required={required}
                disabled={disabled}
                value={value}
                onValueChange={onChange}
            >
                <SelectTrigger aria-invalid={invalid} className="w-full">
                    <SelectValue placeholder="선택" />
                </SelectTrigger>
                <SelectContent>
                    {items.map((item) => (
                        <SelectItem
                            key={String(item.value)}
                            value={item.value}
                            title={item.label}
                        >
                            <span className="truncate">{item.label}</span>
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>
        </Field>
    );
}
