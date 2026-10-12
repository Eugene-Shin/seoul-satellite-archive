"use client";

import type {
    ImageSearchParams,
    ImageFilterOptionsResponse,
    SensorType,
} from "@/features/image/types";
import { toKstDate } from "@/shared/utils/date";
import {
    ORBIT_DIRECTION_LABELS,
    SENSOR_TYPE_LABELS,
} from "@/features/image/labels";
import { CloudCoverField } from "@/features/image/components/CloudCoverField";
import { GsdRangeField } from "@/features/image/components/GsdRangeField";
import { MultiSelect } from "@/shared/components/MultiSelect";

function emptyToUndefined<T>(values: T[]): T[] | undefined {
    return values.length > 0 ? values : undefined;
}

function isSarOnly(sensorTypes: SensorType[] | undefined): boolean {
    return sensorTypes ? sensorTypes.every((type) => type === "SAR") : false;
}

type ImageSearchFormProps = {
    options: ImageFilterOptionsResponse;
    conditions: ImageSearchParams;
    onChange: (change: Partial<ImageSearchParams>) => void;
};

export function ImageSearchForm({
    options,
    conditions,
    onChange,
}: ImageSearchFormProps) {
    const { min, max } = options.acquiredRange;
    const firstDate = min ? toKstDate(min) : undefined;
    const lastDate = max ? toKstDate(max) : undefined;

    return (
        <form
            className="flex flex-col gap-4"
            onSubmit={(e) => e.preventDefault()}
        >
            <label className="flex flex-col gap-1">
                <span className="font-semibold">제목</span>
                <input
                    type="text"
                    value={conditions.keyword ?? ""}
                    onChange={(e) =>
                        onChange({ keyword: e.target.value || undefined })
                    }
                    placeholder="영상 제목을 입력하세요"
                    className="rounded border px-3 py-2"
                />
            </label>

            <fieldset className="flex flex-col gap-1">
                <legend className="font-semibold">촬영일</legend>
                <div className="flex items-center gap-2">
                    <input
                        type="date"
                        aria-label="촬영일 시작"
                        value={conditions.acquiredFrom ?? ""}
                        min={firstDate}
                        max={conditions.acquiredTo || lastDate}
                        onChange={(e) =>
                            onChange({
                                acquiredFrom: e.target.value || undefined,
                            })
                        }
                        className="rounded border px-3 py-2"
                    />
                    <span>~</span>
                    <input
                        type="date"
                        aria-label="촬영일 종료"
                        value={conditions.acquiredTo ?? ""}
                        min={conditions.acquiredFrom || firstDate}
                        max={lastDate}
                        onChange={(e) =>
                            onChange({
                                acquiredTo: e.target.value || undefined,
                            })
                        }
                        className="rounded border px-3 py-2"
                    />
                </div>
            </fieldset>
            <div className="flex flex-wrap gap-2">
                <MultiSelect
                    label="위성"
                    options={options.satellites.map((satellite) => ({
                        value: satellite,
                        label: satellite,
                    }))}
                    selected={conditions.satellites ?? []}
                    onChange={(satellites) =>
                        onChange({ satellites: emptyToUndefined(satellites) })
                    }
                />
                <MultiSelect
                    label="센서"
                    options={options.instruments.map((instrument) => ({
                        value: instrument,
                        label: instrument,
                    }))}
                    selected={conditions.instruments ?? []}
                    onChange={(instruments) =>
                        onChange({ instruments: emptyToUndefined(instruments) })
                    }
                />
                <MultiSelect
                    label="영상 제공 기관"
                    options={options.providers.map((provider) => ({
                        value: provider.code,
                        label: provider.name,
                    }))}
                    selected={conditions.providers ?? []}
                    onChange={(providers) =>
                        onChange({ providers: emptyToUndefined(providers) })
                    }
                />
            </div>

            <div className="flex flex-wrap gap-4">
                <label className="flex flex-col gap-1">
                    <span className="font-semibold">센서 유형</span>
                    <select
                        value={
                            conditions.sensorTypes
                                ? conditions.sensorTypes[0]
                                : ""
                        }
                        onChange={(e) => {
                            const type = options.sensorTypes.find(
                                (t) => t === e.target.value,
                            );
                            const sensorTypes = type ? [type] : undefined;
                            onChange({
                                sensorTypes,
                                cloudMax: isSarOnly(sensorTypes)
                                    ? undefined
                                    : conditions.cloudMax,
                            });
                        }}
                        className="rounded border px-3 py-2"
                    >
                        <option value="">전체</option>
                        {options.sensorTypes.map((type) => (
                            <option key={type} value={type}>
                                {SENSOR_TYPE_LABELS[type]}
                            </option>
                        ))}
                    </select>
                </label>

                <label className="flex flex-col gap-1">
                    <span className="font-semibold">궤도 방향</span>
                    <select
                        value={conditions.orbitDirection ?? ""}
                        onChange={(e) =>
                            onChange({
                                orbitDirection: options.orbitDirections.find(
                                    (direction) => direction === e.target.value,
                                ),
                            })
                        }
                        className="rounded border px-3 py-2"
                    >
                        <option value="">전체</option>
                        {options.orbitDirections.map((direction) => (
                            <option key={direction} value={direction}>
                                {ORBIT_DIRECTION_LABELS[direction]}
                            </option>
                        ))}
                    </select>
                </label>

                <label className="flex flex-col gap-1">
                    <span className="font-semibold">유료 여부</span>
                    <select
                        value={
                            conditions.isPaid === undefined
                                ? ""
                                : String(conditions.isPaid)
                        }
                        onChange={(e) => {
                            onChange({
                                isPaid:
                                    e.target.value === ""
                                        ? undefined
                                        : e.target.value === "true",
                            });
                        }}
                        className="rounded border px-3 py-2"
                    >
                        <option value="">전체</option>
                        <option value="true">유료</option>
                        <option value="false">무료</option>
                    </select>
                </label>
            </div>

            <div className="flex flex-wrap gap-8">
                <GsdRangeField
                    range={options.gsdRange}
                    conditions={conditions}
                    onChange={onChange}
                />
                <CloudCoverField
                    value={conditions.cloudMax}
                    disabled={isSarOnly(conditions.sensorTypes)}
                    onChange={(cloudMax) => onChange({ cloudMax })}
                />
            </div>
        </form>
    );
}
