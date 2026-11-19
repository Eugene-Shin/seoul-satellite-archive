"use client";

import { CircleAlertIcon } from "lucide-react";
import { toast } from "sonner";

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
    onRetry?: () => void;
};

// 오류에서 보일 문구. 서버 오류면 사유(errorDetail)까지
function describeError(error: unknown): { message: string; details: string[] } {
    return {
        message:
            error instanceof Error
                ? error.message
                : "알 수 없는 오류가 발생했습니다.",
        details: error instanceof ApiError ? error.details : [],
    };
}

export function toastError(error: unknown) {
    const { message, details } = describeError(error);
    toast.error(message, { description: details.join(" ") || undefined });
}

export function ErrorMessage({ error, onRetry }: ErrorMessageProps) {
    if (!error) {
        return null;
    }

    const { message, details } = describeError(error);

    return (
        <Alert
            variant="destructive"
            className="border-destructive/50 bg-destructive/10"
        >
            <CircleAlertIcon />
            <AlertTitle>{message}</AlertTitle>
            <AlertDescription>
                {/* 상세가 많으면(응답 검증 실패 등) 패널을 밀어내지 않고 이 안에서 스크롤한다 */}
                <ul className="max-h-40 list-disc overflow-y-auto pl-5">
                    {details.map((detail) => (
                        <li key={detail}>{detail}</li>
                    ))}
                </ul>
            </AlertDescription>
            {onRetry && (
                <AlertAction>
                    <Button variant="outline" size="sm" onClick={onRetry}>
                        다시 시도
                    </Button>
                </AlertAction>
            )}
        </Alert>
    );
}
