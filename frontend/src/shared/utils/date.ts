const KST_OFFSET_MS = 9 * 60 * 60 * 1000;

export function toKstDate(isoDateTime: string): string {
    return new Date(Date.parse(isoDateTime) + KST_OFFSET_MS)
        .toISOString()
        .slice(0, 10);
}

const KST_DATE_TIME_FORMAT = new Intl.DateTimeFormat("ko-KR", {
    timeZone: "Asia/Seoul",
    dateStyle: "medium",
    timeStyle: "short",
});

export function formatKstDateTime(isoDateTime: string): string {
    return KST_DATE_TIME_FORMAT.format(new Date(isoDateTime));
}
