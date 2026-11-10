"use client";

import { type ReactNode, useId, useState } from "react";

import {
    type ImageFormValues,
    isSameForm,
    missingRequiredFields,
    type RequiredField,
    type SarFormValues,
} from "@/features/image/admin/imageFormValues";
import { InstrumentsField } from "@/features/image/admin/InstrumentsField";
import {
    collectionOptionLabel,
    LOOK_DIRECTION_NAMES,
    ORBIT_DIRECTION_NAMES,
    providerName,
    SENSOR_TYPE_NAMES,
} from "@/features/image/imageText";
import { useCollections, useFilterOptions } from "@/features/image/queries";
import {
    lookDirectionSchema,
    polarizationSchema,
} from "@/features/image/schemas";
import type { Polarization } from "@/features/image/types";
import { ErrorMessage } from "@/shared/components/ErrorMessage";
import { MultiSelectMenu } from "@/shared/components/MultiSelect";
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

export const IMAGE_FORM_DIALOG_CLASS =
    "flex max-h-[80vh] flex-col shadow-2xl sm:max-w-2xl";

// 기존 데이터에 밀리초가 있어서, 수정 후 저장해도 그대로 남게
const MILLISECOND_STEP = 0.001;

type ImageFormProps = {
    initialValues: ImageFormValues;
    submitLabel: string;
    pending: boolean;
    error: unknown;
    onSubmit: (form: ImageFormValues) => void;
};

export function ImageForm({
    initialValues,
    submitLabel,
    pending,
    error,
    onSubmit,
}: ImageFormProps) {
    const { data: options } = useFilterOptions();
    const { data: collections } = useCollections();
    const [form, setForm] = useState(initialValues);
    const unchanged = isSameForm(form, initialValues);
    // 한 번 제출을 누른 뒤부터 빈 필수 칸을 표시한다
    const [submitted, setSubmitted] = useState(false);
    const satelliteListId = useId();

    const missing = submitted ? missingRequiredFields(form) : [];

    const providers = options?.providers ?? [];
    const collectionOptions = (collections ?? []).map((collection) => ({
        value: collection.collectionId,
        label: collectionOptionLabel(collection, providers),
    }));
    // 영상의 제공 기관은 고른 컬렉션의 제공 기관이다
    const selectedCollection = collections?.find(
        (collection) => collection.collectionId === form.collectionId,
    );
    const selectedProviderName = selectedCollection
        ? providerName(selectedCollection.providerCode, providers)
        : "";

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
                    <Field label="제공 기관">
                        <Input readOnly value={selectedProviderName} />
                    </Field>
                    <FormSelect
                        field="collectionId"
                        label="컬렉션"
                        required
                        invalid={invalid("collectionId")}
                        options={collectionOptions}
                        value={form.collectionId}
                        onChange={(collectionId) => change({ collectionId })}
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
                    <Field label="원본 갱신 일시" hint="한국 시간(KST) 기준">
                        <Input
                            type="datetime-local"
                            step={MILLISECOND_STEP}
                            value={form.sourceUpdatedAt}
                            onChange={(e) =>
                                change({ sourceUpdatedAt: e.target.value })
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
                        // 유형별 칸(구름량, SAR 정보)은 값을 남겨 두고 요청을 만들 때 뺀다
                        onChange={(sensorType) => {
                            if (sensorType) {
                                change({ sensorType });
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
                            step={MILLISECOND_STEP}
                            required
                            aria-invalid={invalid("acquiredAt")}
                            value={form.acquiredAt}
                            onChange={(e) =>
                                change({ acquiredAt: e.target.value })
                            }
                        />
                    </Field>
                    <Field label="촬영 종료 일시" hint="한국 시간(KST) 기준">
                        <Input
                            type="datetime-local"
                            step={MILLISECOND_STEP}
                            min={form.acquiredAt || undefined}
                            value={form.acquiredEndAt}
                            onChange={(e) =>
                                change({ acquiredEndAt: e.target.value })
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
                            value={
                                form.sensorType === "SAR" ? "" : form.cloudCover
                            }
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
                </div>
                {form.sensorType === "SAR" && (
                    <SarFields
                        value={form.sar}
                        onChange={(sar) => change({ sar })}
                    />
                )}
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
                <Button type="submit" disabled={pending || unchanged}>
                    {pending ? `${submitLabel} 중...` : submitLabel}
                </Button>
            </DialogFooter>
        </form>
    );
}

type SarFieldsProps = {
    value: SarFormValues;
    onChange: (value: SarFormValues) => void;
};

function SarFields({ value, onChange }: SarFieldsProps) {
    function change(changes: Partial<SarFormValues>) {
        onChange({ ...value, ...changes });
    }

    return (
        <section className="flex flex-col gap-4">
            <h3 className="font-semibold">SAR 정보</h3>
            <div className="grid gap-4 sm:grid-cols-2">
                <Field label="상대 궤도">
                    <Input
                        type="number"
                        min={1}
                        step={1}
                        value={value.relativeOrbit}
                        onChange={(e) =>
                            change({ relativeOrbit: e.target.value })
                        }
                    />
                </Field>
                <Field label="프레임">
                    <Input
                        type="number"
                        min={0}
                        step={1}
                        value={value.frame}
                        onChange={(e) => change({ frame: e.target.value })}
                    />
                </Field>
                <Field label="촬영 모드" hint="예: IW, SM">
                    <Input
                        maxLength={10}
                        value={value.instrumentMode}
                        onChange={(e) =>
                            change({ instrumentMode: e.target.value })
                        }
                    />
                </Field>
                <FormSelect
                    label="관측 방향"
                    emptyLabel="없음"
                    options={lookDirectionSchema.options.map((direction) => ({
                        value: direction,
                        label: LOOK_DIRECTION_NAMES[direction],
                    }))}
                    value={value.lookDirection}
                    onChange={(lookDirection) => change({ lookDirection })}
                />
                <Field label="편파">
                    <MultiSelectMenu
                        options={polarizationSchema.options.map(
                            (polarization) => ({
                                value: polarization,
                                label: polarization,
                            }),
                        )}
                        selected={value.polarizations}
                        // 고른 순서와 상관없이 HH, HV, VH, VV 순서로 둬서 같은 조합이면 같은 값이 되게
                        onChange={(selected: Polarization[]) =>
                            change({
                                polarizations:
                                    polarizationSchema.options.filter(
                                        (option) => selected.includes(option),
                                    ),
                            })
                        }
                    >
                        <SelectTrigger className="w-full">
                            <SelectValue>
                                {(selected: Polarization[]) =>
                                    selected.length > 0
                                        ? selected.join(", ")
                                        : "없음"
                                }
                            </SelectValue>
                        </SelectTrigger>
                    </MultiSelectMenu>
                </Field>
                <Field label="입사각 (°)" hint="장면 중심">
                    <Input
                        type="number"
                        min={0}
                        max={90}
                        step={0.01}
                        value={value.incidenceAngle}
                        onChange={(e) =>
                            change({ incidenceAngle: e.target.value })
                        }
                    />
                </Field>
                <Field label="거리 방향 해상도 (m)" hint="진행 방향에 수직">
                    <Input
                        type="number"
                        min={0}
                        step={0.01}
                        value={value.resolutionRangeMeters}
                        onChange={(e) =>
                            change({ resolutionRangeMeters: e.target.value })
                        }
                    />
                </Field>
                <Field label="방위 방향 해상도 (m)" hint="진행 방향">
                    <Input
                        type="number"
                        min={0}
                        step={0.01}
                        value={value.resolutionAzimuthMeters}
                        onChange={(e) =>
                            change({ resolutionAzimuthMeters: e.target.value })
                        }
                    />
                </Field>
            </div>
        </section>
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

type FormSelectProps<T extends string | number> = {
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
function FormSelect<T extends string | number>({
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
