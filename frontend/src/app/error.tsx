"use client";

import { useEffect } from "react";

import { Button } from "@/shared/components/ui/button";

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
            <Button variant="outline" onClick={() => retry()}>
                다시 시도
            </Button>
        </main>
    );
}
