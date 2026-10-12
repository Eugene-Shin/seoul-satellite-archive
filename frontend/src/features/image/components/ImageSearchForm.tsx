"use client";

import { AcquiredDateField } from "@/features/image/components/AcquiredDateField";
import { CloudCoverField } from "@/features/image/components/CloudCoverField";
import { GsdRangeField } from "@/features/image/components/GsdRangeField";
import { SortField } from "@/features/image/components/SortField";
import {
    ORBIT_DIRECTION_LABELS,
    SENSOR_TYPE_LABELS,
} from "@/features/image/labels";
import type {
    ImageFilterOptionsResponse,
    ImageSearchParams,
    SensorType,
} from "@/features/image/types";
import { MultiSelect } from "@/shared/components/MultiSelect";
import { Select } from "@/shared/components/Select";

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

            <AcquiredDateField
                range={options.acquiredRange}
                conditions={conditions}
                onChange={onChange}
            />

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
                <Select
                    label="센서 유형"
                    options={options.sensorTypes.map((type) => ({
                        value: type,
                        label: SENSOR_TYPE_LABELS[type],
                    }))}
                    value={
                        conditions.sensorTypes ? conditions.sensorTypes[0] : ""
                    }
                    onChange={(value) => {
                        const type = options.sensorTypes.find(
                            (t) => t === value,
                        );
                        const sensorTypes = type ? [type] : undefined;
                        onChange({
                            sensorTypes,
                            cloudMax: isSarOnly(sensorTypes)
                                ? undefined
                                : conditions.cloudMax,
                        });
                    }}
                />
                <Select
                    label="궤도 방향"
                    options={options.orbitDirections.map((direction) => ({
                        value: direction,
                        label: ORBIT_DIRECTION_LABELS[direction],
                    }))}
                    value={conditions.orbitDirection ?? ""}
                    onChange={(value) =>
                        onChange({
                            orbitDirection: options.orbitDirections.find(
                                (direction) => direction === value,
                            ),
                        })
                    }
                />
                <Select
                    label="유료 여부"
                    options={[
                        { value: "true", label: "유료" },
                        { value: "false", label: "무료" },
                    ]}
                    value={
                        conditions.isPaid === undefined
                            ? ""
                            : String(conditions.isPaid)
                    }
                    onChange={(value) =>
                        onChange({
                            isPaid: value === "" ? undefined : value === "true",
                        })
                    }
                />
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

            <SortField
                value={conditions.sorts ?? []}
                onChange={(sorts) =>
                    onChange({ sorts: emptyToUndefined(sorts) })
                }
            />
        </form>
    );
}
