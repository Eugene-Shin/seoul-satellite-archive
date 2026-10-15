"use client";

import { CircleAlertIcon } from "lucide-react";

import { ApiError } from "@/shared/api/client";
import {
    Alert,
    AlertAction,
    AlertDescription,
    AlertTitle,
} from "@/shared/components/ui/alert";
import { Button } from "@/shared/components/ui/button";

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
        <Alert
            variant="destructive"
            className="border-destructive/50 bg-destructive/10"
        >
            <CircleAlertIcon />
            <AlertTitle>{message}</AlertTitle>
            <AlertDescription>
                <ul className="list-disc pl-5">
                    {details.map((detail) => (
                        <li key={detail}>{detail}</li>
                    ))}
                </ul>
            </AlertDescription>
            <AlertAction>
                <Button variant="outline" size="sm" onClick={onRetry}>
                    다시 시도
                </Button>
            </AlertAction>
        </Alert>
    );
}
