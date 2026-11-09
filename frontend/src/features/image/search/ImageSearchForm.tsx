"use client";

import { useState } from "react";

import { AcquiredDateField } from "@/features/image/filters/AcquiredDateField";
import { CloudCoverField } from "@/features/image/filters/CloudCoverField";
import { emptyToUndefined } from "@/features/image/filters/emptyToUndefined";
import { ResolutionRangeField } from "@/features/image/filters/ResolutionRangeField";
import {
    isSarOnly,
    sensorTypesChange,
} from "@/features/image/filters/sensorTypesChange";
import {
    collectionOptionLabel,
    ORBIT_DIRECTION_NAMES,
    SENSOR_TYPE_NAMES,
} from "@/features/image/imageText";
import { SortField } from "@/features/image/search/SortField";
import type {
    Collection,
    ImageFilterOptionsResponse,
    ImageSearchParams,
} from "@/features/image/types";
import { ErrorMessage } from "@/shared/components/ErrorMessage";
import { MultiSelect } from "@/shared/components/MultiSelect";
import { Select } from "@/shared/components/Select";
import { Button } from "@/shared/components/ui/button";
import { Input } from "@/shared/components/ui/input";

// 검색 적용은 폼 제출 시에만 수행한다
export const IMAGE_SEARCH_FORM_ID = "image-search-form";

type ImageSearchFormProps = {
    options: ImageFilterOptionsResponse;
    collections: Collection[];
    collectionsError?: unknown;
    onRetryCollections?: () => void;
    conditions: ImageSearchParams;
    onChange: (changes: Partial<ImageSearchParams>) => void;
    onSubmit: () => void;
};

export function ImageSearchForm({
    options,
    collections,
    collectionsError,
    onRetryCollections,
    conditions,
    onChange,
    onSubmit,
}: ImageSearchFormProps) {
    const [advanced, setAdvanced] = useState(false);
    const advancedCount = [
        conditions.acquiredFrom || conditions.acquiredTo,
        conditions.instruments?.length,
        conditions.resolutionMin !== undefined ||
            conditions.resolutionMax !== undefined,
        conditions.cloudMax !== undefined,
        conditions.orbitDirection,
        conditions.isPaid !== undefined,
        conditions.productTypes?.length,
        conditions.sorts?.length,
    ].filter(Boolean).length;
    const productTypes = [
        ...new Set(collections.map((collection) => collection.productType)),
    ].sort();
    return (
        <form
            id={IMAGE_SEARCH_FORM_ID}
            className="flex flex-col gap-3"
            onSubmit={(e) => {
                e.preventDefault();
                onSubmit();
            }}
        >
            <div className="catalog-basic-filters">
                <label className="flex flex-col gap-2">
                    <span className="text-sm font-medium">키워드</span>
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
                <MultiSelect
                    label="컬렉션"
                    options={collections.map((collection) => ({
                        value: collection.collectionId,
                        label: collectionOptionLabel(
                            collection,
                            options.providers,
                        ),
                    }))}
                    selected={conditions.collectionIds ?? []}
                    onChange={(ids) =>
                        onChange({ collectionIds: emptyToUndefined(ids) })
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
                    onChange={(type) =>
                        onChange(
                            sensorTypesChange(
                                conditions,
                                type ? [type] : undefined,
                            ),
                        )
                    }
                />
                <Button type="submit" className="catalog-search-submit">
                    영상 검색
                </Button>
            </div>
            {conditions.bbox && (
                <span className="text-xs text-muted-foreground">
                    지도 검색영역 설정됨
                </span>
            )}
            <ErrorMessage
                error={collectionsError}
                onRetry={onRetryCollections}
            />
            <div
                id="catalog-advanced-filters"
                hidden={!advanced}
                className="catalog-advanced-filters rounded-lg border bg-muted/30 p-4"
            >
                <div className="catalog-date-filter">
                    <AcquiredDateField
                        range={options.acquiredRange}
                        conditions={conditions}
                        onChange={onChange}
                    />
                </div>
                <MultiSelect
                    label="세부 센서"
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
                <ResolutionRangeField
                    range={options.resolutionRange}
                    conditions={conditions}
                    onChange={onChange}
                />
                <CloudCoverField
                    value={conditions.cloudMax}
                    disabled={isSarOnly(conditions.sensorTypes)}
                    onChange={(cloudMax) => onChange({ cloudMax })}
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
                <MultiSelect
                    label="제품 유형"
                    options={productTypes.map((type) => ({
                        value: type,
                        label: type,
                    }))}
                    selected={conditions.productTypes ?? []}
                    onChange={(types) =>
                        onChange({ productTypes: emptyToUndefined(types) })
                    }
                />
                <div className="sm:col-span-2">
                    <SortField
                        value={conditions.sorts ?? []}
                        cloudCoverDisabled={isSarOnly(conditions.sensorTypes)}
                        onChange={(sorts) =>
                            onChange({ sorts: emptyToUndefined(sorts) })
                        }
                    />
                </div>
            </div>
            <div className="flex justify-center">
                <Button
                    type="button"
                    variant="ghost"
                    aria-expanded={advanced}
                    aria-controls="catalog-advanced-filters"
                    onClick={() => setAdvanced(!advanced)}
                >
                    상세 필터 {advanced ? "접기" : "펼치기"}
                    {advancedCount > 0 && (
                        <span className="text-xs text-muted-foreground">
                            · {advancedCount}개 설정
                        </span>
                    )}
                </Button>
            </div>
        </form>
    );
}
