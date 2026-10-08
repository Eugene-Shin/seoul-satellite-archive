import type { ApiResponse } from "@/shared/api/types";

type QueryValue = string | number | boolean | null | undefined;

export type QueryParams = Record<string, QueryValue | QueryValue[]>;

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
    const response = await fetch(url);
    const body: ApiResponse<T> = await response.json();

    if (!body.success) {
        throw new Error(body.errorDetail?.join(", ") ?? body.message);
    }

    return body.data;
}
