import type { z } from "zod";

import { apiResponseSchema } from "@/shared/api/schemas";

type QueryValue = string | number | boolean | null | undefined;

export type QueryParams = Record<string, QueryValue | QueryValue[]>;

export class ApiError extends Error {
    readonly status: number | null;
    readonly code: string | null;
    readonly details: string[];

    constructor(
        message: string,
        {
            status = null,
            code = null,
            details = [],
        }: {
            status?: number | null;
            code?: string | null;
            details?: string[];
        } = {},
    ) {
        super(message);
        this.name = "ApiError";
        this.status = status;
        this.code = code;
        this.details = details;
    }
}

function toSearchParams(params: QueryParams): URLSearchParams {
    const searchParams = new URLSearchParams();

    for (const [key, value] of Object.entries(params)) {
        const values = Array.isArray(value) ? value : [value];
        for (const item of values) {
            if (item !== null && item !== undefined && item !== "") {
                searchParams.append(key, String(item));
            }
        }
    }

    return searchParams;
}

export async function apiGet<S extends z.ZodType>(
    path: string,
    schema: S,
    params: QueryParams = {},
): Promise<z.infer<S>> {
    const baseUrl = process.env.NEXT_PUBLIC_API_URL;

    if (!baseUrl) {
        throw new Error("NEXT_PUBLIC_API_URL이 설정되지 않았습니다.");
    }

    const url = new URL(path, baseUrl);
    url.search = toSearchParams(params).toString();

    let response: Response;
    try {
        response = await fetch(url);
    } catch {
        throw new ApiError(
            "서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.",
        );
    }

    const envelope = apiResponseSchema.safeParse(await readJson(response));

    if (!envelope.success) {
        throw new ApiError(
            `서버 응답을 처리할 수 없습니다. (${response.status})`,
            { status: response.status },
        );
    }

    const body = envelope.data;

    if (!body.success) {
        throw new ApiError(body.message, {
            status: response.status,
            code: body.code,
            details: body.errorDetail ?? [],
        });
    }

    const data = schema.safeParse(body.data);

    if (!data.success) {
        throw new ApiError("서버 응답 형식이 올바르지 않습니다.", {
            status: response.status,
            details: data.error.issues.map(
                (issue) => `${issue.path.join(".")}: ${issue.message}`,
            ),
        });
    }

    return data.data;
}

async function readJson(response: Response): Promise<unknown> {
    try {
        return await response.json();
    } catch {
        return undefined;
    }
}
