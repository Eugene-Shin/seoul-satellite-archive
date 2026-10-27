"use client";

import { SettingsIcon } from "lucide-react";

import { ImageAdminTable } from "@/features/image/admin/ImageAdminTable";
import { MapToolbarPillButton } from "@/features/image/map/MapToolbarPillButton";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/shared/components/ui/dialog";

const LABEL = "영상 관리";

export function ImageAdminDialog() {
    return (
        <Dialog>
            <DialogTrigger render={<MapToolbarPillButton label={LABEL} />}>
                <SettingsIcon />
            </DialogTrigger>
            <DialogContent className="flex h-[90vh] w-[90vw] max-w-none flex-col sm:max-w-none">
                <DialogHeader>
                    <DialogTitle>{LABEL}</DialogTitle>
                </DialogHeader>
                <ImageAdminTable />
            </DialogContent>
        </Dialog>
    );
}
