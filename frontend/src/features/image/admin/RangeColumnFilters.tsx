"use client";

import {
    PopoverColumnFilter,
    SortToggle,
} from "@/features/image/admin/ColumnFilter";
import { AcquiredDateField } from "@/features/image/filters/AcquiredDateField";
import { CloudCoverField } from "@/features/image/filters/CloudCoverField";
import { GsdRangeField } from "@/features/image/filters/GsdRangeField";
import { isSarOnly } from "@/features/image/filters/sensorTypesChange";
import { useFilterOptions } from "@/features/image/queries";
import { isSortedBy, sortDirectionOf } from "@/features/image/sortOptions";
import type { ImageSearchParams } from "@/features/image/types";

type RangeColumnFilterProps = {
    conditions: ImageSearchParams;
    onChange: (changes: Partial<ImageSearchParams>) => void;
};

export function AcquiredAtColumnFilter({
    conditions,
    onChange,
}: RangeColumnFilterProps) {
    const { data: options } = useFilterOptions();
    const sort = conditions.sorts?.[0];
    const acquiredAtSort = sortDirectionOf(sort, "acquiredAt");

    return (
        <PopoverColumnFilter
            label="촬영일시"
            active={
                conditions.acquiredFrom !== undefined ||
                conditions.acquiredTo !== undefined
            }
            sortMark={acquiredAtSort ?? (sort ? undefined : "default")}
            onReset={() =>
                onChange({
                    acquiredFrom: undefined,
                    acquiredTo: undefined,
                    sorts: isSortedBy(sort, "acquiredAt")
                        ? undefined
                        : conditions.sorts,
                })
            }
        >
            {options && (
                <AcquiredDateField
                    range={options.acquiredRange}
                    conditions={conditions}
                    onChange={onChange}
                />
            )}
            <SortToggle
                field="acquiredAt"
                value={sort}
                onChange={(sorts) => onChange({ sorts })}
            />
        </PopoverColumnFilter>
    );
}

export function GsdColumnFilter({
    conditions,
    onChange,
}: RangeColumnFilterProps) {
    const { data: options } = useFilterOptions();

    return (
        <PopoverColumnFilter
            label="해상도"
            active={
                conditions.gsdMin !== undefined ||
                conditions.gsdMax !== undefined
            }
            onReset={() => onChange({ gsdMin: undefined, gsdMax: undefined })}
        >
            {options && (
                <GsdRangeField
                    range={options.gsdRange}
                    conditions={conditions}
                    onChange={onChange}
                />
            )}
        </PopoverColumnFilter>
    );
}

export function CloudCoverColumnFilter({
    conditions,
    onChange,
}: RangeColumnFilterProps) {
    const sort = conditions.sorts?.[0];
    const cloudCoverSort = sortDirectionOf(sort, "cloudCover");
    const sarOnly = isSarOnly(conditions.sensorTypes);

    return (
        <PopoverColumnFilter
            label="구름량"
            active={conditions.cloudMax !== undefined}
            sortMark={cloudCoverSort}
            onReset={() =>
                onChange({
                    cloudMax: undefined,
                    sorts: isSortedBy(sort, "cloudCover")
                        ? undefined
                        : conditions.sorts,
                })
            }
        >
            <CloudCoverField
                value={conditions.cloudMax}
                disabled={sarOnly}
                onChange={(cloudMax) => onChange({ cloudMax })}
            />
            <SortToggle
                field="cloudCover"
                value={sort}
                disabled={sarOnly}
                onChange={(sorts) => onChange({ sorts })}
            />
        </PopoverColumnFilter>
    );
}
