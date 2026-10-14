"use client";

import { useEffect } from "react";

type ErrorPageProps = {
    error: Error & { digest?: string };
    retry: () => void;
};

export default function ErrorPage({ error, retry }: ErrorPageProps) {
    useEffect(() => {
        console.error(error);
    }, [error]);

    return (
        <main className="flex flex-1 flex-col items-center justify-center gap-4 p-6">
            <h2 className="text-lg font-semibold">
                화면을 표시하는 중 문제가 발생했습니다.
            </h2>
            <p className="text-sm text-zinc-500">
                잠시 후 다시 시도해 주세요. 문제가 계속되면 관리자에게 문의해
                주세요.
            </p>
            <button
                type="button"
                onClick={() => retry()}
                className="rounded border px-4 py-2 hover:bg-zinc-100 dark:hover:bg-zinc-800"
            >
                다시 시도
            </button>
        </main>
    );
}
