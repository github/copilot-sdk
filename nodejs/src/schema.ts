/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { ResponseSchema, ZodSchema } from "./types.js";

export function isZodSchema(value: unknown): value is ZodSchema {
    return (
        typeof value === "object" &&
        value !== null &&
        "toJSONSchema" in value &&
        typeof value.toJSONSchema === "function"
    );
}

export function toJsonSchema(
    schema: ZodSchema | Record<string, unknown> | undefined
): Record<string, unknown> | undefined {
    return isZodSchema(schema) ? schema.toJSONSchema() : schema;
}

export function isResponseSchema(value: unknown): value is ResponseSchema {
    return isZodSchema(value) && "parse" in value && typeof value.parse === "function";
}
