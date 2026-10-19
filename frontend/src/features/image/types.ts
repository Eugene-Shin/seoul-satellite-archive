import type { z } from "zod";

import type {
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

export type ImageFilterOptionsResponse = z.infer<
    typeof imageFilterOptionsSchema
>;

export type GsdRange = ImageFilterOptionsResponse["gsdRange"];

export type AcquiredRange = ImageFilterOptionsResponse["acquiredRange"];

export type ImageSort = z.infer<typeof imageSortSchema>;

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
