import { ImageSearch } from "@/features/image/components/ImageSearch";

export default function Home() {
    return (
        <main className="flex flex-col gap-6 p-8">
            <h1 className="text-2xl font-bold">
                위성 영상 검색 및 아카이빙 시스템
            </h1>
            <ImageSearch />
        </main>
    );
}
