"use client";

import { SettingsIcon } from "lucide-react";
import { useState } from "react";

import { ImageAdminTable } from "@/features/image/admin/ImageAdminTable";
import { MapToolbarPillButton } from "@/features/image/map/MapToolbarPillButton";
import type { ImageSearchParams } from "@/features/image/types";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/shared/components/ui/dialog";

const LABEL = "영상 관리";

export function ImageAdminDialog() {
    // 팝업을 닫아도 필터가 남도록 DialogContent 바깥에 둔다
    const [conditions, setConditions] = useState<ImageSearchParams>({});

    return (
        <Dialog>
            <DialogTrigger render={<MapToolbarPillButton label={LABEL} />}>
                <SettingsIcon />
            </DialogTrigger>
            <DialogContent className="flex h-[90vh] w-[90vw] max-w-none flex-col sm:max-w-none">
                <DialogHeader>
                    <DialogTitle>{LABEL}</DialogTitle>
                </DialogHeader>
                <ImageAdminTable
                    conditions={conditions}
                    onConditionsChange={setConditions}
                />
            </DialogContent>
        </Dialog>
    );
}
