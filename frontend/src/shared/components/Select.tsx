"use client";

type SelectOption = {
    value: string;
    label: string;
};

type SelectProps = {
    label: string;
    options: SelectOption[];
    value: string;
    onChange: (value: string) => void;
};

export function Select({ label, options, value, onChange }: SelectProps) {
    return (
        <label className="flex flex-col gap-1">
            <span className="font-semibold">{label}</span>
            <select
                value={value}
                onChange={(e) => onChange(e.target.value)}
                className="rounded border px-3 py-2"
            >
                <option value="">전체</option>
                {options.map((option) => (
                    <option key={option.value} value={option.value}>
                        {option.label}
                    </option>
                ))}
            </select>
        </label>
    );
}
