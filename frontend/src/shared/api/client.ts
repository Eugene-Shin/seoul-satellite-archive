import type { ApiResponse } from "@/shared/api/types";

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

export async function apiGet<T>(
    path: string,
    params: QueryParams = {},
): Promise<T> {
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

    const body = await readBody<T>(response);

    if (!body) {
        throw new ApiError(
            `서버 응답을 처리할 수 없습니다. (${response.status})`,
            { status: response.status },
        );
    }

    if (!body.success) {
        throw new ApiError(body.message, {
            status: response.status,
            code: body.code,
            details: body.errorDetail ?? [],
        });
    }

    return body.data;
}

async function readBody<T>(response: Response): Promise<ApiResponse<T> | null> {
    try {
        return await response.json();
    } catch {
        return null;
    }
}
