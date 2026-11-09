import { Suspense } from "react";

import { ImageSearch } from "@/features/image/search/ImageSearch";

export default function Home() {
    return (
        <main className="h-dvh min-h-0 overflow-hidden">
            <Suspense
                fallback={
                    <p className="area-panel p-4">
                        검색 화면을 불러오는 중입니다...
                    </p>
                }
            >
                <ImageSearch />
            </Suspense>
        </main>
    );
}
