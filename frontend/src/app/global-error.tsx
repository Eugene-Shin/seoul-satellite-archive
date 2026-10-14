"use client";

import { useEffect } from "react";

type GlobalErrorProps = {
    error: Error & { digest?: string };
    retry: () => void;
};

export default function GlobalError({ error, retry }: GlobalErrorProps) {
    useEffect(() => {
        console.error(error);
    }, [error]);

    return (
        <html lang="ko">
            <body>
                <h2>화면을 표시하는 중 문제가 발생했습니다.</h2>
                <button type="button" onClick={() => retry()}>
                    다시 시도
                </button>
            </body>
        </html>
    );
}
