export const BASE_MAPS = ["map", "satellite", "hybrid"] as const;
export type BaseMap = (typeof BASE_MAPS)[number];

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
