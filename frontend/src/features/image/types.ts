import type { z } from "zod";

import type {
    collectionSchema,
    imageDetailSchema,
    imageFileRoleSchema,
    imageFileSchema,
    imageFilterOptionsSchema,
    imageResponseSchema,
    imageSortSchema,
    lookDirectionSchema,
    multiPolygonSchema,
    orbitDirectionSchema,
    polarizationSchema,
    providerSchema,
    sarSchema,
    sensorTypeSchema,
} from "@/features/image/schemas";

export type SensorType = z.infer<typeof sensorTypeSchema>;

export type OrbitDirection = z.infer<typeof orbitDirectionSchema>;

export type Provider = z.infer<typeof providerSchema>;

export type Collection = z.infer<typeof collectionSchema>;

export type Polarization = z.infer<typeof polarizationSchema>;

export type LookDirection = z.infer<typeof lookDirectionSchema>;

export type Sar = z.infer<typeof sarSchema>;

export type ImageFileRole = z.infer<typeof imageFileRoleSchema>;

export type ImageFile = z.infer<typeof imageFileSchema>;

export type MultiPolygon = z.infer<typeof multiPolygonSchema>;

export type ImageResponse = z.infer<typeof imageResponseSchema>;

export type ImageDetailResponse = z.infer<typeof imageDetailSchema>;

export type ImageFilterOptionsResponse = z.infer<
    typeof imageFilterOptionsSchema
>;

export type ResolutionRange = ImageFilterOptionsResponse["resolutionRange"];

export type AcquiredRange = ImageFilterOptionsResponse["acquiredRange"];

export type ImageSort = z.infer<typeof imageSortSchema>;

// 등록·수정 요청 본문. 보내기만 해서 zod 스키마 없이 타입만 둔다
export type ImageWriteRequest = {
    collectionId: number | null;
    sourceId: string;
    title: string;
    satellite: string;
    instruments: string[] | null;
    sensorType: SensorType;
    acquiredAt: string;
    acquiredEndAt: string | null;
    cloudCover: number | null;
    orbitDirection: OrbitDirection | null;
    sar: Sar | null;
    footprint: unknown;
    url: string;
    downloadUrls: string[];
    sourceUpdatedAt: string | null;
    rawMetadata: unknown;
};

// alwaysFit: 화면 안에 있어도 지도를 영상 범위에 맞춘다. 아니면 화면 밖일 때만 이동한다
export type ImageSelection = {
    image: ImageResponse;
    alwaysFit: boolean;
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
    collectionIds?: number[];
    productTypes?: string[];
    isPaid?: boolean;
    resolutionMin?: number;
    resolutionMax?: number;
    cloudMax?: number;
    sorts?: ImageSort[];
    bbox?: Bbox;
    cursor?: string;
    size?: number;
};
