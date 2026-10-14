import { Suspense } from "react";

import { ImageSearch } from "@/features/image/components/ImageSearch";

export default function Home() {
    return (
        <div className="flex h-dvh flex-col">
            <header className="shrink-0 border-b px-6 py-4">
                <h1 className="text-xl font-bold">
                    위성 영상 검색 및 아카이빙 시스템
                </h1>
            </header>
            <Suspense fallback={<p>검색 화면을 불러오는 중입니다...</p>}>
                <ImageSearch />
            </Suspense>
        </div>
    );
}
