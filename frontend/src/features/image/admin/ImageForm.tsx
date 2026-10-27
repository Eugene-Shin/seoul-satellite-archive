"use client";

import { type ReactNode, useId, useState } from "react";

import {
    EMPTY_IMAGE_FORM,
    type ImageFormValues,
    missingRequiredFields,
    type RequiredField,
} from "@/features/image/admin/imageFormValues";
import { InstrumentsField } from "@/features/image/admin/InstrumentsField";
import {
    ORBIT_DIRECTION_NAMES,
    SENSOR_TYPE_NAMES,
} from "@/features/image/imageText";
import { useFilterOptions } from "@/features/image/queries";
import { ErrorMessage } from "@/shared/components/ErrorMessage";
import { Button } from "@/shared/components/ui/button";
import { DialogClose, DialogFooter } from "@/shared/components/ui/dialog";
import { Input } from "@/shared/components/ui/input";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/shared/components/ui/select";
import { Textarea } from "@/shared/components/ui/textarea";

// 초점을 옮길 입력 요소 (Select는 트리거 버튼)
const FOCUSABLE =
    '[data-slot="input"], [data-slot="textarea"], [data-slot="select-trigger"]';

type ImageFormProps = {
    pending: boolean;
    error: unknown;
    onSubmit: (form: ImageFormValues) => void;
};

export function ImageForm({ pending, error, onSubmit }: ImageFormProps) {
    const { data: options } = useFilterOptions();
    const [form, setForm] = useState(EMPTY_IMAGE_FORM);
    // 한 번 [등록]을 누른 뒤부터 빈 필수 칸을 표시한다
    const [submitted, setSubmitted] = useState(false);
    const satelliteListId = useId();

    const missing = submitted ? missingRequiredFields(form) : [];

    function invalid(field: RequiredField): boolean {
        return missing.includes(field);
    }

    function change(changes: Partial<ImageFormValues>) {
        setForm((current) => ({ ...current, ...changes }));
    }

    return (
        <form
            noValidate
            onSubmit={(event) => {
                event.preventDefault();
                setSubmitted(true);
                const [firstMissing] = missingRequiredFields(form);
                if (firstMissing) {
                    event.currentTarget
                        .querySelector(`[data-field="${firstMissing}"]`)
                        ?.querySelector<HTMLElement>(FOCUSABLE)
                        ?.focus();
                    return;
                }
                onSubmit(form);
            }}
            className="flex min-h-0 flex-col gap-4"
        >
            {/* 입력칸만 스크롤해서 제목·닫기·버튼은 항상 보이게. 음수 여백은 초점 테두리가 잘리지 않게 */}
            <div className="-mx-4 flex min-h-0 flex-col gap-4 overflow-y-auto px-4 py-1">
                <div className="grid gap-4 sm:grid-cols-2">
                    <FormSelect
                        field="providerCode"
                        label="제공 기관"
                        required
                        invalid={invalid("providerCode")}
                        options={(options?.providers ?? []).map((provider) => ({
                            value: provider.code,
                            label: provider.name,
                        }))}
                        value={form.providerCode}
                        onChange={(providerCode) => change({ providerCode })}
                    />
                    <Field
                        field="sourceId"
                        label="원본 ID"
                        required
                        invalid={invalid("sourceId")}
                    >
                        <Input
                            required
                            aria-invalid={invalid("sourceId")}
                            value={form.sourceId}
                            onChange={(e) =>
                                change({ sourceId: e.target.value })
                            }
                        />
                    </Field>
                    <Field
                        field="title"
                        label="제목"
                        required
                        invalid={invalid("title")}
                    >
                        <Input
                            required
                            aria-invalid={invalid("title")}
                            maxLength={30}
                            value={form.title}
                            onChange={(e) => change({ title: e.target.value })}
                        />
                    </Field>
                    <Field
                        field="satellite"
                        label="위성"
                        required
                        invalid={invalid("satellite")}
                    >
                        <Input
                            required
                            aria-invalid={invalid("satellite")}
                            list={satelliteListId}
                            value={form.satellite}
                            onChange={(e) =>
                                change({ satellite: e.target.value })
                            }
                        />
                        <datalist id={satelliteListId}>
                            {options?.satellites.map((satellite) => (
                                <option key={satellite} value={satellite} />
                            ))}
                        </datalist>
                    </Field>
                    <Field label="센서" hint="입력 후 Enter로 추가">
                        <InstrumentsField
                            options={options?.instruments ?? []}
                            value={form.instruments}
                            onChange={(instruments) => change({ instruments })}
                        />
                    </Field>
                    <FormSelect
                        field="sensorType"
                        label="유형"
                        required
                        invalid={invalid("sensorType")}
                        options={(options?.sensorTypes ?? []).map((type) => ({
                            value: type,
                            label: SENSOR_TYPE_NAMES[type],
                        }))}
                        value={form.sensorType}
                        onChange={(sensorType) => {
                            if (sensorType) {
                                change({
                                    sensorType,
                                    cloudCover:
                                        sensorType === "SAR"
                                            ? ""
                                            : form.cloudCover,
                                });
                            }
                        }}
                    />
                    <Field
                        field="acquiredAt"
                        label="촬영 일시"
                        required
                        invalid={invalid("acquiredAt")}
                        hint="한국 시간(KST) 기준"
                    >
                        <Input
                            type="datetime-local"
                            required
                            aria-invalid={invalid("acquiredAt")}
                            value={form.acquiredAt}
                            onChange={(e) =>
                                change({ acquiredAt: e.target.value })
                            }
                        />
                    </Field>
                    <Field label="해상도 (m)">
                        <Input
                            type="number"
                            min={0}
                            step={0.01}
                            value={form.gsdMeters}
                            onChange={(e) =>
                                change({ gsdMeters: e.target.value })
                            }
                        />
                    </Field>
                    <Field
                        label="구름량 (%)"
                        hint={
                            form.sensorType === "SAR"
                                ? "SAR 영상은 구름량 정보가 없습니다."
                                : undefined
                        }
                    >
                        <Input
                            type="number"
                            min={0}
                            max={100}
                            step={0.01}
                            disabled={form.sensorType === "SAR"}
                            value={form.cloudCover}
                            onChange={(e) =>
                                change({ cloudCover: e.target.value })
                            }
                        />
                    </Field>
                    <FormSelect
                        label="궤도"
                        emptyLabel="없음"
                        options={(options?.orbitDirections ?? []).map(
                            (direction) => ({
                                value: direction,
                                label: ORBIT_DIRECTION_NAMES[direction],
                            }),
                        )}
                        value={form.orbitDirection}
                        onChange={(orbitDirection) =>
                            change({ orbitDirection })
                        }
                    />
                    <Field
                        field="url"
                        label="메타데이터 URL"
                        required
                        invalid={invalid("url")}
                    >
                        <Input
                            required
                            aria-invalid={invalid("url")}
                            value={form.url}
                            onChange={(e) => change({ url: e.target.value })}
                        />
                    </Field>
                    <Field label="원본 갱신 일시" hint="한국 시간(KST) 기준">
                        <Input
                            type="datetime-local"
                            value={form.sourceUpdatedAt}
                            onChange={(e) =>
                                change({ sourceUpdatedAt: e.target.value })
                            }
                        />
                    </Field>
                </div>
                <Field
                    field="downloadUrls"
                    label="다운로드 URL"
                    required
                    invalid={invalid("downloadUrls")}
                    hint="Enter로 링크를 구분하여 입력"
                >
                    <Textarea
                        required
                        aria-invalid={invalid("downloadUrls")}
                        value={form.downloadUrls}
                        onChange={(e) =>
                            change({ downloadUrls: e.target.value })
                        }
                    />
                </Field>
                <Field
                    field="footprint"
                    label="footprint"
                    required
                    invalid={invalid("footprint")}
                    hint="GeoJSON MultiPolygon"
                >
                    <Textarea
                        required
                        aria-invalid={invalid("footprint")}
                        className="font-mono"
                        value={form.footprint}
                        onChange={(e) => change({ footprint: e.target.value })}
                    />
                </Field>
                <Field label="원본 메타데이터 (JSON)">
                    <Textarea
                        className="font-mono"
                        value={form.rawMetadata}
                        onChange={(e) =>
                            change({ rawMetadata: e.target.value })
                        }
                    />
                </Field>
            </div>
            <ErrorMessage error={error} />
            <DialogFooter>
                <DialogClose render={<Button variant="outline" />}>
                    취소
                </DialogClose>
                <Button type="submit" disabled={pending}>
                    {pending ? "등록 중..." : "등록"}
                </Button>
            </DialogFooter>
        </form>
    );
}

type FieldProps = {
    field?: RequiredField;
    label: string;
    required?: boolean;
    invalid?: boolean;
    hint?: string;
    children: ReactNode;
};

function Field({
    field,
    label,
    required,
    invalid,
    hint,
    children,
}: FieldProps) {
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

type FormSelectProps<T extends string> = {
    field?: RequiredField;
    label: string;
    required?: boolean;
    invalid?: boolean;
    emptyLabel?: string;
    options: { value: T; label: string }[];
    value: T | null;
    onChange: (value: T | null) => void;
};

// 공용 Select는 [전체]가 붙어 있어서 폼에선 쓰지 않는다
function FormSelect<T extends string>({
    field,
    label,
    required,
    invalid,
    emptyLabel,
    options,
    value,
    onChange,
}: FormSelectProps<T>) {
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
                value={value}
                onValueChange={onChange}
            >
                <SelectTrigger aria-invalid={invalid} className="w-full">
                    <SelectValue placeholder="선택" />
                </SelectTrigger>
                <SelectContent>
                    {items.map((item) => (
                        <SelectItem key={String(item.value)} value={item.value}>
                            {item.label}
                        </SelectItem>
                    ))}
                </SelectContent>
            </Select>
        </Field>
    );
}
