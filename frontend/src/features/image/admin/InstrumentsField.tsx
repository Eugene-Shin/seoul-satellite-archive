"use client";

import { useState } from "react";

import {
    Combobox,
    ComboboxChip,
    ComboboxChips,
    ComboboxChipsInput,
    ComboboxContent,
    ComboboxEmpty,
    ComboboxItem,
    ComboboxList,
    ComboboxValue,
    useComboboxAnchor,
} from "@/shared/components/ui/combobox";

type InstrumentsFieldProps = {
    options: string[];
    value: string[];
    onChange: (value: string[]) => void;
};

// 기존 센서는 후보에서 고르고, 없는 센서는 입력 후 Enter로 추가한다
export function InstrumentsField({
    options,
    value,
    onChange,
}: InstrumentsFieldProps) {
    const anchor = useComboboxAnchor();
    const [query, setQuery] = useState("");
    const [highlighted, setHighlighted] = useState<string>();
    const typed = query.trim();
    const items = [...new Set([...options, ...value])];

    return (
        <Combobox
            multiple
            items={items}
            value={value}
            onValueChange={onChange}
            inputValue={query}
            onInputValueChange={setQuery}
            onItemHighlighted={setHighlighted}
        >
            <ComboboxChips ref={anchor}>
                <ComboboxValue>
                    {(selected: string[]) =>
                        selected.map((instrument) => (
                            <ComboboxChip key={instrument}>
                                {instrument}
                            </ComboboxChip>
                        ))
                    }
                </ComboboxValue>
                <ComboboxChipsInput
                    aria-label="센서"
                    onKeyDown={(event) => {
                        // 강조된 후보가 없으면 Base UI는 Enter를 폼 제출로 넘긴다. 그때 입력한 글자를 추가한다
                        if (
                            event.key === "Enter" &&
                            highlighted === undefined &&
                            typed !== ""
                        ) {
                            event.preventDefault();
                            if (!value.includes(typed)) {
                                onChange([...value, typed]);
                            }
                            setQuery("");
                        }
                    }}
                />
            </ComboboxChips>
            <ComboboxContent anchor={anchor}>
                <ComboboxEmpty>
                    {typed ? `Enter로 "${typed}" 추가` : "후보가 없습니다."}
                </ComboboxEmpty>
                <ComboboxList>
                    {(instrument: string) => (
                        <ComboboxItem key={instrument} value={instrument}>
                            {instrument}
                        </ComboboxItem>
                    )}
                </ComboboxList>
            </ComboboxContent>
        </Combobox>
    );
}
