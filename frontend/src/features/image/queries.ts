import { useQuery } from "@tanstack/react-query";

import { fetchFilterOptions } from "@/features/image/api";

const FILTER_OPTIONS_STALE_TIME = 5 * 60 * 1000;

export function useFilterOptions() {
    return useQuery({
        queryKey: ["image", "filter-options"],
        queryFn: fetchFilterOptions,
        staleTime: FILTER_OPTIONS_STALE_TIME,
    });
}
