export type SensorType = "OPTICAL" | "SAR";

export type OrbitDirection = "ASCENDING" | "DESCENDING";

export type Provider = {
    code: string;
    name: string;
    isPaid: boolean;
};

export type MultiPolygon = {
    type: "MultiPolygon";
    coordinates: number[][][][];
};

export type ImageResponse = {
    imageId: number;
    provider: Provider;
    title: string;
    satellite: string;
    instruments: string[] | null;
    sensorType: SensorType;
    acquiredAt: string;
    gsdMeters: number | null;
    cloudCover: number | null;
    orbitDirection: OrbitDirection | null;
    footprint: MultiPolygon;
    downloadUrls: string[];
    sourceUpdatedAt: string | null;
};

export type Range<T> = {
    min: T | null;
    max: T | null;
};

export type ImageFilterOptionsResponse = {
    satellites: string[];
    instruments: string[];
    providers: Provider[];
    sensorTypes: SensorType[];
    orbitDirections: OrbitDirection[];
    gsdRange: Range<number>;
    acquiredRange: Range<string>;
};

export type ImageSort =
    | "ACQUIRED_AT_ASC"
    | "ACQUIRED_AT_DESC"
    | "CLOUD_COVER_ASC"
    | "CLOUD_COVER_DESC";

export type ImageSearchParams = {
    keyword?: string;
    acquiredFrom?: string;
    acquiredTo?: string;
    satellites?: string[];
    instruments?: string[];
    sensorTypes?: SensorType[];
    orbitDirection?: OrbitDirection;
    providers?: string[];
    isPaid?: boolean;
    gsdMin?: number;
    gsdMax?: number;
    cloudMax?: number;
    sorts?: ImageSort[];
    page?: number;
    size?: number;
};
