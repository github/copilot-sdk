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
    lines.push(`[JsonConverter(typeof(JsonStringEnumConverter<${enumName}>))]`);
    lines.push(`public enum ${enumName}`);
    lines.push(`{`);
    for (const value of values) {
        const memberName = toPascalCaseEnumMember(value);
        lines.push(`    [JsonStringEnumMemberName("${value}")]`);
        lines.push(`    ${memberName},`);
    }
    lines.push(`}`);
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
    knownTypes: Map<string, string>,
    nestedClasses: Map<string, string>,
    enumOutput: string[]
): string {
    const lines: string[] = [];
    const dataSchema = variant.dataSchema;

    if (!dataSchema?.properties) {
        lines.push(`public partial class ${variant.dataClassName} { }`);
        return lines.join("\n");
    }

    const required = new Set(dataSchema.required || []);

    lines.push(`public partial class ${variant.dataClassName}`);
    lines.push(`{`);

    for (const [propName, propSchema] of Object.entries(dataSchema.properties)) {
        if (typeof propSchema !== "object") continue;

        const isRequired = required.has(propName);
        const csharpName = toPascalCase(propName);
        const csharpType = resolvePropertyType(
            propSchema as JSONSchema7,
            variant.dataClassName,
            csharpName,
            isRequired,
            knownTypes,
            nestedClasses,
            enumOutput
        );

        const isNullableType = csharpType.endsWith("?");
        if (!isRequired) {
            lines.push(
                `    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]`
            );
        }
        lines.push(`    [JsonPropertyName("${propName}")]`);

        const requiredModifier = isRequired && !isNullableType ? "required " : "";
        lines.push(`    public ${requiredModifier}${csharpType} ${csharpName} { get; set; }`);
        lines.push("");
    }

    // Remove trailing empty line
    if (lines[lines.length - 1] === "") {
        lines.pop();
    }

    lines.push(`}`);
    return lines.join("\n");
}

/**
 * Generate a nested class for complex object properties.
 * This function recursively handles nested objects, arrays of objects, and anyOf unions.
 */
function generateNestedClass(
    className: string,
    schema: JSONSchema7,
    knownTypes: Map<string, string>,
    nestedClasses: Map<string, string>,
    enumOutput: string[]
): string {
    const lines: string[] = [];
    const required = new Set(schema.required || []);

    lines.push(`public partial class ${className}`);
    lines.push(`{`);

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
                knownTypes,
                nestedClasses,
                enumOutput
            );

            if (!isRequired) {
                lines.push(
                    `    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]`
                );
            }
            lines.push(`    [JsonPropertyName("${propName}")]`);

            const isNullableType = csharpType.endsWith("?");
            const requiredModifier = isRequired && !isNullableType ? "required " : "";
            lines.push(`    public ${requiredModifier}${csharpType} ${csharpName} { get; set; }`);
            lines.push("");
        }
    }

    // Remove trailing empty line
    if (lines[lines.length - 1] === "") {
        lines.pop();
    }

    lines.push(`}`);
    return lines.join("\n");
}

/**
 * Find a discriminator property shared by all variants in an anyOf.
 * Returns the property name and the mapping of const values to variant schemas.
 */
function findDiscriminator(variants: JSONSchema7[]): { property: string; mapping: Map<string, JSONSchema7> } | null {
    if (variants.length === 0) return null;

    // Look for a property with a const value in all variants
    const firstVariant = variants[0];
    if (!firstVariant.properties) return null;

    for (const [propName, propSchema] of Object.entries(firstVariant.properties)) {
        if (typeof propSchema !== "object") continue;
        const schema = propSchema as JSONSchema7;
        if (schema.const === undefined) continue;

        // Check if all variants have this property with a const value
        const mapping = new Map<string, JSONSchema7>();
        let isValidDiscriminator = true;

        for (const variant of variants) {
            if (!variant.properties) {
                isValidDiscriminator = false;
                break;
            }
            const variantProp = variant.properties[propName];
            if (typeof variantProp !== "object") {
                isValidDiscriminator = false;
                break;
            }
            const variantSchema = variantProp as JSONSchema7;
            if (variantSchema.const === undefined) {
                isValidDiscriminator = false;
                break;
            }
            mapping.set(String(variantSchema.const), variant);
        }

        if (isValidDiscriminator && mapping.size === variants.length) {
            return { property: propName, mapping };
        }
    }

    return null;
}

/**
 * Generate a polymorphic base class and derived classes for a discriminated union.
 */
function generatePolymorphicClasses(
    baseClassName: string,
    discriminatorProperty: string,
    variants: JSONSchema7[],
    knownTypes: Map<string, string>,
    nestedClasses: Map<string, string>,
    enumOutput: string[]
): string {
    const lines: string[] = [];
    const discriminatorInfo = findDiscriminator(variants)!;

    // Generate base class with JsonPolymorphic attribute
    lines.push(`[JsonPolymorphic(`);
    lines.push(`    TypeDiscriminatorPropertyName = "${discriminatorProperty}",`);
    lines.push(`    UnknownDerivedTypeHandling = JsonUnknownDerivedTypeHandling.FallBackToBaseType)]`);

    // Add JsonDerivedType attributes for each variant
    for (const [constValue] of discriminatorInfo.mapping) {
        const derivedClassName = `${baseClassName}${toPascalCase(constValue)}`;
        lines.push(`[JsonDerivedType(typeof(${derivedClassName}), "${constValue}")]`);
    }

    lines.push(`public partial class ${baseClassName}`);
    lines.push(`{`);
    lines.push(`    [JsonPropertyName("${discriminatorProperty}")]`);
    lines.push(`    public virtual string ${toPascalCase(discriminatorProperty)} { get; set; } = string.Empty;`);
    lines.push(`}`);
    lines.push("");

    // Generate derived classes
    for (const [constValue, variant] of discriminatorInfo.mapping) {
        const derivedClassName = `${baseClassName}${toPascalCase(constValue)}`;
        const derivedCode = generateDerivedClass(
            derivedClassName,
            baseClassName,
            discriminatorProperty,
            constValue,
            variant,
            knownTypes,
            nestedClasses,
            enumOutput
        );
        nestedClasses.set(derivedClassName, derivedCode);
    }

    return lines.join("\n");
}

/**
 * Generate a derived class for a discriminated union variant.
 */
function generateDerivedClass(
    className: string,
    baseClassName: string,
    discriminatorProperty: string,
    discriminatorValue: string,
    schema: JSONSchema7,
    knownTypes: Map<string, string>,
    nestedClasses: Map<string, string>,
    enumOutput: string[]
): string {
    const lines: string[] = [];
    const required = new Set(schema.required || []);

    lines.push(`public partial class ${className} : ${baseClassName}`);
    lines.push(`{`);

    // Override the discriminator property
    lines.push(`    [JsonIgnore]`);
    lines.push(`    public override string ${toPascalCase(discriminatorProperty)} => "${discriminatorValue}";`);
    lines.push("");

    if (schema.properties) {
        for (const [propName, propSchema] of Object.entries(schema.properties)) {
            if (typeof propSchema !== "object") continue;
            // Skip the discriminator property (already in base class)
            if (propName === discriminatorProperty) continue;

            const isRequired = required.has(propName);
            const csharpName = toPascalCase(propName);
            const csharpType = resolvePropertyType(
                propSchema as JSONSchema7,
                className,
                csharpName,
                isRequired,
                knownTypes,
                nestedClasses,
                enumOutput
            );

            if (!isRequired) {
                lines.push(`    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]`);
            }
            lines.push(`    [JsonPropertyName("${propName}")]`);

            const isNullableType = csharpType.endsWith("?");
            const requiredModifier = isRequired && !isNullableType ? "required " : "";
            lines.push(`    public ${requiredModifier}${csharpType} ${csharpName} { get; set; }`);
            lines.push("");
        }
    }

    // Remove trailing empty line
    if (lines[lines.length - 1] === "") {
        lines.pop();
    }

    lines.push(`}`);
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

        // Array of discriminated union (anyOf with shared discriminator)
        if (items.anyOf && Array.isArray(items.anyOf)) {
            const variants = items.anyOf.filter((v): v is JSONSchema7 => typeof v === "object");
            const discriminatorInfo = findDiscriminator(variants);
            
            if (discriminatorInfo) {
                const baseClassName = `${parentClassName}${propName}Item`;
                const polymorphicCode = generatePolymorphicClasses(
                    baseClassName,
                    discriminatorInfo.property,
                    variants,
                    knownTypes,
                    nestedClasses,
                    enumOutput
                );
                nestedClasses.set(baseClassName, polymorphicCode);
                return isRequired ? `${baseClassName}[]` : `${baseClassName}[]?`;
            }
        }

        // Array of objects with properties
        if (items.type === "object" && items.properties) {
            const itemClassName = `${parentClassName}${propName}Item`;
            const nestedCode = generateNestedClass(
                itemClassName,
                items,
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

using System.Text.Json;
using System.Text.Json.Serialization;

namespace GitHub.Copilot.SDK;
`);

    // Generate base class with JsonPolymorphic attributes
    lines.push(`/// <summary>`);
    lines.push(
        `/// Base class for all session events with polymorphic JSON serialization.`
    );
    lines.push(`/// </summary>`);
    lines.push(`[JsonPolymorphic(`);
    lines.push(`    TypeDiscriminatorPropertyName = "type",`);
    lines.push(
        `    UnknownDerivedTypeHandling = JsonUnknownDerivedTypeHandling.FailSerialization)]`
    );

    // Generate JsonDerivedType attributes for each variant (alphabetized)
    for (const variant of [...variants].sort((a, b) => a.typeName.localeCompare(b.typeName))) {
        lines.push(
            `[JsonDerivedType(typeof(${variant.className}), "${variant.typeName}")]`
        );
    }

    lines.push(`public abstract partial class SessionEvent`);
    lines.push(`{`);
    lines.push(`    [JsonPropertyName("id")]`);
    lines.push(`    public Guid Id { get; set; }`);
    lines.push("");
    lines.push(`    [JsonPropertyName("timestamp")]`);
    lines.push(`    public DateTimeOffset Timestamp { get; set; }`);
    lines.push("");
    lines.push(`    [JsonPropertyName("parentId")]`);
    lines.push(`    public Guid? ParentId { get; set; }`);
    lines.push("");
    lines.push(`    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]`);
    lines.push(`    [JsonPropertyName("ephemeral")]`);
    lines.push(`    public bool? Ephemeral { get; set; }`);
    lines.push("");
    lines.push(`    /// <summary>`);
    lines.push(`    /// The event type discriminator.`);
    lines.push(`    /// </summary>`);
    lines.push(`    [JsonIgnore]`);
    lines.push(`    public abstract string Type { get; }`);
    lines.push("");
    lines.push(`    public static SessionEvent FromJson(string json) =>`);
    lines.push(
        `        JsonSerializer.Deserialize(json, SessionEventsJsonContext.Default.SessionEvent)!;`
    );
    lines.push("");
    lines.push(`    public string ToJson() =>`);
    lines.push(
        `        JsonSerializer.Serialize(this, SessionEventsJsonContext.Default.SessionEvent);`
    );
    lines.push(`}`);
    lines.push("");

    // Generate each event class
    for (const variant of variants) {
        lines.push(`/// <summary>`);
        lines.push(`/// Event: ${variant.typeName}`);
        lines.push(`/// </summary>`);
        lines.push(`public partial class ${variant.className} : SessionEvent`);
        lines.push(`{`);
        lines.push(`    [JsonIgnore]`);
        lines.push(`    public override string Type => "${variant.typeName}";`);
        lines.push("");
        lines.push(`    [JsonPropertyName("data")]`);
        lines.push(`    public required ${variant.dataClassName} Data { get; set; }`);
        lines.push(`}`);
        lines.push("");
    }

    // Generate data classes
    for (const variant of variants) {
        const dataClass = generateDataClass(variant, knownTypes, nestedClasses, enumOutput);
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

    // Collect all serializable types (sorted alphabetically)
    const serializableTypes: string[] = [];

    // Add SessionEvent base class
    serializableTypes.push("SessionEvent");

    // Add all event classes and their data classes
    for (const variant of variants) {
        serializableTypes.push(variant.className);
        serializableTypes.push(variant.dataClassName);
    }

    // Add all nested classes
    for (const [className] of nestedClasses) {
        serializableTypes.push(className);
    }

    // Sort alphabetically
    serializableTypes.sort((a, b) => a.localeCompare(b));

    // Generate JsonSerializerContext with JsonSerializable attributes
    lines.push(`[JsonSourceGenerationOptions(`);
    lines.push(`    JsonSerializerDefaults.Web,`);
    lines.push(`    AllowOutOfOrderMetadataProperties = true,`);
    lines.push(`    NumberHandling = JsonNumberHandling.AllowReadingFromString,`);
    lines.push(`    DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull)]`);
    for (const typeName of serializableTypes) {
        lines.push(`[JsonSerializable(typeof(${typeName}))]`);
    }
    lines.push(`internal partial class SessionEventsJsonContext : JsonSerializerContext;`);

    return lines.join("\n");
}
