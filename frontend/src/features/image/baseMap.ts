import { useState } from "react";

export const BASE_MAPS = ["map", "satellite", "hybrid"] as const;
export type BaseMap = (typeof BASE_MAPS)[number];

const BASE_MAP_STORAGE_KEY = "imageMap.baseMap";

function readStoredBaseMap(): BaseMap | undefined {
    try {
        const stored = localStorage.getItem(BASE_MAP_STORAGE_KEY);
        return BASE_MAPS.find((baseMap) => baseMap === stored);
    } catch {
        return undefined;
    }
}

function storeBaseMap(baseMap: BaseMap) {
    try {
        localStorage.setItem(BASE_MAP_STORAGE_KEY, baseMap);
    } catch {
        // 저장할 수 없으면(개인정보 모드 등) 새로고침 때 유지만 안 된다
    }
}

// 바꿀 때 항상 저장되도록 set 함수는 밖에 내주지 않는다
export function useStoredBaseMap() {
    const [baseMap, setBaseMap] = useState<BaseMap>(
        () => readStoredBaseMap() ?? "map",
    );

    function changeBaseMap(next: BaseMap) {
        setBaseMap(next);
        storeBaseMap(next);
    }

    return [baseMap, changeBaseMap] as const;
}

// 지도에 쌓는 순서. Hybrid(지명)는 Satellite 위
export const VWORLD_LAYER_NAMES = [
    "Base",
    "midnight",
    "Satellite",
    "Hybrid",
] as const;
export type VworldLayer = (typeof VWORLD_LAYER_NAMES)[number];

export function visibleVworldLayers(
    baseMap: BaseMap,
    dark: boolean,
): VworldLayer[] {
    if (baseMap === "satellite") {
        return ["Satellite"];
    }
    if (baseMap === "hybrid") {
        return ["Satellite", "Hybrid"];
    }
    return [dark ? "midnight" : "Base"];
}
