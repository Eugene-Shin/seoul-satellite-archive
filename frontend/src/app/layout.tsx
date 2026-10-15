import type { Metadata } from "next";
import { Noto_Sans_KR } from "next/font/google";

import "./globals.css";
import { Providers } from "@/app/providers";

const notoSansKr = Noto_Sans_KR({
    variable: "--font-sans",
    subsets: ["latin"],
});

export const metadata: Metadata = {
    title: "Seoul Satellite Archive",
    description: "서울 지역 위성 영상 검색 및 아카이빙 시스템",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
    return (
        <html lang="ko" className={`${notoSansKr.variable} h-full antialiased`}>
            <body className="min-h-full flex flex-col">
                <Providers>{children}</Providers>
            </body>
        </html>
    );
}
