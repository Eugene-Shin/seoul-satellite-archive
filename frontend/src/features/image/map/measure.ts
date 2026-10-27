export function formatLength(meters: number): string {
    return meters < 1000
        ? `${Math.round(meters)} m`
        : `${(meters / 1000).toFixed(2)} km`;
}

export function formatArea(squareMeters: number): string {
    return squareMeters < 1_000_000
        ? `${Math.round(squareMeters).toLocaleString("ko-KR")} m²`
        : `${(squareMeters / 1_000_000).toFixed(2)} km²`;
}
