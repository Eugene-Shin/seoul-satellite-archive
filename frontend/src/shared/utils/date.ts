const KST_OFFSET_MS = 9 * 60 * 60 * 1000;

export function toKstDate(isoDateTime: string): string {
    return new Date(Date.parse(isoDateTime) + KST_OFFSET_MS)
        .toISOString()
        .slice(0, 10);
}
