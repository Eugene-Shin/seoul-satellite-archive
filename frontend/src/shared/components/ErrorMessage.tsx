"use client";

import { ApiError } from "@/shared/api/client";

type ErrorMessageProps = {
    error: unknown;
    onRetry: () => void;
};

export function ErrorMessage({ error, onRetry }: ErrorMessageProps) {
    if (!error) {
        return null;
    }

    const message =
        error instanceof Error
            ? error.message
            : "알 수 없는 오류가 발생했습니다.";
    const details = error instanceof ApiError ? error.details : [];

    return (
        <div
            role="alert"
            className="flex flex-col gap-2 rounded border border-red-300 bg-red-50 p-3 text-sm text-red-700 dark:border-red-800 dark:bg-red-950 dark:text-red-300"
        >
            <p>{message}</p>
            <ul className="list-disc pl-5">
                {details.map((detail) => (
                    <li key={detail}>{detail}</li>
                ))}
            </ul>
            <button
                type="button"
                onClick={onRetry}
                className="self-start rounded border border-current px-3 py-1 hover:bg-red-100 dark:hover:bg-red-900"
            >
                다시 시도
            </button>
        </div>
    );
}
