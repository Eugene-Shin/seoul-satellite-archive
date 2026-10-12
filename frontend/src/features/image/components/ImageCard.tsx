import {
    ORBIT_DIRECTION_LABELS,
    SENSOR_TYPE_LABELS,
} from "@/features/image/labels";
import type { ImageResponse } from "@/features/image/types";
import { formatKstDateTime } from "@/shared/utils/date";

type ImageCardProps = {
    image: ImageResponse;
};

export function ImageCard({ image }: ImageCardProps) {
    const satellite = image.instruments
        ? `${image.satellite} (${image.instruments.join(", ")})`
        : image.satellite;
    const gsd = image.gsdMeters === null ? "-" : `${image.gsdMeters} m`;
    const cloudCover = image.cloudCover === null ? "-" : `${image.cloudCover}%`;
    const orbitDirection = image.orbitDirection
        ? ORBIT_DIRECTION_LABELS[image.orbitDirection]
        : "-";
    const provider = image.provider.isPaid
        ? `${image.provider.name} (유료)`
        : `${image.provider.name} (무료)`;

    return (
        <li className="flex flex-col gap-2 rounded border p-4">
            <h2 className="truncate font-semibold" title={image.title}>
                {image.title}
            </h2>
            <dl className="grid grid-cols-[max-content_1fr] gap-x-3 gap-y-1 text-sm">
                <dt className="text-zinc-500">위성</dt>
                <dd>{satellite}</dd>
                <dt className="text-zinc-500">센서 유형</dt>
                <dd>{SENSOR_TYPE_LABELS[image.sensorType]}</dd>
                <dt className="text-zinc-500">촬영일시(KST)</dt>
                <dd>{formatKstDateTime(image.acquiredAt)}</dd>
                <dt className="text-zinc-500">해상도</dt>
                <dd>{gsd}</dd>
                <dt className="text-zinc-500">구름량</dt>
                <dd>{cloudCover}</dd>
                <dt className="text-zinc-500">궤도 방향</dt>
                <dd>{orbitDirection}</dd>
                <dt className="text-zinc-500">제공 기관</dt>
                <dd>{provider}</dd>
            </dl>
        </li>
    );
}
