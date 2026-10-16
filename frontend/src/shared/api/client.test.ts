import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { z } from "zod";

import { ApiError, apiGet } from "@/shared/api/client";

const itemSchema = z.object({ id: z.number() });

function jsonResponse(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), { status });
}

function mockFetch(response: Response) {
    const fetchMock = vi.fn().mockResolvedValue(response);
    vi.stubGlobal("fetch", fetchMock);
    return fetchMock;
}

async function catchError(promise: Promise<unknown>): Promise<ApiError> {
    const error = await promise.catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    return error as ApiError;
}

beforeEach(() => {
    vi.stubEnv("NEXT_PUBLIC_API_URL", "http://api.test");
});

afterEach(() => {
    vi.unstubAllEnvs();
    vi.unstubAllGlobals();
});

describe("apiGet", () => {
    it("배열은 펼치고 빈 값은 빼서 요청한다", async () => {
        const fetchMock = mockFetch(
            jsonResponse({
                success: true,
                code: "OK",
                message: "",
                data: { id: 1 },
            }),
        );

        await apiGet("/api/images", itemSchema, {
            satellites: ["A", "B"],
            keyword: "",
            isPaid: false,
            gsdMin: null,
            cloudMax: undefined,
        });

        const url: URL = fetchMock.mock.calls[0][0];
        expect(url.toString()).toBe(
            "http://api.test/api/images?satellites=A&satellites=B&isPaid=false",
        );
    });

    it("성공하면 스키마로 검증한 data를 돌려준다", async () => {
        mockFetch(
            jsonResponse({
                success: true,
                code: "OK",
                message: "",
                data: { id: 1 },
            }),
        );

        await expect(apiGet("/x", itemSchema)).resolves.toEqual({ id: 1 });
    });

    it("백엔드가 실패라고 하면 그 메시지와 코드로 ApiError", async () => {
        mockFetch(
            jsonResponse(
                {
                    success: false,
                    code: "INVALID_PARAM",
                    message: "잘못된 조건입니다.",
                    data: null,
                    errorDetail: ["gsdMin: 0 이상"],
                },
                400,
            ),
        );

        const error = await catchError(apiGet("/x", itemSchema));
        expect(error.message).toBe("잘못된 조건입니다.");
        expect(error.status).toBe(400);
        expect(error.code).toBe("INVALID_PARAM");
        expect(error.details).toEqual(["gsdMin: 0 이상"]);
    });

    it("data 형식이 다르면 필드 경로를 담은 ApiError", async () => {
        mockFetch(
            jsonResponse({
                success: true,
                code: "OK",
                message: "",
                data: { id: "1" },
            }),
        );

        const error = await catchError(apiGet("/x", itemSchema));
        expect(error.message).toBe("서버 응답 형식이 올바르지 않습니다.");
        expect(error.details[0]).toMatch(/^id: /);
    });

    it("응답이 JSON이 아니면 상태 코드를 담은 ApiError", async () => {
        mockFetch(new Response("<html>Bad Gateway</html>", { status: 502 }));

        const error = await catchError(apiGet("/x", itemSchema));
        expect(error.message).toBe("서버 응답을 처리할 수 없습니다. (502)");
        expect(error.status).toBe(502);
    });

    it("네트워크 실패면 연결 실패 ApiError", async () => {
        vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError()));

        const error = await catchError(apiGet("/x", itemSchema));
        expect(error.message).toBe(
            "서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.",
        );
        expect(error.status).toBeNull();
    });

    it("취소 신호를 fetch에 넘긴다", async () => {
        const fetchMock = mockFetch(
            jsonResponse({
                success: true,
                code: "OK",
                message: "",
                data: { id: 1 },
            }),
        );
        const controller = new AbortController();

        await apiGet("/x", itemSchema, {}, controller.signal);

        expect(fetchMock.mock.calls[0][1]).toEqual({
            signal: controller.signal,
        });
    });

    it("취소된 요청은 ApiError로 감싸지 않는다", async () => {
        const controller = new AbortController();
        controller.abort();
        const abortError = new DOMException("Aborted", "AbortError");
        vi.stubGlobal("fetch", vi.fn().mockRejectedValue(abortError));

        await expect(
            apiGet("/x", itemSchema, {}, controller.signal),
        ).rejects.toBe(abortError);
    });
});
