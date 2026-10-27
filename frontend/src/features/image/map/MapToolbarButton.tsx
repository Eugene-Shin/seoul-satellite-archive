"use client";

import type { ComponentProps } from "react";

import { IconButton } from "@/shared/components/IconButton";

export function MapToolbarButton(props: ComponentProps<typeof IconButton>) {
    return (
        <IconButton
            variant="secondary"
            className="shadow-sm"
            title={props.label}
            {...props}
        />
    );
}
