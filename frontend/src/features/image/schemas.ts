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
    ownOrganization: z.boolean(),
});

// providerCode는 이 컬렉션을 정의한 출처. 영상의 제공 기관과 다를 수 있다(올포랜드 영상)
export const collectionSchema = z.object({
    collectionId: z.number(),
    providerCode: z.string(),
    code: z.string(),
    title: z.string(),
    productType: z.string(),
});

export const polarizationSchema = z.enum(["HH", "HV", "VH", "VV"]);

export const lookDirectionSchema = z.enum(["LEFT", "RIGHT"]);

export const sarSchema = z.object({
    relativeOrbit: z.number().nullable(),
    frame: z.number().nullable(),
    instrumentMode: z.string().nullable(),
    polarizations: z.array(polarizationSchema).nullable(),
    lookDirection: lookDirectionSchema.nullable(),
    incidenceAngle: z.number().nullable(),
    resolutionRangeMeters: z.number().nullable(),
    resolutionAzimuthMeters: z.number().nullable(),
});

// 파일의 픽셀 간격 범위. 없는 영상은 센서 해상도로 대체
export const resolutionSchema = z.object({
    minMeters: z.number(),
    maxMeters: z.number(),
});

export const imageFileRoleSchema = z.enum([
    "ORIGINAL",
    "DATA",
    "PREVIEW",
    "THUMBNAIL",
    "METADATA",
]);

export const imageFileBandSchema = z.object({
    bandIndex: z.number(),
    bandName: z.string().nullable(),
    commonName: z.string().nullable(),
    centerWavelengthUm: z.number().nullable(),
    bandwidthUm: z.number().nullable(),
    resolutionMeters: z.number().nullable(),
});

export const imageFileSchema = z.object({
    fileId: z.number(),
    assetKey: z.string(),
    role: imageFileRoleSchema,
    href: z.string(),
    mediaType: z.string().nullable(),
    pixelSpacingMeters: z.number().nullable(),
    bands: z.array(imageFileBandSchema),
});

export const multiPolygonSchema = z.object({
    type: z.literal("MultiPolygon"),
    coordinates: z.array(z.array(z.array(z.array(z.number())))),
});

export const imageResponseSchema = z.object({
    imageId: z.number(),
    provider: providerSchema,
    collection: collectionSchema,
    title: z.string(),
    satellite: z.string(),
    instruments: z.array(z.string()).nullable(),
    sensorType: sensorTypeSchema,
    acquiredAt: z.iso.datetime(),
    acquiredEndAt: z.iso.datetime().nullable(),
    resolution: resolutionSchema.nullable(),
    cloudCover: z.number().nullable(),
    orbitDirection: orbitDirectionSchema.nullable(),
    sar: sarSchema.nullable(),
    footprint: multiPolygonSchema,
    sourceUpdatedAt: z.iso.datetime().nullable(),
});

export const imageDetailSchema = z.object({
    imageId: z.number(),
    providerCode: z.string(),
    collection: collectionSchema,
    sourceId: z.string(),
    title: z.string(),
    satellite: z.string(),
    instruments: z.array(z.string()).nullable(),
    sensorType: sensorTypeSchema,
    acquiredAt: z.iso.datetime(),
    acquiredEndAt: z.iso.datetime().nullable(),
    resolution: resolutionSchema.nullable(),
    cloudCover: z.number().nullable(),
    orbitDirection: orbitDirectionSchema.nullable(),
    sar: sarSchema.nullable(),
    footprint: multiPolygonSchema,
    url: z.string(),
    downloadUrls: z.array(z.string()),
    files: z.array(imageFileSchema),
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
    resolutionRange: rangeSchema(z.number()),
    acquiredRange: rangeSchema(z.iso.datetime()),
});

export const imageSearchResultSchema = sliceResponseSchema(imageResponseSchema);
