import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderToStaticMarkup } from "react-dom/server";
import { afterEach, describe, expect, it, vi } from "vitest";

import { fetchImageDetail } from "@/features/image/api";
import {
    imageDetailSchema,
    imageFileSchema,
    imageResponseSchema,
} from "@/features/image/schemas";
import { isHttpUrl } from "@/features/image/search/fileLinks";
import { ImageCard } from "@/features/image/search/ImageCard";
import { ImageDetail } from "@/features/image/search/ImageDetail";
import type { ImageResponse } from "@/features/image/types";

const image = imageResponseSchema.parse({
    imageId: 1,
    provider: {
        code: "test",
        name: "기관",
        isPaid: false,
        ownOrganization: false,
    },
    collection: {
        collectionId: 1,
        providerCode: "test",
        code: "test",
        title: "컬렉션",
        productType: "L1",
        licenseName: null,
        licenseUrl: null,
        creditText: null,
    },
    title: "선택 영상",
    satellite: "위성",
    instruments: null,
    sensorType: "SAR",
    acquiredAt: "2026-11-04T00:00:00.123456Z",
    acquiredEndAt: null,
    resolution: null,
    cloudCover: null,
    orbitDirection: null,
    sar: null,
    footprint: { type: "MultiPolygon", coordinates: [] },
    sourceUpdatedAt: null,
    thumbnailUrl: null,
    overlayUrl: null,
});
const file = {
    fileId: 1,
    assetKey: "red",
    role: "DATA",
    href: "s3://archive/red.tif",
    mediaType: "image/tiff",
    pixelSpacingMeters: 10.25,
    bands: [],
};
const detail = imageDetailSchema.parse({
    ...image,
    providerCode: "test",
    sourceId: "source",
    url: "s3://archive/source.json",
    title: "API 상세 영상",
    rawMetadata: { source: "original" },
    files: [file],
    downloadUrls: [
        file.href,
        "https://example.com/extra.zip",
        "https://example.com/extra.zip",
    ],
    createdAt: "2026-11-04T00:00:00Z",
    updatedAt: "2026-11-04T00:00:00Z",
});
const noop = () => {};

afterEach(() => {
    vi.unstubAllEnvs();
    vi.unstubAllGlobals();
});

describe("카탈로그 응답 계약", () => {
    it("기존 상세 API로 선택 ID와 취소 signal을 전달하고 새 필드를 파싱한다", async () => {
        vi.stubEnv("NEXT_PUBLIC_API_URL", "http://api.test");
        const fetchMock = vi.fn().mockResolvedValue(
            new Response(
                JSON.stringify({
                    success: true,
                    code: "OK",
                    message: "",
                    data: detail,
                }),
            ),
        );
        vi.stubGlobal("fetch", fetchMock);
        const controller = new AbortController();
        expect(await fetchImageDetail(1, controller.signal)).toEqual(detail);
        expect(String(fetchMock.mock.calls[0][0])).toBe(
            "http://api.test/api/images/1",
        );
        expect(fetchMock.mock.calls[0][1].signal).toBe(controller.signal);
    });
    it("nullable 썸네일과 Instant의 소수 초를 유지한다", () => {
        expect(image.thumbnailUrl).toBeNull();
        expect(image.acquiredAt).toBe("2026-11-04T00:00:00.123456Z");
        expect(
            imageResponseSchema.safeParse({ ...image, thumbnailUrl: undefined })
                .success,
        ).toBe(false);
    });
    it("파일의 숫자와 null, 모든 역할 및 빈 목록을 허용한다", () => {
        for (const role of [
            "DATA",
            "PREVIEW",
            "THUMBNAIL",
            "METADATA",
            "ORIGINAL",
        ]) {
            expect(
                imageFileSchema.parse({
                    ...file,
                    role,
                    mediaType: null,
                    pixelSpacingMeters: null,
                }).role,
            ).toBe(role);
        }
        expect(imageDetailSchema.parse({ ...detail, files: [] }).files).toEqual(
            [],
        );
        expect(
            imageFileSchema.safeParse({ ...file, pixelSpacingMeters: "10.25" })
                .success,
        ).toBe(false);
        expect(
            imageFileSchema.safeParse({ ...file, role: "OTHER" }).success,
        ).toBe(false);
        expect(
            imageDetailSchema.safeParse({ ...detail, createdAt: "invalid" })
                .success,
        ).toBe(false);
    });
});

describe("썸네일과 링크", () => {
    it.each([
        null,
        "s3://archive/a.jpg",
        "javascript:alert(1)",
        "data:image/png;base64,x",
        "//example.com/a",
        "https://",
        "httpx://example.com/a",
        "https://user:pass@example.com/a",
    ])("직접 표시할 수 없는 URL을 거부한다: %s", (url) => {
        expect(isHttpUrl(url)).toBe(false);
    });
    it.each(["https://example.com/a.jpg", "http://example.com/a.jpg"])(
        "HTTP(S) 주소를 허용한다: %s",
        (url) => {
            expect(isHttpUrl(url)).toBe(true);
        },
    );
    function card(thumbnailUrl: string | null) {
        return renderToStaticMarkup(
            <ImageCard
                image={{ ...image, thumbnailUrl }}
                selected
                hovered={false}
                onSelect={noop}
                onHoverStart={noop}
                onHoverEnd={noop}
            />,
        );
    }
    it("HTTP 썸네일과 기존 카드 선택 상태를 표시한다", () => {
        const html = card("https://example.com/thumb.jpg");
        expect(html).toContain('src="https://example.com/thumb.jpg"');
        expect(html).toContain('aria-pressed="true"');
        expect(html).toContain("선택 영상");
    });
    it("썸네일이 없거나 S3 주소인 SAR 카드에 대체 표시한다", () => {
        for (const url of [null, "s3://archive/thumb.jpg"]) {
            const html = card(url);
            expect(html).not.toContain("<img");
            expect(html).toContain("썸네일 없음");
            expect(html).toContain("SAR");
        }
    });
});

describe("선택 영상의 상세 패널", () => {
    function render(
        selected: ImageResponse | null,
        client = new QueryClient(),
        overlayShown = false,
    ) {
        return renderToStaticMarkup(
            <QueryClientProvider client={client}>
                <ImageDetail
                    image={selected}
                    onClose={noop}
                    overlayShown={overlayShown}
                    onPreview={noop}
                    onToggleOverlay={noop}
                />
            </QueryClientProvider>,
        );
    }
    it("선택하지 않으면 상세 쿼리를 생성하지 않는다", () => {
        const client = new QueryClient();
        render(null, client);
        expect(client.getQueryCache().getAll()).toHaveLength(0);
    });
    it("선택한 ID로 조회하며 이전 ID의 캐시를 표시하지 않는다", () => {
        const client = new QueryClient();
        client.setQueryData(["image", "detail", 1], detail);
        const html = render({ ...image, imageId: 2 }, client);
        expect(html).toContain("상세정보를 불러오는 중");
        expect(html).not.toContain("API 상세 영상");
        expect(client.getQueryState(["image", "detail", 2])).toBeDefined();
    });
    it("API 기본정보, 역할별 파일, 원본 JSON과 중복 없는 추가 링크를 표시한다", () => {
        const client = new QueryClient();
        const roles = ["DATA", "PREVIEW", "THUMBNAIL", "METADATA", "ORIGINAL"];
        client.setQueryData(["image", "detail", 1], {
            ...detail,
            files: roles.map((role, i) => ({
                ...file,
                fileId: i + 1,
                role,
                href: i === 0 ? file.href : `s3://archive/${i}`,
            })),
        });
        const html = render(image, client);
        for (const label of [
            "API 상세 영상",
            "영상 데이터",
            "미리보기 관련 파일",
            "썸네일",
            "메타데이터 파일",
            "원본 패키지",
            "원본 메타데이터 (JSON)",
            "10.25 m",
            "original",
        ])
            expect(html).toContain(label);
        expect(html).toContain("추가 파일 주소 (1)");
        expect(html).toContain("S3 저장 위치 · 직접 열기 불가");
        expect(html).toContain("파일 식별자");
        expect(html).not.toContain('href="s3://');
        expect(html).toContain('href="https://example.com/extra.zip"');
        expect(html).toContain('rel="noopener noreferrer"');
    });
    it("파일이 없으면 빈 상태를 표시한다", () => {
        const client = new QueryClient();
        client.setQueryData(["image", "detail", 1], { ...detail, files: [] });
        expect(render(image, client)).toContain("등록된 파일이 없습니다.");
    });
    it("원격의 밴드·라이선스 정보와 COG 겹치기 상태를 유지한다", () => {
        const selected = {
            ...image,
            overlayUrl: "https://example.com/preview.tif",
            collection: {
                ...image.collection,
                licenseName: "Test license",
                licenseUrl: "https://example.com/license",
                creditText: "Test credit",
            },
        };
        const band = {
            bandIndex: 1,
            bandName: "B04",
            commonName: "red",
            centerWavelengthUm: 0.665,
            bandwidthUm: 0.038,
            resolutionMeters: 10,
        };
        const response = imageDetailSchema.parse({
            ...detail,
            collection: selected.collection,
            files: [{ ...file, bands: [band] }],
        });
        expect(response.files[0].bands).toEqual([band]);
        const client = new QueryClient();
        client.setQueryData(["image", "detail", 1], response);
        const html = render(selected, client, true);
        expect(html).toContain("B04");
        expect(html).toContain("Test license");
        expect(html).toContain('href="https://example.com/license"');
        expect(html).toContain("Test credit 2026");
        expect(html).toContain('aria-pressed="true"');
    });
    it("추가 파일 주소가 없으면 다운로드가 없는 것처럼 보이는 빈 영역을 표시하지 않는다", () => {
        const client = new QueryClient();
        client.setQueryData(["image", "detail", 1], {
            ...detail,
            downloadUrls: [file.href],
        });
        const html = render(image, client);
        expect(html).not.toContain("추가 파일 주소 (");
        expect(html).not.toContain("다운로드 (");
        expect(html).toContain("파일 목록 (1)");
    });
    it("오류와 재시도를 표시한다", () => {
        const client = new QueryClient({
            defaultOptions: { queries: { retryOnMount: false } },
        });
        client
            .getQueryCache()
            .build(client, { queryKey: ["image", "detail", 1] })
            .setState({ status: "error", error: new Error("상세 조회 실패") });
        const html = render(image, client);
        expect(html).toContain("상세 조회 실패");
        expect(html).toContain("다시 시도");
    });
});
