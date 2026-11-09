import { createLoader, createSerializer } from "nuqs/server";
import { describe, expect, it } from "vitest";

import {
    fromSearchValues,
    imageSearchParsers,
    toSearchValues,
} from "@/features/image/search/searchParams";

describe("카탈로그 검색 URL", () => {
    it("컬렉션·제품 유형과 기존 조건을 직렬화하고 복원한다", () => {
        const conditions = {
            keyword: "서울 영상",
            collectionIds: [1, 20],
            productTypes: ["L1", "L2A"],
            providers: ["test"],
            isPaid: false,
            cloudMax: 0,
            bbox: [126, 37, 128, 38] as [number, number, number, number],
        };
        const url = createSerializer(imageSearchParsers)(
            toSearchValues(conditions),
        );
        const restored = createLoader(imageSearchParsers)(
            new URLSearchParams(url),
        );
        expect(fromSearchValues(restored)).toMatchObject(conditions);
    });
    it("조건을 비우면 이전 컬렉션과 제품 유형도 제거한다", () => {
        const values = toSearchValues({});
        expect(values.collectionIds).toBeNull();
        expect(values.productTypes).toBeNull();
        expect(fromSearchValues(values).collectionIds).toBeUndefined();
    });
});
