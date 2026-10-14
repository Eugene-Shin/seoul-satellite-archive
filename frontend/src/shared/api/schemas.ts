import { z } from "zod";

export const apiResponseSchema = z.object({
    success: z.boolean(),
    code: z.string(),
    message: z.string(),
    data: z.unknown(),
    errorDetail: z.array(z.string()).optional(),
});

export function sliceResponseSchema<T extends z.ZodType>(itemSchema: T) {
    return z.object({
        content: z.array(itemSchema),
        hasNext: z.boolean(),
    });
}
