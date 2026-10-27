"use client";

import { useVirtualizer } from "@tanstack/react-virtual";
import { FunnelXIcon, PencilIcon, Trash2Icon } from "lucide-react";
import { useEffect, useRef, useState } from "react";

import {
    MultiColumnFilter,
    SelectColumnFilter,
    toOptions,
} from "@/features/image/admin/ColumnFilter";
import { CreateImageDialog } from "@/features/image/admin/CreateImageDialog";
import { DeleteImageDialog } from "@/features/image/admin/DeleteImageDialog";
import { EditImageDialog } from "@/features/image/admin/EditImageDialog";
import {
    AcquiredAtColumnFilter,
    CloudCoverColumnFilter,
    GsdColumnFilter,
} from "@/features/image/admin/RangeColumnFilters";
import { sensorTypesChange } from "@/features/image/filters/sensorTypesChange";
import {
    acquiredAtText,
    cloudCoverText,
    gsdText,
    ORBIT_DIRECTION_NAMES,
    orbitDirectionText,
    SENSOR_TYPE_NAMES,
    sensorTypeText,
} from "@/features/image/imageText";
import { useAdminImages, useFilterOptions } from "@/features/image/queries";
import type { ImageResponse, ImageSearchParams } from "@/features/image/types";
import { ErrorMessage } from "@/shared/components/ErrorMessage";
import { IconButton } from "@/shared/components/IconButton";
import { Button } from "@/shared/components/ui/button";

// 한 줄 고정: py-2(16px) + text-sm 줄 높이(20px) + 아래 테두리(1px)
const ROW_HEIGHT = 37;
const CELL = "px-3 py-2 whitespace-nowrap";

// 글자는 왼쪽, 숫자는 오른쪽, 짧은 구분값은 가운데. 머리 칸과 값 칸이 같은 정렬을 쓴다
const ALIGN = {
    text: "text-left",
    number: "text-right tabular-nums",
    short: "text-center",
};

const COLUMN_ALIGN = {
    title: ALIGN.text,
    satellite: ALIGN.text,
    instruments: ALIGN.text,
    sensorType: ALIGN.short,
    acquiredAt: ALIGN.text,
    gsd: ALIGN.number,
    cloudCover: ALIGN.number,
    orbitDirection: ALIGN.short,
    provider: ALIGN.text,
    isPaid: ALIGN.short,
};

type Column = keyof typeof COLUMN_ALIGN;

function cellClass(column: Column): string {
    return `${CELL} ${COLUMN_ALIGN[column]}`;
}

function headerClass(column: Column): string {
    return `${cellClass(column)} font-medium`;
}

// 아이콘 버튼(24px)이 들어가도 행 높이(ROW_HEIGHT)가 같게 py-1.5
const ACTIONS_CELL =
    "sticky right-0 border-l px-3 py-1.5 text-center whitespace-nowrap";

const PAID_OPTIONS = [
    { value: true, label: "유료" },
    { value: false, label: "무료" },
];

type ImageAdminTableProps = {
    conditions: ImageSearchParams;
    onConditionsChange: (conditions: ImageSearchParams) => void;
};

export function ImageAdminTable({
    conditions,
    onConditionsChange,
}: ImageAdminTableProps) {
    const {
        data,
        error,
        fetchNextPage,
        hasNextPage,
        isPending,
        isPlaceholderData,
        isFetching,
        isFetchNextPageError,
        refetch,
    } = useAdminImages(conditions);
    const scrollRef = useRef<HTMLDivElement | null>(null);
    const [editingImageId, setEditingImageId] = useState<number | null>(null);
    const [deletingImage, setDeletingImage] = useState<ImageResponse | null>(
        null,
    );

    const images = data ? data.pages.flatMap((page) => page.content) : [];

    const virtualizer = useVirtualizer({
        count: images.length,
        getScrollElement: () => scrollRef.current,
        estimateSize: () => ROW_HEIGHT,
        overscan: 10,
    });
    const virtualRows = virtualizer.getVirtualItems();
    const paddingTop = virtualRows[0]?.start ?? 0;
    const paddingBottom =
        virtualizer.getTotalSize() - (virtualRows.at(-1)?.end ?? 0);

    useEffect(() => {
        if (hasNextPage && !isFetching && !error) {
            fetchNextPage();
        }
    }, [hasNextPage, isFetching, error, fetchNextPage]);

    function statusMessage(): string | null {
        if (error) {
            return null;
        }

        // 조건을 바꾸면 새 결과가 올 때까지 이전 목록을 보여준다
        if (isPending || isPlaceholderData) {
            return "불러오는 중입니다...";
        }

        if (images.length === 0) {
            return "영상이 없습니다.";
        }

        return hasNextPage
            ? `${images.length}건 불러오는 중...`
            : `총 ${images.length}건`;
    }

    function retry() {
        if (isFetchNextPageError) {
            fetchNextPage();
        } else {
            refetch();
        }
    }

    const status = statusMessage();
    const filtered = Object.values(conditions).some(
        (value) => value !== undefined,
    );

    return (
        <div className="flex min-h-0 flex-1 flex-col gap-3">
            <ErrorMessage error={error} onRetry={retry} />
            <div className="flex items-center justify-between gap-3">
                <p
                    hidden={status === null}
                    className="text-sm text-muted-foreground"
                >
                    {status}
                </p>
                <div className="ml-auto flex gap-2">
                    <CreateImageDialog />
                    <Button
                        variant="outline"
                        size="sm"
                        disabled={!filtered}
                        onClick={() => onConditionsChange({})}
                    >
                        <FunnelXIcon />
                        필터 초기화
                    </Button>
                </div>
            </div>
            <div
                ref={scrollRef}
                className="isolate min-h-0 flex-1 overflow-auto rounded-lg border"
            >
                <table className="w-full text-sm">
                    {/* 고정된 관리 칸이 머리 위로 그려지지 않게. 스크롤 영역이 isolate라 이 표 안에서만 쓰는 순서 */}
                    <thead className="sticky top-0 z-10 bg-muted">
                        <ImageAdminHeader
                            conditions={conditions}
                            onChange={(changes) =>
                                onConditionsChange({
                                    ...conditions,
                                    ...changes,
                                })
                            }
                        />
                    </thead>
                    <tbody>
                        <tr aria-hidden style={{ height: paddingTop }} />
                        {virtualRows.map((row) => (
                            <ImageAdminRow
                                key={images[row.index].imageId}
                                image={images[row.index]}
                                onEdit={() =>
                                    setEditingImageId(images[row.index].imageId)
                                }
                                onDelete={() =>
                                    setDeletingImage(images[row.index])
                                }
                            />
                        ))}
                        <tr aria-hidden style={{ height: paddingBottom }} />
                    </tbody>
                </table>
            </div>
            <EditImageDialog
                imageId={editingImageId}
                onClose={() => setEditingImageId(null)}
            />
            <DeleteImageDialog
                image={deletingImage}
                onClose={() => setDeletingImage(null)}
            />
        </div>
    );
}

type ImageAdminHeaderProps = {
    conditions: ImageSearchParams;
    onChange: (changes: Partial<ImageSearchParams>) => void;
};

function ImageAdminHeader({ conditions, onChange }: ImageAdminHeaderProps) {
    const { data: options } = useFilterOptions();

    return (
        <tr>
            <th className={headerClass("title")}>제목</th>
            <th className={headerClass("satellite")}>
                <MultiColumnFilter
                    label="위성"
                    options={toOptions(options?.satellites)}
                    selected={conditions.satellites}
                    onChange={(satellites) => onChange({ satellites })}
                />
            </th>
            <th className={headerClass("instruments")}>
                <MultiColumnFilter
                    label="센서"
                    options={toOptions(options?.instruments)}
                    selected={conditions.instruments}
                    onChange={(instruments) => onChange({ instruments })}
                />
            </th>
            <th className={headerClass("sensorType")}>
                <MultiColumnFilter
                    label="유형"
                    options={(options?.sensorTypes ?? []).map((type) => ({
                        value: type,
                        label: SENSOR_TYPE_NAMES[type],
                    }))}
                    selected={conditions.sensorTypes}
                    onChange={(sensorTypes) =>
                        onChange(sensorTypesChange(conditions, sensorTypes))
                    }
                />
            </th>
            <th className={headerClass("acquiredAt")}>
                <AcquiredAtColumnFilter
                    conditions={conditions}
                    onChange={onChange}
                />
            </th>
            <th className={headerClass("gsd")}>
                <GsdColumnFilter conditions={conditions} onChange={onChange} />
            </th>
            <th className={headerClass("cloudCover")}>
                <CloudCoverColumnFilter
                    conditions={conditions}
                    onChange={onChange}
                />
            </th>
            <th className={headerClass("orbitDirection")}>
                <SelectColumnFilter
                    label="궤도"
                    options={(options?.orbitDirections ?? []).map(
                        (direction) => ({
                            value: direction,
                            label: ORBIT_DIRECTION_NAMES[direction],
                        }),
                    )}
                    value={conditions.orbitDirection}
                    onChange={(orbitDirection) => onChange({ orbitDirection })}
                />
            </th>
            <th className={headerClass("provider")}>
                <MultiColumnFilter
                    label="제공 기관"
                    options={(options?.providers ?? []).map((provider) => ({
                        value: provider.code,
                        label: provider.name,
                    }))}
                    selected={conditions.providers}
                    onChange={(providers) => onChange({ providers })}
                />
            </th>
            <th className={headerClass("isPaid")}>
                <SelectColumnFilter
                    label="유료 여부"
                    options={PAID_OPTIONS}
                    value={conditions.isPaid}
                    onChange={(isPaid) => onChange({ isPaid })}
                />
            </th>
            {/* 자기도 sticky라 위쪽 고정을 따로 줘야 세로 스크롤에 안 밀린다 */}
            <th className={`${ACTIONS_CELL} top-0 bg-muted font-medium`}>
                관리
            </th>
        </tr>
    );
}

type ImageAdminRowProps = {
    image: ImageResponse;
    onEdit: () => void;
    onDelete: () => void;
};

function ImageAdminRow({ image, onEdit, onDelete }: ImageAdminRowProps) {
    return (
        <tr className="border-b">
            <td className={cellClass("title")}>
                <span className="block max-w-60 truncate" title={image.title}>
                    {image.title}
                </span>
            </td>
            <td className={cellClass("satellite")}>{image.satellite}</td>
            <td className={cellClass("instruments")}>
                {image.instruments ? image.instruments.join(", ") : "-"}
            </td>
            <td className={cellClass("sensorType")}>{sensorTypeText(image)}</td>
            <td className={cellClass("acquiredAt")}>{acquiredAtText(image)}</td>
            <td className={cellClass("gsd")}>{gsdText(image)}</td>
            <td className={cellClass("cloudCover")}>{cloudCoverText(image)}</td>
            <td className={cellClass("orbitDirection")}>
                {orbitDirectionText(image)}
            </td>
            <td className={cellClass("provider")}>{image.provider.name}</td>
            <td className={cellClass("isPaid")}>
                {image.provider.isPaid ? "유료" : "무료"}
            </td>
            <td className={`${ACTIONS_CELL} bg-popover`}>
                <IconButton
                    label={`${image.title} 수정`}
                    variant="ghost"
                    size="icon-xs"
                    onClick={onEdit}
                >
                    <PencilIcon />
                </IconButton>
                <IconButton
                    label={`${image.title} 삭제`}
                    variant="ghost"
                    size="icon-xs"
                    onClick={onDelete}
                >
                    <Trash2Icon />
                </IconButton>
            </td>
        </tr>
    );
}
