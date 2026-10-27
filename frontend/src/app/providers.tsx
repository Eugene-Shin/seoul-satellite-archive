"use client";

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { NuqsAdapter } from "nuqs/adapters/next/app";
import { useState } from "react";
import { Toaster } from "sonner";

import { ApiError } from "@/shared/api/client";

const MAX_RETRIES = 2;

function shouldRetry(failureCount: number, error: Error): boolean {
    const isClientError =
        error instanceof ApiError &&
        error.status !== null &&
        error.status < 500;
    return !isClientError && failureCount < MAX_RETRIES;
}

export function Providers({ children }: { children: React.ReactNode }) {
    const [queryClient] = useState(
        () =>
            new QueryClient({
                defaultOptions: {
                    queries: {
                        retry: shouldRetry,
                        refetchOnWindowFocus: false,
                    },
                },
            }),
    );

    return (
        <NuqsAdapter>
            <QueryClientProvider client={queryClient}>
                {children}
                <Toaster theme="system" position="bottom-center" />
            </QueryClientProvider>
        </NuqsAdapter>
    );
}
