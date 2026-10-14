import type { z } from "zod";

import type {
    imageFilterOptionsSchema,
    imageResponseSchema,
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

export type ImageSort =
    | "ACQUIRED_AT_ASC"
    | "ACQUIRED_AT_DESC"
    | "CLOUD_COVER_ASC"
    | "CLOUD_COVER_DESC";

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
    page?: number;
    size?: number;
};
