import {
    type inferParserType,
    parseAsArrayOf,
    parseAsBoolean,
    parseAsFloat,
    parseAsString,
    parseAsStringLiteral,
    useQueryStates,
} from "nuqs";

import {
    imageSortSchema,
    orbitDirectionSchema,
    sensorTypeSchema,
} from "@/features/image/schemas";
import type { ImageSearchParams } from "@/features/image/types";

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
    };
}

export function useAppliedConditions() {
    const [values, setValues] = useQueryStates(imageSearchParsers, {
        history: "push",
    });

    function setConditions(conditions: ImageSearchParams) {
        setValues(toSearchValues(conditions));
    }

    return [fromSearchValues(values), setConditions] as const;
}
