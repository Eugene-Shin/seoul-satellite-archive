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

export const collectRunStatusSchema = z.enum([
    "QUEUED",
    "RUNNING",
    "SUCCEEDED",
    "FAILED",
    "CANCELED",
]);

export const collectRunTypeSchema = z.enum(["SCHEDULED", "ONE_OFF"]);

export const collectRunSchema = z.object({
    runId: z.number(),
    targetId: z.number(),
    collectionTitle: z.string(),
    runType: collectRunTypeSchema,
    status: collectRunStatusSchema,
    windowFrom: z.iso.datetime().nullable(),
    windowTo: z.iso.datetime().nullable(),
    found: z.number(),
    inserted: z.number(),
    updated: z.number(),
    skipped: z.number(),
    failureReason: z.string().nullable(),
    createdAt: z.iso.datetime(),
    startedAt: z.iso.datetime().nullable(),
    cancelRequestedAt: z.iso.datetime().nullable(),
    finishedAt: z.iso.datetime().nullable(),
});

export const collectRunPageSchema = z.object({
    content: z.array(collectRunSchema),
    nextCursor: z.string().nullable(),
});

export const collectRunSkipSchema = z.object({
    sourceId: z.string(),
    reason: z.string(),
});

export const aoiPresetSchema = z.object({
    code: z.string(),
    name: z.string(),
    aoi: multiPolygonSchema,
    source: z.string(),
});
