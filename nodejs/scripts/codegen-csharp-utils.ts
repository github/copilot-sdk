/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Shared C# code-generation utilities used by both session-event and RPC generators.
 */

import type { JSONSchema7 } from "json-schema";

/**
 * Convert a dot/underscore-separated type string to PascalCase class name.
 * e.g. "session.start" → "SessionStart", "models.list" → "ModelsList"
 */
export function typeToClassName(typeName: string): string {
    return typeName
        .split(/[._]/)
        .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
        .join("");
}

/**
 * Convert a property name to PascalCase for C#.
 */
export function toPascalCase(name: string): string {
    if (name.includes("_")) {
        return name
            .split("_")
            .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
            .join("");
    }
    return name.charAt(0).toUpperCase() + name.slice(1);
}

/**
 * Convert a string value to a valid C# enum member name.
 */
export function toPascalCaseEnumMember(value: string): string {
    return value
        .split(/[-_.]/)
        .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
        .join("");
}

/**
 * Map a leaf JSON Schema type to a C# type string.
 */
export function schemaTypeToCSharp(
    schema: JSONSchema7,
    required: boolean,
    knownTypes: Map<string, string>
): string {
    if (schema.anyOf) {
        const nonNull = schema.anyOf.filter((s) => typeof s === "object" && s.type !== "null");
        if (nonNull.length === 1 && typeof nonNull[0] === "object") {
            return schemaTypeToCSharp(nonNull[0] as JSONSchema7, false, knownTypes) + "?";
        }
    }

    if (schema.$ref) {
        const refName = schema.$ref.split("/").pop()!;
        return knownTypes.get(refName) || refName;
    }

    const type = schema.type;
    const format = schema.format;

    if (type === "string") {
        if (format === "uuid") return required ? "Guid" : "Guid?";
        if (format === "date-time") return required ? "DateTimeOffset" : "DateTimeOffset?";
        return required ? "string" : "string?";
    }
    if (type === "number" || type === "integer") {
        return required ? "double" : "double?";
    }
    if (type === "boolean") {
        return required ? "bool" : "bool?";
    }
    if (type === "array") {
        const items = schema.items as JSONSchema7 | undefined;
        const itemType = items ? schemaTypeToCSharp(items, true, knownTypes) : "object";
        return required ? `${itemType}[]` : `${itemType}[]?`;
    }
    if (type === "object") {
        if (schema.additionalProperties) {
            const valueSchema = schema.additionalProperties;
            if (typeof valueSchema === "object") {
                const valueType = schemaTypeToCSharp(valueSchema as JSONSchema7, true, knownTypes);
                return required
                    ? `Dictionary<string, ${valueType}>`
                    : `Dictionary<string, ${valueType}>?`;
            }
            return required ? "Dictionary<string, object>" : "Dictionary<string, object>?";
        }
        return required ? "object" : "object?";
    }

    return required ? "object" : "object?";
}

/**
 * C# copyright header for generated files.
 */
export const CSHARP_COPYRIGHT = `/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/`;

