"use client";

import type { ComponentProps } from "react";

import { Button } from "@/shared/components/ui/button";

type IconButtonProps = ComponentProps<typeof Button> & {
    label: string;
};

export function IconButton({ label, ...props }: IconButtonProps) {
    return (
        <Button
            variant="outline"
            size="icon-sm"
            aria-label={label}
            {...props}
        />
    );
}
