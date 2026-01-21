/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Custom C# code generator for session event types with proper polymorphic serialization.
 *
 * This generator produces:
 * - A base SessionEvent class with [JsonPolymorphic] and [JsonDerivedType] attributes
 * - Separate event classes (SessionStartEvent, AssistantMessageEvent, etc.) with strongly-typed Data
 * - Separate Data classes for each event type with only the relevant properties
 *
 * This approach provides type-safe access to event data instead of a single Data class with 60+ nullable properties.
 */

import type { JSONSchema7 } from "json-schema";

interface EventVariant {
    typeName: string; // e.g., "session.start"
    className: string; // e.g., "SessionStartEvent"
    dataClassName: string; // e.g., "SessionStartData"
    dataSchema: JSONSchema7;
    ephemeralConst?: boolean; // if ephemeral has a const value
}

/**
 * Convert a type string like "session.start" to PascalCase class name like "SessionStart"
 */
function typeToClassName(typeName: string): string {
    return typeName
        .split(/[._]/)
        .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
        .join("");
}

/**
 * Convert a property name to PascalCase for C#
 */
function toPascalCase(name: string): string {
    // Handle snake_case
    if (name.includes("_")) {
        return name
            .split("_")
            .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
            .join("");
    }
    // Handle camelCase
    return name.charAt(0).toUpperCase() + name.slice(1);
}

/**
 * Map JSON Schema type to C# type
 */
function schemaTypeToCSharp(
    schema: JSONSchema7,
    required: boolean,
    knownTypes: Map<string, string>,
    parentClassName?: string,
    propName?: string,
    enumOutput?: string[]
): string {
    if (schema.anyOf) {
        // Handle nullable types (anyOf with null)
        const nonNull = schema.anyOf.filter((s) => typeof s === "object" && s.type !== "null");
        if (nonNull.length === 1 && typeof nonNull[0] === "object") {
            return (
                schemaTypeToCSharp(
                    nonNull[0] as JSONSchema7,
                    false,
                    knownTypes,
                    parentClassName,
                    propName,
                    enumOutput
                ) + "?"
            );
        }
    }

    if (schema.enum && parentClassName && propName && enumOutput) {
        // Generate C# enum
        const enumName = getOrCreateEnum(
            parentClassName,
            propName,
            schema.enum as string[],
            enumOutput
        );
        return required ? enumName : `${enumName}?`;
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
                return required ? `Dictionary<string, ${valueType}>` : `Dictionary<string, ${valueType}>?`;
            }
            return required ? "Dictionary<string, object>" : "Dictionary<string, object>?";
        }
        return required ? "object" : "object?";
    }

    return required ? "object" : "object?";
}

/**
 * Event types to exclude from generation (internal/legacy types)
 */
const EXCLUDED_EVENT_TYPES = new Set(["session.import_legacy"]);

/**
 * Track enums that have been generated to avoid duplicates
 */
const generatedEnums = new Map<string, { enumName: string; values: string[] }>();

/**
 * Generate a C# enum name from the context
 */
function generateEnumName(parentClassName: string, propName: string): string {
    return `${parentClassName}${propName}`;
}

/**
 * Get or create an enum for a given set of values.
 * Returns the enum name and whether it's newly generated.
 */
function getOrCreateEnum(
    parentClassName: string,
    propName: string,
    values: string[],
    enumOutput: string[]
): string {
    // Create a key based on the sorted values to detect duplicates
    const valuesKey = [...values].sort().join("|");

    // Check if we already have an enum with these exact values
    for (const [, existing] of generatedEnums) {
        const existingKey = [...existing.values].sort().join("|");
        if (existingKey === valuesKey) {
            return existing.enumName;
        }
    }

    const enumName = generateEnumName(parentClassName, propName);
    generatedEnums.set(enumName, { enumName, values });

    // Generate the enum code with JsonConverter and JsonStringEnumMemberName attributes
    const lines: string[] = [];
    lines.push(`    [JsonConverter(typeof(JsonStringEnumConverter<${enumName}>))]`);
    lines.push(`    public enum ${enumName}`);
    lines.push(`    {`);
    for (const value of values) {
        const memberName = toPascalCaseEnumMember(value);
        lines.push(`        [JsonStringEnumMemberName("${value}")]`);
        lines.push(`        ${memberName},`);
    }
    lines.push(`    }`);
    lines.push("");

    enumOutput.push(lines.join("\n"));
    return enumName;
}

/**
 * Convert a string value to a valid C# enum member name
 */
function toPascalCaseEnumMember(value: string): string {
    // Handle special characters and convert to PascalCase
    return value
        .split(/[-_.]/)
        .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
        .join("");
}

/**
 * Extract event variants from the schema's anyOf
 */
function extractEventVariants(schema: JSONSchema7): EventVariant[] {
    const sessionEvent = schema.definitions?.SessionEvent as JSONSchema7;
    if (!sessionEvent?.anyOf) {
        throw new Error("Schema must have SessionEvent definition with anyOf");
    }

    return sessionEvent.anyOf
        .map((variant) => {
            if (typeof variant !== "object" || !variant.properties) {
                throw new Error("Invalid variant in anyOf");
            }

            const typeSchema = variant.properties.type as JSONSchema7;
            const typeName = typeSchema?.const as string;
            if (!typeName) {
                throw new Error("Variant must have type.const");
            }

            const baseName = typeToClassName(typeName);
            const ephemeralSchema = variant.properties.ephemeral as JSONSchema7 | undefined;

            return {
                typeName,
                className: `${baseName}Event`,
                dataClassName: `${baseName}Data`,
                dataSchema: variant.properties.data as JSONSchema7,
                ephemeralConst: ephemeralSchema?.const as boolean | undefined,
            };
        })
        .filter((variant) => !EXCLUDED_EVENT_TYPES.has(variant.typeName));
}

/**
 * Generate C# code for a Data class
 */
function generateDataClass(
    variant: EventVariant,
    indent: string,
    knownTypes: Map<string, string>,
    nestedClasses: Map<string, string>,
    enumOutput: string[]
): string {
    const lines: string[] = [];
    const dataSchema = variant.dataSchema;

    if (!dataSchema?.properties) {
        lines.push(`${indent}public partial class ${variant.dataClassName} { }`);
        return lines.join("\n");
    }

    const required = new Set(dataSchema.required || []);

    lines.push(`${indent}public partial class ${variant.dataClassName}`);
    lines.push(`${indent}{`);

    for (const [propName, propSchema] of Object.entries(dataSchema.properties)) {
        if (typeof propSchema !== "object") continue;

        const isRequired = required.has(propName);
        const csharpName = toPascalCase(propName);
        const csharpType = resolvePropertyType(
            propSchema as JSONSchema7,
            variant.dataClassName,
            csharpName,
            isRequired,
            indent,
            knownTypes,
            nestedClasses,
            enumOutput
        );

        const isNullableType = csharpType.endsWith("?");
        if (!isRequired) {
            lines.push(
                `${indent}    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]`
            );
        }
        lines.push(`${indent}    [JsonPropertyName("${propName}")]`);

        const requiredModifier = isRequired && !isNullableType ? "required " : "";
        lines.push(`${indent}    public ${requiredModifier}${csharpType} ${csharpName} { get; set; }`);
        lines.push("");
    }

    // Remove trailing empty line
    if (lines[lines.length - 1] === "") {
        lines.pop();
    }

    lines.push(`${indent}}`);
    return lines.join("\n");
}

/**
 * Generate a nested class for complex object properties.
 * This function recursively handles nested objects, arrays of objects, and anyOf unions.
 */
function generateNestedClass(
    className: string,
    schema: JSONSchema7,
    indent: string,
    knownTypes: Map<string, string>,
    nestedClasses: Map<string, string>,
    enumOutput: string[]
): string {
    const lines: string[] = [];
    const required = new Set(schema.required || []);

    lines.push(`${indent}public partial class ${className}`);
    lines.push(`${indent}{`);

    if (schema.properties) {
        for (const [propName, propSchema] of Object.entries(schema.properties)) {
            if (typeof propSchema !== "object") continue;

            const isRequired = required.has(propName);
            const csharpName = toPascalCase(propName);
            let csharpType = resolvePropertyType(
                propSchema as JSONSchema7,
                className,
                csharpName,
                isRequired,
                indent,
                knownTypes,
                nestedClasses,
                enumOutput
            );

            if (!isRequired) {
                lines.push(
                    `${indent}    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]`
                );
            }
            lines.push(`${indent}    [JsonPropertyName("${propName}")]`);

            const isNullableType = csharpType.endsWith("?");
            const requiredModifier = isRequired && !isNullableType ? "required " : "";
            lines.push(`${indent}    public ${requiredModifier}${csharpType} ${csharpName} { get; set; }`);
            lines.push("");
        }
    }

    // Remove trailing empty line
    if (lines[lines.length - 1] === "") {
        lines.pop();
    }

    lines.push(`${indent}}`);
    return lines.join("\n");
}

/**
 * Resolve the C# type for a property, generating nested classes as needed.
 * Handles objects and arrays of objects.
 */
function resolvePropertyType(
    propSchema: JSONSchema7,
    parentClassName: string,
    propName: string,
    isRequired: boolean,
    indent: string,
    knownTypes: Map<string, string>,
    nestedClasses: Map<string, string>,
    enumOutput: string[]
): string {
    // Handle anyOf - simplify to nullable of the non-null type or object
    if (propSchema.anyOf) {
        const hasNull = propSchema.anyOf.some(
            (s) => typeof s === "object" && (s as JSONSchema7).type === "null"
        );
        const nonNullTypes = propSchema.anyOf.filter(
            (s) => typeof s === "object" && (s as JSONSchema7).type !== "null"
        );
        if (nonNullTypes.length === 1) {
            // Simple nullable - recurse with the inner type, marking as not required if null is an option
            return resolvePropertyType(
                nonNullTypes[0] as JSONSchema7,
                parentClassName,
                propName,
                isRequired && !hasNull,
                indent,
                knownTypes,
                nestedClasses,
                enumOutput
            );
        }
        // Complex union - use object, nullable if null is in the union or property is not required
        return (hasNull || !isRequired) ? "object?" : "object";
    }

    // Handle enum types
    if (propSchema.enum && Array.isArray(propSchema.enum)) {
        const enumName = getOrCreateEnum(
            parentClassName,
            propName,
            propSchema.enum as string[],
            enumOutput
        );
        return isRequired ? enumName : `${enumName}?`;
    }

    // Handle nested object types
    if (propSchema.type === "object" && propSchema.properties) {
        const nestedClassName = `${parentClassName}${propName}`;
        const nestedCode = generateNestedClass(
            nestedClassName,
            propSchema,
            indent,
            knownTypes,
            nestedClasses,
            enumOutput
        );
        nestedClasses.set(nestedClassName, nestedCode);
        return isRequired ? nestedClassName : `${nestedClassName}?`;
    }

    // Handle array of objects
    if (propSchema.type === "array" && propSchema.items) {
        const items = propSchema.items as JSONSchema7;

        // Array of objects with properties
        if (items.type === "object" && items.properties) {
            const itemClassName = `${parentClassName}${propName}Item`;
            const nestedCode = generateNestedClass(
                itemClassName,
                items,
                indent,
                knownTypes,
                nestedClasses,
                enumOutput
            );
            nestedClasses.set(itemClassName, nestedCode);
            return isRequired ? `${itemClassName}[]` : `${itemClassName}[]?`;
        }

        // Array of enums
        if (items.enum && Array.isArray(items.enum)) {
            const enumName = getOrCreateEnum(
                parentClassName,
                `${propName}Item`,
                items.enum as string[],
                enumOutput
            );
            return isRequired ? `${enumName}[]` : `${enumName}[]?`;
        }

        // Simple array type
        const itemType = schemaTypeToCSharp(
            items,
            true,
            knownTypes,
            parentClassName,
            propName,
            enumOutput
        );
        return isRequired ? `${itemType}[]` : `${itemType}[]?`;
    }

    // Default: use basic type mapping
    return schemaTypeToCSharp(
        propSchema,
        isRequired,
        knownTypes,
        parentClassName,
        propName,
        enumOutput
    );
}

/**
 * Generate the complete C# file
 */
export function generateCSharpSessionTypes(schema: JSONSchema7, generatedAt: string): string {
    // Clear the generated enums map from any previous run
    generatedEnums.clear();

    const variants = extractEventVariants(schema);
    const knownTypes = new Map<string, string>();
    const nestedClasses = new Map<string, string>();
    const enumOutput: string[] = [];
    const indent = "    ";

    const lines: string[] = [];

    // File header
    lines.push(`/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
//
// Generated from: @github/copilot/session-events.schema.json
// Generated by: scripts/generate-session-types.ts
// Generated at: ${generatedAt}
//
// To update these types:
// 1. Update the schema in copilot-agent-runtime
// 2. Run: npm run generate:session-types

// <auto-generated />
#nullable enable

namespace GitHub.Copilot.SDK
{
    using System;
    using System.Collections.Generic;
    using System.Text.Json;
    using System.Text.Json.Serialization;
`);

    // Generate base class with JsonPolymorphic attributes
    lines.push(`${indent}/// <summary>`);
    lines.push(
        `${indent}/// Base class for all session events with polymorphic JSON serialization.`
    );
    lines.push(`${indent}/// </summary>`);
    lines.push(`${indent}[JsonPolymorphic(`);
    lines.push(`${indent}    TypeDiscriminatorPropertyName = "type", `);
    lines.push(
        `${indent}    UnknownDerivedTypeHandling = JsonUnknownDerivedTypeHandling.FailSerialization)]`
    );

    // Generate JsonDerivedType attributes for each variant (alphabetized)
    for (const variant of [...variants].sort((a, b) => a.typeName.localeCompare(b.typeName))) {
        lines.push(
            `${indent}[JsonDerivedType(typeof(${variant.className}), "${variant.typeName}")]`
        );
    }

    lines.push(`${indent}public abstract partial class SessionEvent`);
    lines.push(`${indent}{`);
    lines.push(`${indent}    [JsonPropertyName("id")]`);
    lines.push(`${indent}    public Guid Id { get; set; }`);
    lines.push("");
    lines.push(`${indent}    [JsonPropertyName("timestamp")]`);
    lines.push(`${indent}    public DateTimeOffset Timestamp { get; set; }`);
    lines.push("");
    lines.push(`${indent}    [JsonPropertyName("parentId")]`);
    lines.push(`${indent}    public Guid? ParentId { get; set; }`);
    lines.push("");
    lines.push(`${indent}    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]`);
    lines.push(`${indent}    [JsonPropertyName("ephemeral")]`);
    lines.push(`${indent}    public bool? Ephemeral { get; set; }`);
    lines.push("");
    lines.push(`${indent}    /// <summary>`);
    lines.push(`${indent}    /// The event type discriminator.`);
    lines.push(`${indent}    /// </summary>`);
    lines.push(`${indent}    [JsonIgnore]`);
    lines.push(`${indent}    public abstract string Type { get; }`);
    lines.push("");
    lines.push(`${indent}    public static SessionEvent FromJson(string json) =>`);
    lines.push(
        `${indent}        JsonSerializer.Deserialize<SessionEvent>(json, SerializerOptions.Default)!;`
    );
    lines.push("");
    lines.push(`${indent}    public string ToJson() =>`);
    lines.push(
        `${indent}        JsonSerializer.Serialize(this, GetType(), SerializerOptions.Default);`
    );
    lines.push(`${indent}}`);
    lines.push("");

    // Generate each event class
    for (const variant of variants) {
        lines.push(`${indent}/// <summary>`);
        lines.push(`${indent}/// Event: ${variant.typeName}`);
        lines.push(`${indent}/// </summary>`);
        lines.push(`${indent}public partial class ${variant.className} : SessionEvent`);
        lines.push(`${indent}{`);
        lines.push(`${indent}    [JsonIgnore]`);
        lines.push(`${indent}    public override string Type => "${variant.typeName}";`);
        lines.push("");
        lines.push(`${indent}    [JsonPropertyName("data")]`);
        lines.push(`${indent}    public required ${variant.dataClassName} Data { get; set; }`);
        lines.push(`${indent}}`);
        lines.push("");
    }

    // Generate data classes
    for (const variant of variants) {
        const dataClass = generateDataClass(variant, indent, knownTypes, nestedClasses, enumOutput);
        lines.push(dataClass);
        lines.push("");
    }

    // Generate nested classes
    for (const [, nestedCode] of nestedClasses) {
        lines.push(nestedCode);
        lines.push("");
    }

    // Generate enums
    for (const enumCode of enumOutput) {
        lines.push(enumCode);
    }

    // Generate serializer options
    lines.push(`${indent}internal static class SerializerOptions`);
    lines.push(`${indent}{`);
    lines.push(`${indent}    /// <summary>`);
    lines.push(`${indent}    /// Default options for polymorphic deserialization.`);
    lines.push(`${indent}    /// </summary>`);
    lines.push(`${indent}    public static readonly JsonSerializerOptions Default = new()`);
    lines.push(`${indent}    {`);
    lines.push(`${indent}        AllowOutOfOrderMetadataProperties = true,`);
    lines.push(`${indent}        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,`);
    lines.push(`${indent}        DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull`);
    lines.push(`${indent}    };`);
    lines.push(`${indent}}`);

    // Close namespace
    lines.push(`}`);

    return lines.join("\n");
}
