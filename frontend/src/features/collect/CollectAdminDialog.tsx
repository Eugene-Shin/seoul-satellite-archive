"use client";

import { CalendarClockIcon } from "lucide-react";

import { CollectTargetTable } from "@/features/collect/CollectTargetTable";
import { MapToolbarPillButton } from "@/features/image/map/MapToolbarPillButton";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/shared/components/ui/dialog";

const LABEL = "수집 관리";

export function CollectAdminDialog() {
    return (
        <Dialog>
            <DialogTrigger render={<MapToolbarPillButton label={LABEL} />}>
                <CalendarClockIcon />
            </DialogTrigger>
            <DialogContent className="flex max-h-[80vh] w-[90vw] max-w-4xl flex-col sm:max-w-4xl">
                <DialogHeader>
                    <DialogTitle>{LABEL}</DialogTitle>
                </DialogHeader>
                <CollectTargetTable />
            </DialogContent>
        </Dialog>
    );
}
