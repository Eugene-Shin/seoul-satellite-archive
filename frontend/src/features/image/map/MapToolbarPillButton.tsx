"use client";

import type { ComponentProps } from "react";

import { Button } from "@/shared/components/ui/button";

type MapToolbarPillButtonProps = ComponentProps<typeof Button> & {
    label: string;
};

export function MapToolbarPillButton({
    label,
    ...props
}: MapToolbarPillButtonProps) {
    return (
        <Button
            variant="secondary"
            size="sm"
            aria-label={label}
            title={label}
            className="rounded-full px-3 shadow-sm"
            {...props}
        />
    );
}
