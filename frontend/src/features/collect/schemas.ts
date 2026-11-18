import { z } from "zod";

import { collectionSchema, multiPolygonSchema } from "@/features/image/schemas";

export const collectTargetSchema = z.object({
    targetId: z.number(),
    collection: collectionSchema,
    aoi: multiPolygonSchema,
    scheduleCron: z.string(),
    nextRunAt: z.iso.datetime().nullable(),
    lookbackDays: z.number(),
    enabled: z.boolean(),
    createdAt: z.iso.datetime(),
    updatedAt: z.iso.datetime(),
});

export const aoiPresetSchema = z.object({
    code: z.string(),
    name: z.string(),
    aoi: multiPolygonSchema,
    source: z.string(),
});
