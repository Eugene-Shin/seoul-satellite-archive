import type { z } from "zod";

import type {
    aoiPresetSchema,
    collectTargetSchema,
} from "@/features/collect/schemas";
import type { multiPolygonSchema } from "@/features/image/schemas";

export type CollectTarget = z.infer<typeof collectTargetSchema>;
export type AoiPreset = z.infer<typeof aoiPresetSchema>;

export type CollectTargetRequest = {
    collectionId: number;
    aoi: z.infer<typeof multiPolygonSchema>;
    scheduleCron: string;
    lookbackDays: number;
    enabled: boolean;
};
