import { SatelliteIcon } from "lucide-react";
import { Suspense } from "react";

import { ImageSearch } from "@/features/image/components/ImageSearch";

export default function Home() {
    return (
        <div className="app-layout h-dvh">
            <header className="area-header flex items-center justify-center gap-2 border-r border-b px-4 py-3">
                <SatelliteIcon className="size-5" />
                <h1 className="text-lg font-semibold">
                    위성 영상 검색 및 아카이빙 시스템
                </h1>
            </header>
            <Suspense
                fallback={
                    <p className="area-panel p-4">
                        검색 화면을 불러오는 중입니다...
                    </p>
                }
            >
                <ImageSearch />
            </Suspense>
        </div>
    );
}
