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
