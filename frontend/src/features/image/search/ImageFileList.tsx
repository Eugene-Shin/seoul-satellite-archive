"use client";

import {
    IMAGE_FILE_ROLE_NAMES,
    imageFileDetailText,
} from "@/features/image/imageText";
import { useImageDetail } from "@/features/image/queries";
import { imageFileRoleSchema } from "@/features/image/schemas";
import type { ImageFile } from "@/features/image/types";
import { ErrorMessage } from "@/shared/components/ErrorMessage";

export function ImageFileList({ imageId }: { imageId: number }) {
    const { data, isPending, error, refetch } = useImageDetail(imageId);

    if (error) {
        return <ErrorMessage error={error} onRetry={() => refetch()} />;
    }
    if (isPending) {
        return (
            <p className="text-sm text-muted-foreground">
                파일 목록을 불러오는 중입니다...
            </p>
        );
    }

    return (
        <section className="flex flex-col gap-3">
            <h3 className="font-semibold">파일 ({data.files.length})</h3>
            {imageFileRoleSchema.options.map((role) => {
                const files = data.files.filter((file) => file.role === role);
                if (files.length === 0) {
                    return null;
                }
                return (
                    <div key={role} className="flex flex-col gap-1 text-sm">
                        <h4 className="font-medium">
                            {IMAGE_FILE_ROLE_NAMES[role]} ({files.length})
                        </h4>
                        <ul className="flex flex-col gap-1">
                            {files.map((file) => (
                                <FileRow key={file.fileId} file={file} />
                            ))}
                        </ul>
                    </div>
                );
            })}
        </section>
    );
}

function FileRow({ file }: { file: ImageFile }) {
    const detail = imageFileDetailText(file);

    return (
        <li className="flex flex-wrap gap-x-2">
            <FileLink file={file} />
            {detail && <span className="text-muted-foreground">{detail}</span>}
        </li>
    );
}

function FileLink({ file }: { file: ImageFile }) {
    if (!file.href.startsWith("http")) {
        return (
            <span className="break-all text-muted-foreground" title={file.href}>
                {file.assetKey}
            </span>
        );
    }

    return (
        <a
            href={file.href}
            target="_blank"
            rel="noopener noreferrer"
            className="break-all underline"
            title={file.href}
        >
            {file.assetKey}
        </a>
    );
}
