"use client";

import { ImageIcon, XIcon } from "lucide-react";
import { Fragment } from "react";

import {
    acquiredAtText,
    acquiredEndAtText,
    cloudCoverText,
    creditText,
    imageFileDetailText,
    LOOK_DIRECTION_NAMES,
    orbitDirectionText,
    providerText,
    resolutionText,
    satelliteText,
    sensorTypeText,
    sourceUpdatedAtText,
} from "@/features/image/imageText";
import { useImageDetail } from "@/features/image/queries";
import { isHttpUrl } from "@/features/image/search/fileLinks";
import {
    ImagePreviewDialog,
    PREVIEW_LABEL,
} from "@/features/image/search/ImagePreviewDialog";
import type {
    Collection,
    ImageDetailResponse,
    ImageFile,
    ImageResponse,
    Sar,
} from "@/features/image/types";
import { ErrorMessage } from "@/shared/components/ErrorMessage";
import { IconButton } from "@/shared/components/IconButton";

type PreviewProps = {
    overlayShown: boolean;
    onPreview: (image: ImageResponse) => void;
    onToggleOverlay: (image: ImageResponse) => void;
};

type ImageDetailProps = PreviewProps & {
    image: ImageResponse | null;
    onClose: () => void;
};

export function ImageDetail({ image, onClose, ...preview }: ImageDetailProps) {
    if (!image) return null;
    return (
        <aside
            className="catalog-detail flex min-h-0 min-w-0 flex-col border-l bg-background"
            aria-label="영상 상세정보"
        >
            <div className="flex shrink-0 items-center justify-between border-b px-5 py-3">
                <h2 className="text-sm font-semibold">영상 상세정보</h2>
                <IconButton label="상세 정보 닫기" onClick={onClose}>
                    <XIcon />
                </IconButton>
            </div>
            <SelectedImageDetail
                key={image.imageId}
                selected={image}
                {...preview}
            />
        </aside>
    );
}

function SelectedImageDetail({
    selected,
    ...preview
}: PreviewProps & { selected: ImageResponse }) {
    // Mount only for a selection. The keyed component and query key isolate each image.
    const { data, isPending, error, refetch } = useImageDetail(
        selected.imageId,
    );

    if (isPending) {
        return (
            <p role="status" className="p-4 text-sm">
                상세정보를 불러오는 중입니다...
            </p>
        );
    }
    if (error) {
        return (
            <div className="overflow-y-auto p-4">
                <ErrorMessage error={error} onRetry={() => void refetch()} />
            </div>
        );
    }
    if (!data || data.imageId !== selected.imageId) return null;

    return <ImageDetailContent image={data} selected={selected} {...preview} />;
}

function ImageDetailContent({
    image,
    selected,
    overlayShown,
    onPreview,
    onToggleOverlay,
}: PreviewProps & {
    image: ImageDetailResponse;
    selected: ImageResponse;
}) {
    const fileHrefs = new Set(image.files.map((file) => file.href));
    const downloadUrls = [...new Set(image.downloadUrls)].filter(
        (url) => !fileHrefs.has(url),
    );

    return (
        <div className="flex min-h-0 flex-1 flex-col gap-4 overflow-y-auto p-4">
            <div>
                <p className="mb-2 text-xs text-muted-foreground">
                    {sensorTypeText(image)} · {image.satellite}
                </p>
                <h2 className="text-lg font-semibold break-all">
                    {image.title}
                </h2>
            </div>
            <div className="flex justify-end">
                <PreviewButton
                    image={selected}
                    overlayShown={overlayShown}
                    onPreview={() => onPreview(selected)}
                    onToggleOverlay={() => onToggleOverlay(selected)}
                />
            </div>
            <h3 className="text-sm font-semibold">기본정보</h3>

            <dl className="grid grid-cols-[max-content_1fr] gap-x-3 gap-y-1 text-sm">
                <dt className="text-muted-foreground">영상 ID</dt>
                <dd>{image.imageId}</dd>
                <dt className="text-muted-foreground">원본 ID</dt>
                <dd className="break-all">{image.sourceId}</dd>
                <dt className="text-muted-foreground">위성</dt>
                <dd>{satelliteText(image)}</dd>
                <dt className="text-muted-foreground">센서 유형</dt>
                <dd>{sensorTypeText(image)}</dd>
                <dt className="text-muted-foreground">제공 기관</dt>
                <dd>
                    {image.providerCode === selected.provider.code
                        ? providerText(selected)
                        : image.providerCode}
                </dd>
                <dt className="text-muted-foreground">컬렉션</dt>
                <dd className="break-all">{image.collection.title}</dd>
                <dt className="text-muted-foreground">제품 유형</dt>
                <dd>{image.collection.productType}</dd>
                <dt className="text-muted-foreground">원본 갱신(KST)</dt>
                <dd>{sourceUpdatedAtText(image)}</dd>
                <LicenseRow collection={image.collection} />
                <CreditRow image={selected} />
            </dl>

            <section className="border-t pt-4">
                <h3 className="mb-3 text-sm font-semibold">촬영정보</h3>
                <dl className="grid grid-cols-[max-content_1fr] gap-x-3 gap-y-2 text-sm">
                    <dt className="text-muted-foreground">촬영일시(KST)</dt>
                    <dd>{acquiredAtText(image)}</dd>
                    <dt className="text-muted-foreground">촬영 종료(KST)</dt>
                    <dd>{acquiredEndAtText(image)}</dd>
                    <dt className="text-muted-foreground">해상도</dt>
                    <dd>{resolutionText(image)}</dd>
                    {image.sensorType === "OPTICAL" && (
                        <>
                            <dt className="text-muted-foreground">구름량</dt>
                            <dd>{cloudCoverText(image)}</dd>
                        </>
                    )}
                    <dt className="text-muted-foreground">궤도 방향</dt>
                    <dd>{orbitDirectionText(image)}</dd>
                </dl>
            </section>
            {image.sensorType === "SAR" && image.sar && (
                <section className="border-t pt-4">
                    <h3 className="mb-3 text-sm font-semibold">SAR 정보</h3>
                    <dl className="grid grid-cols-[max-content_1fr] gap-x-3 gap-y-2 text-sm">
                        <SarRows sar={image.sar} />
                    </dl>
                </section>
            )}
            <ImageFiles files={image.files} />

            {downloadUrls.length > 0 && (
                <section className="flex flex-col gap-2">
                    <h3 className="font-semibold">
                        추가 파일 주소 ({downloadUrls.length})
                    </h3>
                    <ul className="flex flex-col gap-1 text-sm">
                        {downloadUrls.map((url) => (
                            <li key={url}>
                                <FileAddress url={url} />
                            </li>
                        ))}
                    </ul>
                </section>
            )}
            <details className="text-sm">
                <summary className="cursor-pointer font-semibold">
                    원본 메타데이터 (JSON)
                </summary>
                <pre className="mt-3 max-h-96 overflow-auto rounded-lg border bg-muted p-3 font-mono text-xs leading-relaxed whitespace-pre-wrap break-all">
                    {JSON.stringify(image.rawMetadata, null, 2)}
                </pre>
            </details>
        </div>
    );
}

type PreviewButtonProps = {
    image: ImageResponse;
    overlayShown: boolean;
    onPreview: () => void;
    onToggleOverlay: () => void;
};

function PreviewButton({
    image,
    overlayShown,
    onPreview,
    onToggleOverlay,
}: PreviewButtonProps) {
    if (image.overlayUrl) {
        return (
            <IconButton
                label={PREVIEW_LABEL}
                title={PREVIEW_LABEL}
                aria-pressed={overlayShown}
                variant={overlayShown ? "default" : "outline"}
                className="shrink-0"
                onClick={onToggleOverlay}
            >
                <ImageIcon />
            </IconButton>
        );
    }
    if (image.thumbnailUrl) {
        return (
            <ImagePreviewDialog
                title={image.title}
                thumbnailUrl={image.thumbnailUrl}
                credit={creditText(image)}
                onOpen={onPreview}
            />
        );
    }
    return null;
}

function LicenseRow({ collection }: { collection: Collection }) {
    if (!collection.licenseName) {
        return null;
    }
    return (
        <>
            <dt className="text-muted-foreground">라이선스</dt>
            <dd className="break-all">
                <LicenseName
                    name={collection.licenseName}
                    url={collection.licenseUrl}
                />
            </dd>
        </>
    );
}

function LicenseName({ name, url }: { name: string; url: string | null }) {
    if (!url?.startsWith("http")) {
        return name;
    }
    return (
        <a
            href={url}
            target="_blank"
            rel="noopener noreferrer"
            className="underline"
        >
            {name}
        </a>
    );
}

function CreditRow({ image }: { image: ImageResponse }) {
    const credit = creditText(image);
    if (!credit) {
        return null;
    }
    return (
        <>
            <dt className="text-muted-foreground">출처</dt>
            <dd className="break-all">{credit}</dd>
        </>
    );
}

function SarRows({ sar }: { sar: Sar }) {
    const rows: [label: string, value: string | null][] = [
        ["상대 궤도", sar.relativeOrbit?.toString() ?? null],
        ["프레임", sar.frame?.toString() ?? null],
        ["촬영 모드", sar.instrumentMode],
        ["편파", sar.polarizations?.join(", ") ?? null],
        [
            "관측 방향",
            sar.lookDirection && LOOK_DIRECTION_NAMES[sar.lookDirection],
        ],
        [
            "입사각",
            sar.incidenceAngle === null ? null : `${sar.incidenceAngle}°`,
        ],
        [
            "거리 방향 해상도",
            sar.resolutionRangeMeters === null
                ? null
                : `${sar.resolutionRangeMeters} m`,
        ],
        [
            "방위 방향 해상도",
            sar.resolutionAzimuthMeters === null
                ? null
                : `${sar.resolutionAzimuthMeters} m`,
        ],
    ];
    return rows.map(([label, value]) => (
        <Fragment key={label}>
            <dt className="text-muted-foreground">{label}</dt>
            <dd>{value ?? "-"}</dd>
        </Fragment>
    ));
}

const FILE_ROLE_LABELS = {
    DATA: "영상 데이터",
    PREVIEW: "미리보기 관련 파일",
    THUMBNAIL: "썸네일",
    METADATA: "메타데이터 파일",
    ORIGINAL: "원본 패키지",
} satisfies Record<ImageFile["role"], string>;

function ImageFiles({ files }: { files: ImageFile[] }) {
    return (
        <details className="text-sm">
            <summary className="cursor-pointer font-semibold">
                파일 목록 ({files.length})
            </summary>
            <p className="mt-2 text-xs leading-relaxed text-muted-foreground">
                제공기관에서 제공하는 파일 위치와 속성입니다. 영상 데이터에는
                분석용 보조 파일도 포함될 수 있습니다. 외부 주소는 새 창에서
                열리며, S3 저장 위치는 브라우저에서 직접 열 수 없습니다.
            </p>
            {files.length === 0 ? (
                <p className="mt-2 text-muted-foreground">
                    등록된 파일이 없습니다.
                </p>
            ) : (
                Object.entries(FILE_ROLE_LABELS).map(([role, label]) => {
                    const group = files.filter((file) => file.role === role);
                    if (group.length === 0) return null;
                    return (
                        <details
                            key={role}
                            className="mt-3 rounded-lg border p-3"
                        >
                            <summary className="cursor-pointer font-semibold">
                                {label} ({group.length})
                            </summary>
                            <ul className="flex flex-col gap-2">
                                {group.map((file) => (
                                    <li
                                        key={file.fileId}
                                        className="rounded border p-2"
                                    >
                                        <dl className="grid grid-cols-[max-content_1fr] gap-x-2 gap-y-1 break-all">
                                            <dt>파일 식별자</dt>
                                            <dd>{file.assetKey}</dd>
                                            <dt>파일 역할</dt>
                                            <dd>
                                                {FILE_ROLE_LABELS[file.role]} (
                                                {file.role})
                                            </dd>
                                            <dt>파일 형식</dt>
                                            <dd>
                                                {file.mediaType ?? "정보 없음"}
                                            </dd>
                                            <dt>픽셀 간격</dt>
                                            <dd>
                                                {file.pixelSpacingMeters ===
                                                null
                                                    ? "정보 없음"
                                                    : `${file.pixelSpacingMeters} m`}
                                            </dd>
                                            <dt>밴드 정보</dt>
                                            <dd>
                                                {imageFileDetailText({
                                                    pixelSpacingMeters: null,
                                                    bands: file.bands,
                                                }) || "정보 없음"}
                                            </dd>
                                            <dt>파일 위치</dt>
                                            <dd>
                                                <FileAddress url={file.href} />
                                            </dd>
                                        </dl>
                                    </li>
                                ))}
                            </ul>
                        </details>
                    );
                })
            )}
        </details>
    );
}

function FileAddress({ url }: { url: string }) {
    const locationLabel = url.startsWith("s3://")
        ? "S3 저장 위치 · 직접 열기 불가"
        : "파일 위치 · 직접 열기 불가";
    if (!isHttpUrl(url)) {
        return (
            <span
                className="flex flex-col gap-1 break-all text-muted-foreground"
                title={url}
            >
                <span className="text-xs">{locationLabel}</span>
                <span>{url}</span>
            </span>
        );
    }

    return (
        <span className="flex flex-col gap-1">
            <span className="text-xs text-muted-foreground">
                외부 주소 · 새 창에서 열기
            </span>
            <a
                href={url}
                target="_blank"
                rel="noopener noreferrer"
                className="break-all underline"
                title={url}
            >
                {url}
            </a>
        </span>
    );
}
