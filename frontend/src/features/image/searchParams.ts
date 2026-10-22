import {
    createParser,
    type inferParserType,
    parseAsArrayOf,
    parseAsBoolean,
    parseAsFloat,
    parseAsString,
    parseAsStringLiteral,
    useQueryStates,
} from "nuqs";
import { useState } from "react";

import { toBbox } from "@/features/image/bbox";
import {
    imageSortSchema,
    orbitDirectionSchema,
    sensorTypeSchema,
} from "@/features/image/schemas";
import type { ImageSearchParams } from "@/features/image/types";

const parseAsBbox = createParser({
    parse: (value) => toBbox(value.split(",").map(parseFloat)),
    serialize: (bbox) => bbox.join(","),
});

const imageSearchParsers = {
    keyword: parseAsString,
    acquiredFrom: parseAsString,
    acquiredTo: parseAsString,
    satellites: parseAsArrayOf(parseAsString),
    instruments: parseAsArrayOf(parseAsString),
    sensorTypes: parseAsArrayOf(parseAsStringLiteral(sensorTypeSchema.options)),
    orbitDirection: parseAsStringLiteral(orbitDirectionSchema.options),
    providers: parseAsArrayOf(parseAsString),
    isPaid: parseAsBoolean,
    gsdMin: parseAsFloat,
    gsdMax: parseAsFloat,
    cloudMax: parseAsFloat,
    sorts: parseAsArrayOf(parseAsStringLiteral(imageSortSchema.options)),
    bbox: parseAsBbox,
};

type ImageSearchValues = inferParserType<typeof imageSearchParsers>;

function fromSearchValues(values: ImageSearchValues): ImageSearchParams {
    return {
        keyword: values.keyword ?? undefined,
        acquiredFrom: values.acquiredFrom ?? undefined,
        acquiredTo: values.acquiredTo ?? undefined,
        satellites: values.satellites ?? undefined,
        instruments: values.instruments ?? undefined,
        sensorTypes: values.sensorTypes ?? undefined,
        orbitDirection: values.orbitDirection ?? undefined,
        providers: values.providers ?? undefined,
        isPaid: values.isPaid ?? undefined,
        gsdMin: values.gsdMin ?? undefined,
        gsdMax: values.gsdMax ?? undefined,
        cloudMax: values.cloudMax ?? undefined,
        sorts: values.sorts ?? undefined,
        bbox: values.bbox ?? undefined,
    };
}

function toSearchValues(conditions: ImageSearchParams): ImageSearchValues {
    return {
        keyword: conditions.keyword ?? null,
        acquiredFrom: conditions.acquiredFrom ?? null,
        acquiredTo: conditions.acquiredTo ?? null,
        satellites: conditions.satellites ?? null,
        instruments: conditions.instruments ?? null,
        sensorTypes: conditions.sensorTypes ?? null,
        orbitDirection: conditions.orbitDirection ?? null,
        providers: conditions.providers ?? null,
        isPaid: conditions.isPaid ?? null,
        gsdMin: conditions.gsdMin ?? null,
        gsdMax: conditions.gsdMax ?? null,
        cloudMax: conditions.cloudMax ?? null,
        sorts: conditions.sorts ?? null,
        bbox: conditions.bbox ?? null,
    };
}

function useAppliedConditions() {
    const [values, setValues] = useQueryStates(imageSearchParsers, {
        history: "push",
    });

    function setConditions(conditions: ImageSearchParams) {
        setValues(toSearchValues(conditions));
    }

    return [fromSearchValues(values), setConditions] as const;
}

// 같은 조건으로 다시 검색해도 새 요청이 되도록 검색한 조건과 검색 번호를 함께 바꾼다
export function useCurrentSearch() {
    const [appliedConditions, setAppliedConditions] = useAppliedConditions();
    const [searchId, setSearchId] = useState(0);
    // searchId는 우리 코드로 검색할 때, appliedConditions는 URL이 바뀔 때 달라져서 둘을 함께 쓴다
    const searchKey = JSON.stringify([searchId, appliedConditions]);

    function searchWith(conditions: ImageSearchParams) {
        setAppliedConditions(conditions);
        setSearchId((prev) => prev + 1);
    }

    return {
        conditions: appliedConditions,
        id: searchId,
        key: searchKey,
        searchWith,
    };
}
