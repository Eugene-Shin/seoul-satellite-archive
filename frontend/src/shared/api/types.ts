export type ApiResponse<T> = {
    success: boolean;
    code: string;
    message: string;
    data: T;
    errorDetail?: string[];
};

export type SliceResponse<T> = {
    content: T[];
    hasNext: boolean;
};
