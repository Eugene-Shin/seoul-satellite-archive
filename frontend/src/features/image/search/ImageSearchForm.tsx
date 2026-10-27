"use client";

import { AcquiredDateField } from "@/features/image/filters/AcquiredDateField";
import { CloudCoverField } from "@/features/image/filters/CloudCoverField";
import { emptyToUndefined } from "@/features/image/filters/emptyToUndefined";
import { GsdRangeField } from "@/features/image/filters/GsdRangeField";
import {
    ORBIT_DIRECTION_NAMES,
    SENSOR_TYPE_NAMES,
} from "@/features/image/imageText";
import { SortField } from "@/features/image/search/SortField";
import type {
    ImageFilterOptionsResponse,
    ImageSearchParams,
    SensorType,
} from "@/features/image/types";
import { MultiSelect } from "@/shared/components/MultiSelect";
import { Select } from "@/shared/components/Select";
import { Input } from "@/shared/components/ui/input";

function isSarOnly(sensorTypes: SensorType[] | undefined): boolean {
    return sensorTypes ? sensorTypes.every((type) => type === "SAR") : false;
}

// 검색 버튼은 두 탭이 함께 쓰려고 폼 밖에 있어서, 이 id로 폼에 연결한다
export const IMAGE_SEARCH_FORM_ID = "image-search-form";

type ImageSearchFormProps = {
    options: ImageFilterOptionsResponse;
    conditions: ImageSearchParams;
    onChange: (changes: Partial<ImageSearchParams>) => void;
    onSubmit: () => void;
};

export function ImageSearchForm({
    options,
    conditions,
    onChange,
    onSubmit,
}: ImageSearchFormProps) {
    return (
        <form
            id={IMAGE_SEARCH_FORM_ID}
            className="flex flex-col gap-4"
            onSubmit={(e) => {
                e.preventDefault();
                onSubmit();
            }}
        >
            <label className="flex flex-col gap-2">
                <span className="text-sm font-medium">제목</span>
                <Input
                    type="text"
                    value={conditions.keyword ?? ""}
                    onChange={(e) =>
                        onChange({
                            keyword: e.target.value || undefined,
                        })
                    }
                    placeholder="영상 제목을 입력하세요"
                />
            </label>

            <AcquiredDateField
                range={options.acquiredRange}
                conditions={conditions}
                onChange={onChange}
            />

            <div className="grid grid-cols-2 gap-x-2 gap-y-4">
                <MultiSelect
                    label="위성"
                    options={options.satellites.map((satellite) => ({
                        value: satellite,
                        label: satellite,
                    }))}
                    selected={conditions.satellites ?? []}
                    onChange={(satellites) =>
                        onChange({
                            satellites: emptyToUndefined(satellites),
                        })
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
                        onChange({
                            instruments: emptyToUndefined(instruments),
                        })
                    }
                />
                <Select
                    label="센서 유형"
                    options={options.sensorTypes.map((type) => ({
                        value: type,
                        label: SENSOR_TYPE_NAMES[type],
                    }))}
                    value={
                        conditions.sensorTypes
                            ? conditions.sensorTypes[0]
                            : null
                    }
                    onChange={(type) => {
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
                        label: ORBIT_DIRECTION_NAMES[direction],
                    }))}
                    value={conditions.orbitDirection ?? null}
                    onChange={(orbitDirection) =>
                        onChange({
                            orbitDirection: orbitDirection ?? undefined,
                        })
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
                        onChange({
                            providers: emptyToUndefined(providers),
                        })
                    }
                />
                <Select
                    label="유료 여부"
                    options={[
                        { value: true, label: "유료" },
                        { value: false, label: "무료" },
                    ]}
                    value={conditions.isPaid ?? null}
                    onChange={(isPaid) =>
                        onChange({ isPaid: isPaid ?? undefined })
                    }
                />
            </div>

            <div className="flex flex-col gap-4">
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
