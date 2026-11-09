export function isHttpUrl(value: string | null): value is string {
    if (!value || !/^https?:\/\//i.test(value)) return false;
    try {
        const url = new URL(value);
        return (
            (url.protocol === "https:" || url.protocol === "http:") &&
            !url.username &&
            !url.password
        );
    } catch {
        return false;
    }
}
