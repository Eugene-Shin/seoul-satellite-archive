"use client";

type MultiSelectOption = {
    value: string;
    label: string;
};

type MultiSelectProps = {
    label: string;
    options: MultiSelectOption[];
    selected: string[];
    onChange: (selected: string[]) => void;
};

export function MultiSelect({
    label,
    options,
    selected,
    onChange,
}: MultiSelectProps) {
    function toggle(value: string) {
        onChange(
            selected.includes(value)
                ? selected.filter((v) => v !== value)
                : [...selected, value],
        );
    }

    const summary =
        selected.length === 0 ? "전체" : `${selected.length}개 선택됨`;

    return (
        <details className="relative">
            <summary className="cursor-pointer rounded border px-3 py-2">
                {label}: {summary}
            </summary>
            <ul className="absolute z-10 mt-1 max-h-60 min-w-full overflow-auto rounded border bg-background p-2 shadow">
                {options.map((option) => (
                    <li key={option.value}>
                        <label className="flex items-center gap-2 whitespace-nowrap">
                            <input
                                type="checkbox"
                                checked={selected.includes(option.value)}
                                onChange={() => toggle(option.value)}
                            />
                            {option.label}
                        </label>
                    </li>
                ))}
            </ul>
        </details>
    );
}
