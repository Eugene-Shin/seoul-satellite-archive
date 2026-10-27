import { z } from "zod";

import { sliceResponseSchema } from "@/shared/api/schemas";

export const sensorTypeSchema = z.enum(["OPTICAL", "SAR"]);

export const orbitDirectionSchema = z.enum(["ASCENDING", "DESCENDING"]);

export const imageSortSchema = z.enum([
    "ACQUIRED_AT_DESC",
    "ACQUIRED_AT_ASC",
    "CLOUD_COVER_ASC",
    "CLOUD_COVER_DESC",
]);

export const providerSchema = z.object({
    code: z.string(),
    name: z.string(),
    isPaid: z.boolean(),
});

export const multiPolygonSchema = z.object({
    type: z.literal("MultiPolygon"),
    coordinates: z.array(z.array(z.array(z.array(z.number())))),
});

export const imageResponseSchema = z.object({
    imageId: z.number(),
    provider: providerSchema,
    title: z.string(),
    satellite: z.string(),
    instruments: z.array(z.string()).nullable(),
    sensorType: sensorTypeSchema,
    acquiredAt: z.iso.datetime(),
    gsdMeters: z.number().nullable(),
    cloudCover: z.number().nullable(),
    orbitDirection: orbitDirectionSchema.nullable(),
    footprint: multiPolygonSchema,
    downloadUrls: z.array(z.string()),
    sourceUpdatedAt: z.iso.datetime().nullable(),
});

export const imageDetailSchema = z.object({
    imageId: z.number(),
    providerCode: z.string(),
    sourceId: z.string(),
    title: z.string(),
    satellite: z.string(),
    instruments: z.array(z.string()).nullable(),
    sensorType: sensorTypeSchema,
    acquiredAt: z.iso.datetime(),
    gsdMeters: z.number().nullable(),
    cloudCover: z.number().nullable(),
    orbitDirection: orbitDirectionSchema.nullable(),
    footprint: multiPolygonSchema,
    url: z.string(),
    downloadUrls: z.array(z.string()),
    sourceUpdatedAt: z.iso.datetime().nullable(),
    rawMetadata: z.record(z.string(), z.unknown()),
});

function rangeSchema<T extends z.ZodType>(valueSchema: T) {
    return z.object({
        min: valueSchema.nullable(),
        max: valueSchema.nullable(),
    });
}

export const imageFilterOptionsSchema = z.object({
    satellites: z.array(z.string()),
    instruments: z.array(z.string()),
    providers: z.array(providerSchema),
    sensorTypes: z.array(sensorTypeSchema),
    orbitDirections: z.array(orbitDirectionSchema),
    gsdRange: rangeSchema(z.number()),
    acquiredRange: rangeSchema(z.iso.datetime()),
});

export const imageSearchResultSchema = sliceResponseSchema(imageResponseSchema);
