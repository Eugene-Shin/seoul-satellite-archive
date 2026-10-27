import type { z } from "zod";

import type {
    imageDetailSchema,
    imageFilterOptionsSchema,
    imageResponseSchema,
    imageSortSchema,
    multiPolygonSchema,
    orbitDirectionSchema,
    providerSchema,
    sensorTypeSchema,
} from "@/features/image/schemas";

export type SensorType = z.infer<typeof sensorTypeSchema>;

export type OrbitDirection = z.infer<typeof orbitDirectionSchema>;

export type Provider = z.infer<typeof providerSchema>;

export type MultiPolygon = z.infer<typeof multiPolygonSchema>;

export type ImageResponse = z.infer<typeof imageResponseSchema>;

export type ImageDetailResponse = z.infer<typeof imageDetailSchema>;

export type ImageFilterOptionsResponse = z.infer<
    typeof imageFilterOptionsSchema
>;

export type GsdRange = ImageFilterOptionsResponse["gsdRange"];

export type AcquiredRange = ImageFilterOptionsResponse["acquiredRange"];

export type ImageSort = z.infer<typeof imageSortSchema>;

// 등록·수정 요청 본문. 보내기만 해서 zod 스키마 없이 타입만 둔다
export type ImageWriteRequest = {
    providerCode: string;
    sourceId: string;
    title: string;
    satellite: string;
    instruments: string[] | null;
    sensorType: SensorType;
    acquiredAt: string;
    gsdMeters: number | null;
    cloudCover: number | null;
    orbitDirection: OrbitDirection | null;
    footprint: unknown;
    url: string;
    downloadUrls: string[];
    sourceUpdatedAt: string | null;
    rawMetadata: unknown;
};

// 지도 도구는 한 번에 하나만: 영역 그리기 / 거리 측정 / 면적 측정
export type MapTool = "area" | "distance" | "surface";

export type Bbox = [
    minLon: number,
    minLat: number,
    maxLon: number,
    maxLat: number,
];

export type ImageSearchParams = {
    keyword?: string;
    acquiredFrom?: string;
    acquiredTo?: string;
    satellites?: string[];
    instruments?: string[];
    sensorTypes?: SensorType[];
    orbitDirection?: OrbitDirection;
    providers?: string[];
    isPaid?: boolean;
    gsdMin?: number;
    gsdMax?: number;
    cloudMax?: number;
    sorts?: ImageSort[];
    bbox?: Bbox;
    cursor?: string;
    size?: number;
};
