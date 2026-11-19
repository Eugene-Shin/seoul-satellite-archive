import type { z } from "zod";

import type {
    aoiPresetSchema,
    collectRunPageSchema,
    collectRunSchema,
    collectRunSkipSchema,
    collectRunStatusSchema,
    collectRunTypeSchema,
    collectTargetSchema,
} from "@/features/collect/schemas";
import type { multiPolygonSchema } from "@/features/image/schemas";

export type CollectTarget = z.infer<typeof collectTargetSchema>;
export type AoiPreset = z.infer<typeof aoiPresetSchema>;
export type CollectRun = z.infer<typeof collectRunSchema>;
export type CollectRunPage = z.infer<typeof collectRunPageSchema>;
export type CollectRunStatus = z.infer<typeof collectRunStatusSchema>;
export type CollectRunType = z.infer<typeof collectRunTypeSchema>;
export type CollectRunSkip = z.infer<typeof collectRunSkipSchema>;

export type CollectTargetRequest = {
    collectionId: number;
    aoi: z.infer<typeof multiPolygonSchema>;
    scheduleCron: string;
    lookbackDays: number;
    enabled: boolean;
};
