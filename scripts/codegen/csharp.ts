/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * C# code generator for session-events and RPC types.
 */

import { execFile } from "child_process";
import fs from "fs/promises";
import path from "path";
import { promisify } from "util";
import type { JSONSchema7 } from "json-schema";
import {
    cloneSchemaForCodegen,
    fixNullableRequiredRefsInApiSchema,
    getApiSchemaPath,
    getRpcSchemaTypeName,
    getSessionEventsSchemaPath,
    writeGeneratedFile,
    collectDefinitionCollections,
    postProcessSchema,
    resolveRef,
    resolveObjectSchema,
    resolveSchema,
    refTypeName,
    isRpcMethod,
    isNodeFullyExperimental,
    isNodeFullyDeprecated,
    isSchemaDeprecated,
    isObjectSchema,
    isVoidSchema,
    getNullableInner,
    REPO_ROOT,
    type ApiSchema,
    type DefinitionCollections,
    type RpcMethod,
} from "./utils.js";

const execFileAsync = promisify(execFile);

// ── C# type rename overrides ────────────────────────────────────────────────
// Map generated class names to shorter public-facing names.
// Applied to base classes AND their derived variants (e.g., FooBar → Bar, FooBazShell → BarShell).
const TYPE_RENAMES: Record<string, string> = {
    PermissionRequestedDataPermissionRequest: "PermissionRequest",
};

/** Apply rename to a generated class name, checking both exact match and prefix replacement for derived types. */
function applyTypeRename(className: string): string {
    if (TYPE_RENAMES[className]) return TYPE_RENAMES[className];
    for (const [from, to] of Object.entries(TYPE_RENAMES)) {
        if (className.startsWith(from)) {
            return to + className.slice(from.length);
        }
    }
    return className;
}

// ── C# utilities ────────────────────────────────────────────────────────────

function escapeXml(text: string): string {
    return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

/** Ensures text ends with sentence-ending punctuation. */
function ensureTrailingPunctuation(text: string): string {
    const trimmed = text.trimEnd();
    if (/[.!?]$/.test(trimmed)) return trimmed;
    return `${trimmed}.`;
}

function xmlDocComment(description: string | undefined, indent: string): string[] {
    if (!description) return [];
    const escaped = ensureTrailingPunctuation(escapeXml(description.trim()));
    const lines = escaped.split(/\r?\n/);
    if (lines.length === 1) {
        return [`${indent}/// <summary>${lines[0]}</summary>`];
    }
    return [
        `${indent}/// <summary>`,
        ...lines.map((l) => `${indent}/// ${l}`),
        `${indent}/// </summary>`,
    ];
}

/** Like xmlDocComment but skips XML escaping — use only for codegen-controlled strings that already contain valid XML tags. */
function rawXmlDocSummary(text: string, indent: string): string[] {
    const line = ensureTrailingPunctuation(text.trim());
    return [`${indent}/// <summary>${line}</summary>`];
}

/** Emits a summary (from description or fallback) and, when a real description exists, a remarks line with the fallback. */
function xmlDocCommentWithFallback(description: string | undefined, fallback: string, indent: string): string[] {
    if (description) {
        return [
            ...xmlDocComment(description, indent),
            `${indent}/// <remarks>${ensureTrailingPunctuation(fallback)}</remarks>`,
        ];
    }
    return rawXmlDocSummary(fallback, indent);
}

/** Emits a summary from the schema description, or a fallback naming the property by its JSON key. */
function xmlDocPropertyComment(description: string | undefined, jsonPropName: string, indent: string): string[] {
    if (description) return xmlDocComment(description, indent);
    return rawXmlDocSummary(`Gets or sets the <c>${escapeXml(jsonPropName)}</c> value.`, indent);
}

/** Emits a summary from the schema description, or a generic fallback. */
function xmlDocEnumComment(description: string | undefined, indent: string): string[] {
    if (description) return xmlDocComment(description, indent);
    return rawXmlDocSummary(`Defines the allowed values.`, indent);
}

function toPascalCase(name: string): string {
    if (name.includes("_") || name.includes("-")) {
        return name.split(/[-_]/).map((p) => p.charAt(0).toUpperCase() + p.slice(1)).join("");
    }
    return name.charAt(0).toUpperCase() + name.slice(1);
}

function typeToClassName(typeName: string): string {
    return typeName.split(/[._]/).map((p) => p.charAt(0).toUpperCase() + p.slice(1)).join("");
}

function toPascalCaseEnumMember(value: string): string {
    return value.split(/[-_.]/).map((p) => p.charAt(0).toUpperCase() + p.slice(1)).join("");
}

async function formatCSharpFile(filePath: string): Promise<void> {
    try {
        const projectFile = path.join(REPO_ROOT, "dotnet/src/GitHub.Copilot.SDK.csproj");
        await execFileAsync("dotnet", ["format", projectFile, "--include", filePath]);
        console.log(`  ✓ Formatted with dotnet format`);
    } catch {
        // dotnet format not available, skip
    }
}

function collectRpcMethods(node: Record<string, unknown>): RpcMethod[] {
    const results: RpcMethod[] = [];
    for (const value of Object.values(node)) {
        if (isRpcMethod(value)) {
            results.push(value);
        } else if (typeof value === "object" && value !== null) {
            results.push(...collectRpcMethods(value as Record<string, unknown>));
        }
    }
    return results;
}

function schemaTypeToCSharp(schema: JSONSchema7, required: boolean, knownTypes: Map<string, string>): string {
    const nullableInner = getNullableInner(schema);
    if (nullableInner) {
        // Pass required=true to get the base type, then add "?" for nullable
        return schemaTypeToCSharp(nullableInner, true, knownTypes) + "?";
    }
    if (schema.$ref) {
        const refName = schema.$ref.split("/").pop()!;
        return knownTypes.get(refName) || refName;
    }
    // Titled union schemas (anyOf with a title) — use the title if it's a known generated type
    if (schema.title && schema.anyOf && knownTypes.has(schema.title)) {
        return required ? schema.title : `${schema.title}?`;
    }
    const type = schema.type;
    const format = schema.format;
    // Handle type: ["string", "null"] patterns (nullable string)
    if (Array.isArray(type)) {
        const nonNullTypes = type.filter((t) => t !== "null");
        if (nonNullTypes.length === 1 && nonNullTypes[0] === "string") {
            if (format === "uuid") return "Guid?";
            if (format === "date-time") return "DateTimeOffset?";
            return "string?";
        }
        if (nonNullTypes.length === 1 && (nonNullTypes[0] === "number" || nonNullTypes[0] === "integer")) {
            if (format === "duration") {
                return "TimeSpan?";
            }
            return nonNullTypes[0] === "integer" ? "long?" : "double?";
        }
    }
    if (type === "string") {
        if (format === "uuid") return required ? "Guid" : "Guid?";
        if (format === "date-time") return required ? "DateTimeOffset" : "DateTimeOffset?";
        return required ? "string" : "string?";
    }
    if (type === "number" || type === "integer") {
        if (format === "duration") {
            return required ? "TimeSpan" : "TimeSpan?";
        }
        if (type === "integer") return required ? "long" : "long?";
        return required ? "double" : "double?";
    }
    if (type === "boolean") return required ? "bool" : "bool?";
    if (type === "array") {
        const items = schema.items as JSONSchema7 | undefined;
        const itemType = items ? schemaTypeToCSharp(items, true, knownTypes) : "object";
        return required ? `${itemType}[]` : `${itemType}[]?`;
    }
    if (type === "object") {
        if (schema.additionalProperties && typeof schema.additionalProperties === "object") {
            const valueType = schemaTypeToCSharp(schema.additionalProperties as JSONSchema7, true, knownTypes);
            return required ? `IDictionary<string, ${valueType}>` : `IDictionary<string, ${valueType}>?`;
        }
        return required ? "object" : "object?";
    }
    return required ? "object" : "object?";
}

/** Tracks whether any TimeSpan property was emitted so the converter can be generated. */


/**
 * Emit C# data-annotation attributes for a JSON Schema property.
 * Returns an array of attribute lines (without trailing newlines).
 */
function emitDataAnnotations(schema: JSONSchema7, indent: string): string[] {
    const attrs: string[] = [];
    const format = schema.format;

    // [Url] + [StringSyntax(StringSyntaxAttribute.Uri)] for format: "uri"
    if (format === "uri") {
        attrs.push(`${indent}[Url]`);
        attrs.push(`${indent}[StringSyntax(StringSyntaxAttribute.Uri)]`);
    }

    // [StringSyntax(StringSyntaxAttribute.Regex)] for format: "regex"
    if (format === "regex") {
        attrs.push(`${indent}[StringSyntax(StringSyntaxAttribute.Regex)]`);
    }

    // [Base64String] for base64-encoded string properties
    if (format === "byte" || (schema as Record<string, unknown>).contentEncoding === "base64") {
        attrs.push(`${indent}[Base64String]`);
    }

    // [Range] for minimum/maximum
    const hasMin = typeof schema.minimum === "number";
    const hasMax = typeof schema.maximum === "number";
    if (hasMin || hasMax) {
        const namedArgs: string[] = [];
        if (schema.exclusiveMinimum === true) namedArgs.push("MinimumIsExclusive = true");
        if (schema.exclusiveMaximum === true) namedArgs.push("MaximumIsExclusive = true");
        const namedSuffix = namedArgs.length > 0 ? `, ${namedArgs.join(", ")}` : "";
        if (schema.type === "integer") {
            // Use Range(double, double) for AOT/trimming compatibility.
            // The Range(Type, string, string) overload uses TypeConverter which triggers IL2026.
            const min = hasMin ? String(schema.minimum) : "long.MinValue";
            const max = hasMax ? String(schema.maximum) : "long.MaxValue";
            attrs.push(`${indent}[Range((double)${min}, (double)${max}${namedSuffix})]`);
        } else {
            const min = hasMin ? String(schema.minimum) : "double.MinValue";
            const max = hasMax ? String(schema.maximum) : "double.MaxValue";
            attrs.push(`${indent}[Range(${min}, ${max}${namedSuffix})]`);
        }
    }

    // [RegularExpression] for pattern
    if (typeof schema.pattern === "string") {
        const escaped = schema.pattern.replace(/\\/g, "\\\\").replace(/"/g, '\\"');
        attrs.push(`${indent}[RegularExpression("${escaped}")]`);
    }

    // [MinLength] / [MaxLength] for string constraints
    if (typeof schema.minLength === "number" || typeof schema.maxLength === "number") {
        attrs.push(
            `${indent}[UnconditionalSuppressMessage("Trimming", "IL2026", Justification = "Safe for generated string properties: JSON Schema minLength/maxLength map to string length validation, not reflection over trimmed Count members")]`
        );
    }
    if (typeof schema.minLength === "number") {
        attrs.push(`${indent}[MinLength(${schema.minLength})]`);
    }
    if (typeof schema.maxLength === "number") {
        attrs.push(`${indent}[MaxLength(${schema.maxLength})]`);
    }

    return attrs;
}

/**
 * Returns true when a TimeSpan-typed property needs a [JsonConverter] attribute.
 *
 * NOTE: The runtime schema uses `format: "duration"` on numeric (integer/number) fields
 * to mean "a duration value expressed in milliseconds". This differs from the JSON Schema
 * spec, where `format: "duration"` denotes an ISO 8601 duration string (e.g. "PT1H30M").
 * The generator and runtime agree on this convention, so we map these to TimeSpan with a
 * milliseconds-based JSON converter rather than expecting ISO 8601 strings.
 */
function isDurationProperty(schema: JSONSchema7): boolean {
    if (schema.format === "duration") {
        const t = schema.type;
        if (t === "number" || t === "integer") return true;
        if (Array.isArray(t)) {
            const nonNull = (t as string[]).filter((x) => x !== "null");
            if (nonNull.length === 1 && (nonNull[0] === "number" || nonNull[0] === "integer")) return true;
        }
    }
    return false;
}


const COPYRIGHT = `/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/`;

// ══════════════════════════════════════════════════════════════════════════════
// SESSION EVENTS
// ══════════════════════════════════════════════════════════════════════════════

interface EventVariant {
    typeName: string;
    className: string;
    dataClassName: string;
    dataSchema: JSONSchema7;
    dataDescription?: string;
}

let generatedEnums = new Map<string, { enumName: string; values: string[] }>();

/** Schema definitions available during session event generation (for $ref resolution). */
let sessionDefinitions: DefinitionCollections = { definitions: {}, $defs: {} };

function getOrCreateEnum(parentClassName: string, propName: string, values: string[], enumOutput: string[], description?: string, explicitName?: string, deprecated?: boolean): string {
    const enumName = explicitName ?? `${parentClassName}${propName}`;
    const existing = generatedEnums.get(enumName);
    if (existing) return existing.enumName;
    generatedEnums.set(enumName, { enumName, values });

    const lines: string[] = [];
    lines.push(...xmlDocEnumComment(description, ""));
    if (deprecated) lines.push(`[Obsolete("This member is deprecated and will be removed in a future version.")]`);
    lines.push(`[JsonConverter(typeof(JsonStringEnumConverter<${enumName}>))]`, `public enum ${enumName}`, `{`);
    for (const value of values) {
        lines.push(`    /// <summary>The <c>${escapeXml(value)}</c> variant.</summary>`);
        lines.push(`    [JsonStringEnumMemberName("${value}")]`, `    ${toPascalCaseEnumMember(value)},`);
    }
    lines.push(`}`, "");
    enumOutput.push(lines.join("\n"));
    return enumName;
}

function extractEventVariants(schema: JSONSchema7): EventVariant[] {
    const definitionCollections = collectDefinitionCollections(schema as Record<string, unknown>);
    const sessionEvent =
        resolveSchema({ $ref: "#/definitions/SessionEvent" }, definitionCollections) ??
        resolveSchema({ $ref: "#/$defs/SessionEvent" }, definitionCollections);
    if (!sessionEvent?.anyOf) throw new Error("Schema must have SessionEvent definition with anyOf");

    return sessionEvent.anyOf
        .map((variant) => {
            const resolvedVariant =
                resolveObjectSchema(variant as JSONSchema7, definitionCollections) ??
                resolveSchema(variant as JSONSchema7, definitionCollections) ??
                (variant as JSONSchema7);
            if (typeof resolvedVariant !== "object" || !resolvedVariant.properties) throw new Error("Invalid variant");
            const typeSchema = resolvedVariant.properties.type as JSONSchema7;
            const typeName = typeSchema?.const as string;
            if (!typeName) throw new Error("Variant must have type.const");
            const baseName = typeToClassName(typeName);
            const dataSchema =
                resolveObjectSchema(resolvedVariant.properties.data as JSONSchema7, definitionCollections) ??
                resolveSchema(resolvedVariant.properties.data as JSONSchema7, definitionCollections) ??
                (resolvedVariant.properties.data as JSONSchema7);
            return {
                typeName,
                className: `${baseName}Event`,
                dataClassName: `${baseName}Data`,
                dataSchema,
                dataDescription: dataSchema?.description,
            };
        });
}

/**
 * Find a discriminator property shared by all variants in an anyOf.
 */
function findDiscriminator(variants: JSONSchema7[]): { property: string; mapping: Map<string, JSONSchema7> } | null {
    if (variants.length === 0) return null;
    const firstVariant = variants[0];
    if (!firstVariant.properties) return null;

    for (const [propName, propSchema] of Object.entries(firstVariant.properties).sort(([a], [b]) => a.localeCompare(b))) {
        if (typeof propSchema !== "object") continue;
        const schema = propSchema as JSONSchema7;
        if (schema.const === undefined) continue;

        const mapping = new Map<string, JSONSchema7>();
        let isValidDiscriminator = true;

        for (const variant of variants) {
            if (!variant.properties) { isValidDiscriminator = false; break; }
            const variantProp = variant.properties[propName];
            if (typeof variantProp !== "object") { isValidDiscriminator = false; break; }
            const variantSchema = variantProp as JSONSchema7;
            if (variantSchema.const === undefined) { isValidDiscriminator = false; break; }
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
    enumOutput: string[],
    description?: string
): string {
    const lines: string[] = [];
    const discriminatorInfo = findDiscriminator(variants)!;
    const renamedBase = applyTypeRename(baseClassName);

    lines.push(...xmlDocCommentWithFallback(description, `Polymorphic base type discriminated by <c>${escapeXml(discriminatorProperty)}</c>.`, ""));
    lines.push(`[JsonPolymorphic(`);
    lines.push(`    TypeDiscriminatorPropertyName = "${discriminatorProperty}",`);
    lines.push(`    UnknownDerivedTypeHandling = JsonUnknownDerivedTypeHandling.FallBackToBaseType)]`);

    for (const [constValue] of discriminatorInfo.mapping) {
        const derivedClassName = applyTypeRename(`${baseClassName}${toPascalCase(constValue)}`);
        lines.push(`[JsonDerivedType(typeof(${derivedClassName}), "${constValue}")]`);
    }

    lines.push(`public partial class ${renamedBase}`);
    lines.push(`{`);
    lines.push(`    /// <summary>The type discriminator.</summary>`);
    lines.push(`    [JsonPropertyName("${discriminatorProperty}")]`);
    lines.push(`    public virtual string ${toPascalCase(discriminatorProperty)} { get; set; } = string.Empty;`);
    lines.push(`}`);
    lines.push("");

    for (const [constValue, variant] of discriminatorInfo.mapping) {
        const derivedClassName = applyTypeRename(`${baseClassName}${toPascalCase(constValue)}`);
        const derivedCode = generateDerivedClass(derivedClassName, renamedBase, discriminatorProperty, constValue, variant, knownTypes, nestedClasses, enumOutput);
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

    lines.push(...xmlDocCommentWithFallback(schema.description, `The <c>${escapeXml(discriminatorValue)}</c> variant of <see cref="${baseClassName}"/>.`, ""));
    if (isSchemaDeprecated(schema)) lines.push(`[Obsolete("This member is deprecated and will be removed in a future version.")]`);
    lines.push(`public partial class ${className} : ${baseClassName}`);
    lines.push(`{`);
    lines.push(`    /// <inheritdoc />`);
    lines.push(`    [JsonIgnore]`);
    lines.push(`    public override string ${toPascalCase(discriminatorProperty)} => "${discriminatorValue}";`);
    lines.push("");

    if (schema.properties) {
        for (const [propName, propSchema] of Object.entries(schema.properties).sort(([a], [b]) => a.localeCompare(b))) {
            if (typeof propSchema !== "object") continue;
            if (propName === discriminatorProperty) continue;

            const isReq = required.has(propName);
            const csharpName = toPascalCase(propName);
            const csharpType = resolveSessionPropertyType(propSchema as JSONSchema7, className, csharpName, isReq, knownTypes, nestedClasses, enumOutput);

            lines.push(...xmlDocPropertyComment((propSchema as JSONSchema7).description, propName, "    "));
            lines.push(...emitDataAnnotations(propSchema as JSONSchema7, "    "));
            if (isSchemaDeprecated(propSchema as JSONSchema7)) lines.push(`    [Obsolete("This member is deprecated and will be removed in a future version.")]`);
            if (isDurationProperty(propSchema as JSONSchema7)) lines.push(`    [JsonConverter(typeof(MillisecondsTimeSpanConverter))]`);
            if (!isReq) lines.push(`    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]`);
            lines.push(`    [JsonPropertyName("${propName}")]`);
            const reqMod = isReq && !csharpType.endsWith("?") ? "required " : "";
            lines.push(`    public ${reqMod}${csharpType} ${csharpName} { get; set; }`, "");
        }
    }

    if (lines[lines.length - 1] === "") lines.pop();
    lines.push(`}`);
    return lines.join("\n");
}

function generateNestedClass(
    className: string,
    schema: JSONSchema7,
    knownTypes: Map<string, string>,
    nestedClasses: Map<string, string>,
    enumOutput: string[]
): string {
    const required = new Set(schema.required || []);
    const lines: string[] = [];
    lines.push(...xmlDocCommentWithFallback(schema.description, `Nested data type for <c>${className}</c>.`, ""));
    if (isSchemaDeprecated(schema)) lines.push(`[Obsolete("This member is deprecated and will be removed in a future version.")]`);
    lines.push(`public partial class ${className}`, `{`);

    for (const [propName, propSchema] of Object.entries(schema.properties || {}).sort(([a], [b]) => a.localeCompare(b))) {
        if (typeof propSchema !== "object") continue;
        const prop = propSchema as JSONSchema7;
        const isReq = required.has(propName);
        const csharpName = toPascalCase(propName);
        const csharpType = resolveSessionPropertyType(prop, className, csharpName, isReq, knownTypes, nestedClasses, enumOutput);

        lines.push(...xmlDocPropertyComment(prop.description, propName, "    "));
        lines.push(...emitDataAnnotations(prop, "    "));
        if (isSchemaDeprecated(prop)) lines.push(`    [Obsolete("This member is deprecated and will be removed in a future version.")]`);
        if (isDurationProperty(prop)) lines.push(`    [JsonConverter(typeof(MillisecondsTimeSpanConverter))]`);
        if (!isReq) lines.push(`    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]`);
        lines.push(`    [JsonPropertyName("${propName}")]`);
        const reqMod = isReq && !csharpType.endsWith("?") ? "required " : "";
        lines.push(`    public ${reqMod}${csharpType} ${csharpName} { get; set; }`, "");
    }
    if (lines[lines.length - 1] === "") lines.pop();
    lines.push(`}`);
    return lines.join("\n");
}

function resolveSessionPropertyType(
    propSchema: JSONSchema7,
    parentClassName: string,
    propName: string,
    isRequired: boolean,
    knownTypes: Map<string, string>,
    nestedClasses: Map<string, string>,
    enumOutput: string[]
): string {
    // Handle $ref by resolving against schema definitions
    if (propSchema.$ref) {
        const className = typeToClassName(refTypeName(propSchema.$ref, sessionDefinitions));
        const refSchema = resolveRef(propSchema.$ref, sessionDefinitions);
        if (!refSchema) {
            return isRequired ? className : `${className}?`;
        }

        if (refSchema.enum && Array.isArray(refSchema.enum)) {
            const enumName = getOrCreateEnum(className, "", refSchema.enum as string[], enumOutput, refSchema.description, undefined, isSchemaDeprecated(refSchema));
            return isRequired ? enumName : `${enumName}?`;
        }

        if (refSchema.type === "object" && refSchema.properties) {
            if (!nestedClasses.has(className)) {
                nestedClasses.set(className, generateNestedClass(className, refSchema, knownTypes, nestedClasses, enumOutput));
            }
            return isRequired ? className : `${className}?`;
        }

        return resolveSessionPropertyType(refSchema, parentClassName, propName, isRequired, knownTypes, nestedClasses, enumOutput);
    }
    if (propSchema.anyOf) {
        const simpleNullable = getNullableInner(propSchema);
        if (simpleNullable) {
            return resolveSessionPropertyType(simpleNullable, parentClassName, propName, false, knownTypes, nestedClasses, enumOutput);
        }
        // Discriminated union: anyOf with multiple object variants sharing a const discriminator
        const nonNull = propSchema.anyOf.filter((s) => typeof s === "object" && s !== null && (s as JSONSchema7).type !== "null");
        if (nonNull.length > 1) {
            // Resolve $ref variants to their actual schemas
            const variants = (nonNull as JSONSchema7[]).map((v) => {
                if (v.$ref) {
                    const resolved = resolveRef(v.$ref, sessionDefinitions);
                    return resolved ?? v;
                }
                return v;
            });
            const discriminatorInfo = findDiscriminator(variants);
            if (discriminatorInfo) {
                const hasNull = propSchema.anyOf.length > nonNull.length;
                const baseClassName = (propSchema.title as string) ?? `${parentClassName}${propName}`;
                const renamedBase = applyTypeRename(baseClassName);
                const polymorphicCode = generatePolymorphicClasses(baseClassName, discriminatorInfo.property, variants, knownTypes, nestedClasses, enumOutput, propSchema.description);
                nestedClasses.set(renamedBase, polymorphicCode);
                return isRequired && !hasNull ? renamedBase : `${renamedBase}?`;
            }
        }
        return !isRequired ? "object?" : "object";
    }
    if (propSchema.enum && Array.isArray(propSchema.enum)) {
        const enumName = getOrCreateEnum(parentClassName, propName, propSchema.enum as string[], enumOutput, propSchema.description, propSchema.title as string | undefined, isSchemaDeprecated(propSchema));
        return isRequired ? enumName : `${enumName}?`;
    }
    if (propSchema.type === "object" && propSchema.properties) {
        const nestedClassName = (propSchema.title as string) ?? `${parentClassName}${propName}`;
        nestedClasses.set(nestedClassName, generateNestedClass(nestedClassName, propSchema, knownTypes, nestedClasses, enumOutput));
        return isRequired ? nestedClassName : `${nestedClassName}?`;
    }
    if (propSchema.type === "array" && propSchema.items) {
        const items = propSchema.items as JSONSchema7;
        const itemType = resolveSessionPropertyType(
            items,
            parentClassName,
            `${propName}Item`,
            true,
            knownTypes,
            nestedClasses,
            enumOutput
        );
        return isRequired ? `${itemType}[]` : `${itemType}[]?`;
    }
    if (propSchema.type === "object" && propSchema.additionalProperties && typeof propSchema.additionalProperties === "object") {
        const valueSchema = propSchema.additionalProperties as JSONSchema7;
        const valueType = resolveSessionPropertyType(
            valueSchema,
            parentClassName,
            `${propName}Value`,
            true,
            knownTypes,
            nestedClasses,
            enumOutput
        );
        return isRequired ? `IDictionary<string, ${valueType}>` : `IDictionary<string, ${valueType}>?`;
    }
    return schemaTypeToCSharp(propSchema, isRequired, knownTypes);
}

function generateDataClass(variant: EventVariant, knownTypes: Map<string, string>, nestedClasses: Map<string, string>, enumOutput: string[]): string {
    if (!variant.dataSchema?.properties) return `public partial class ${variant.dataClassName} { }`;

    const required = new Set(variant.dataSchema.required || []);
    const lines: string[] = [];
    if (variant.dataDescription) {
        lines.push(...xmlDocComment(variant.dataDescription, ""));
    } else {
        lines.push(...rawXmlDocSummary(`Event payload for <see cref="${variant.className}"/>.`, ""));
    }
    if (isSchemaDeprecated(variant.dataSchema)) {
        lines.push(`[Obsolete("This member is deprecated and will be removed in a future version.")]`);
    }
    lines.push(`public partial class ${variant.dataClassName}`, `{`);

    for (const [propName, propSchema] of Object.entries(variant.dataSchema.properties).sort(([a], [b]) => a.localeCompare(b))) {
        if (typeof propSchema !== "object") continue;
        const isReq = required.has(propName);
        const csharpName = toPascalCase(propName);
        const csharpType = resolveSessionPropertyType(propSchema as JSONSchema7, variant.dataClassName, csharpName, isReq, knownTypes, nestedClasses, enumOutput);

        lines.push(...xmlDocPropertyComment((propSchema as JSONSchema7).description, propName, "    "));
        lines.push(...emitDataAnnotations(propSchema as JSONSchema7, "    "));
        if (isSchemaDeprecated(propSchema as JSONSchema7)) lines.push(`    [Obsolete("This member is deprecated and will be removed in a future version.")]`);
        if (isDurationProperty(propSchema as JSONSchema7)) lines.push(`    [JsonConverter(typeof(MillisecondsTimeSpanConverter))]`);
        if (!isReq) lines.push(`    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]`);
        lines.push(`    [JsonPropertyName("${propName}")]`);
        const reqMod = isReq && !csharpType.endsWith("?") ? "required " : "";
        lines.push(`    public ${reqMod}${csharpType} ${csharpName} { get; set; }`, "");
    }
    if (lines[lines.length - 1] === "") lines.pop();
    lines.push(`}`);
    return lines.join("\n");
}

function generateSessionEventsCode(schema: JSONSchema7): string {
    generatedEnums.clear();
    sessionDefinitions = collectDefinitionCollections(schema as Record<string, unknown>);
    const variants = extractEventVariants(schema);
    const knownTypes = new Map<string, string>();
    const nestedClasses = new Map<string, string>();
    const enumOutput: string[] = [];

    // Extract descriptions for base class properties from the first variant
    const sessionEventDefinition =
        resolveSchema({ $ref: "#/definitions/SessionEvent" }, sessionDefinitions) ??
        resolveSchema({ $ref: "#/$defs/SessionEvent" }, sessionDefinitions);
    const firstVariant =
        typeof sessionEventDefinition === "object" ? (sessionEventDefinition.anyOf?.[0] as JSONSchema7 | undefined) : undefined;
    const resolvedFirstVariant =
        resolveObjectSchema(firstVariant, sessionDefinitions) ??
        resolveSchema(firstVariant, sessionDefinitions) ??
        firstVariant;
    const baseProps =
        typeof resolvedFirstVariant === "object" && resolvedFirstVariant?.properties ? resolvedFirstVariant.properties : {};
    const baseDesc = (name: string) => {
        const prop = baseProps[name];
        return typeof prop === "object" ? (prop as JSONSchema7).description : undefined;
    };

    const lines: string[] = [];
    lines.push(`${COPYRIGHT}

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

#pragma warning disable CS0612 // Type or member is obsolete
#pragma warning disable CS0618 // Type or member is obsolete (with message)

using System.ComponentModel.DataAnnotations;
using System.Diagnostics;
using System.Diagnostics.CodeAnalysis;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace GitHub.Copilot.SDK;
`);

    // Base class with XML doc
    lines.push(`/// <summary>`);
    lines.push(`/// Provides the base class from which all session events derive.`);
    lines.push(`/// </summary>`);
    lines.push(`[DebuggerDisplay("{DebuggerDisplay,nq}")]`);
    lines.push(`[JsonPolymorphic(`, `    TypeDiscriminatorPropertyName = "type",`, `    IgnoreUnrecognizedTypeDiscriminators = true)]`);
    for (const variant of [...variants].sort((a, b) => a.typeName.localeCompare(b.typeName))) {
        lines.push(`[JsonDerivedType(typeof(${variant.className}), "${variant.typeName}")]`);
    }
    lines.push(`public partial class SessionEvent`, `{`);
    lines.push(...xmlDocComment(baseDesc("id"), "    "));
    lines.push(`    [JsonPropertyName("id")]`, `    public Guid Id { get; set; }`, "");
    lines.push(...xmlDocComment(baseDesc("timestamp"), "    "));
    lines.push(`    [JsonPropertyName("timestamp")]`, `    public DateTimeOffset Timestamp { get; set; }`, "");
    lines.push(...xmlDocComment(baseDesc("parentId"), "    "));
    lines.push(`    [JsonPropertyName("parentId")]`, `    public Guid? ParentId { get; set; }`, "");
    lines.push(...xmlDocComment(baseDesc("ephemeral"), "    "));
    lines.push(`    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]`, `    [JsonPropertyName("ephemeral")]`, `    public bool? Ephemeral { get; set; }`, "");
    lines.push(`    /// <summary>`, `    /// The event type discriminator.`, `    /// </summary>`);
    lines.push(`    [JsonIgnore]`, `    public virtual string Type => "unknown";`, "");
    lines.push(`    /// <summary>Deserializes a JSON string into a <see cref="SessionEvent"/>.</summary>`);
    lines.push(`    public static SessionEvent FromJson(string json) =>`, `        JsonSerializer.Deserialize(json, SessionEventsJsonContext.Default.SessionEvent)!;`, "");
    lines.push(`    /// <summary>Serializes this event to a JSON string.</summary>`);
    lines.push(`    public string ToJson() =>`, `        JsonSerializer.Serialize(this, SessionEventsJsonContext.Default.SessionEvent);`, "");
    lines.push(`    [DebuggerBrowsable(DebuggerBrowsableState.Never)]`, `    private string DebuggerDisplay => ToJson();`);
    lines.push(`}`, "");

    // Event classes with XML docs
    for (const variant of variants) {
        const remarksLine = `/// <remarks>Represents the <c>${escapeXml(variant.typeName)}</c> event.</remarks>`;
        if (variant.dataDescription) {
            lines.push(...xmlDocComment(variant.dataDescription, ""));
            lines.push(remarksLine);
        } else {
            lines.push(`/// <summary>Represents the <c>${escapeXml(variant.typeName)}</c> event.</summary>`);
        }
        lines.push(`public partial class ${variant.className} : SessionEvent`, `{`);
        lines.push(`    /// <inheritdoc />`);
        lines.push(`    [JsonIgnore]`, `    public override string Type => "${variant.typeName}";`, "");
        lines.push(`    /// <summary>The <c>${escapeXml(variant.typeName)}</c> event payload.</summary>`);
        lines.push(`    [JsonPropertyName("data")]`, `    public required ${variant.dataClassName} Data { get; set; }`, `}`, "");
    }

    // Data classes
    for (const variant of variants) {
        lines.push(generateDataClass(variant, knownTypes, nestedClasses, enumOutput), "");
    }

    // Nested classes
    for (const [, code] of nestedClasses) lines.push(code, "");

    // Enums
    for (const code of enumOutput) lines.push(code);

    // JsonSerializerContext
    const types = ["SessionEvent", ...variants.flatMap((v) => [v.className, v.dataClassName]), ...nestedClasses.keys()].sort();
    lines.push(`[JsonSourceGenerationOptions(`, `    JsonSerializerDefaults.Web,`, `    AllowOutOfOrderMetadataProperties = true,`, `    NumberHandling = JsonNumberHandling.AllowReadingFromString,`, `    DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull)]`);
    for (const t of types) lines.push(`[JsonSerializable(typeof(${t}))]`);
    lines.push(`[JsonSerializable(typeof(JsonElement))]`);
    lines.push(`internal partial class SessionEventsJsonContext : JsonSerializerContext;`);

    return lines.join("\n");
}

export async function generateSessionEvents(schemaPath?: string): Promise<void> {
    console.log("C#: generating session-events...");
    const resolvedPath = schemaPath ?? (await getSessionEventsSchemaPath());
    const schema = cloneSchemaForCodegen(JSON.parse(await fs.readFile(resolvedPath, "utf-8")) as JSONSchema7);
    const processed = postProcessSchema(schema);
    const code = generateSessionEventsCode(processed);
    const outPath = await writeGeneratedFile("dotnet/src/Generated/SessionEvents.cs", code);
    console.log(`  ✓ ${outPath}`);
    await formatCSharpFile(outPath);
}

// ══════════════════════════════════════════════════════════════════════════════
// RPC TYPES
// ══════════════════════════════════════════════════════════════════════════════

let emittedRpcClassSchemas = new Map<string, string>();
let emittedRpcEnumResultTypes = new Set<string>();
let experimentalRpcTypes = new Set<string>();
let rpcKnownTypes = new Map<string, string>();
let rpcEnumOutput: string[] = [];

/** Schema definitions available during RPC generation (for $ref resolution). */
let rpcDefinitions: DefinitionCollections = { definitions: {}, $defs: {} };

function singularPascal(s: string): string {
    const p = toPascalCase(s);
    if (p.endsWith("ies")) return `${p.slice(0, -3)}y`;
    if (/(xes|zes|ches|shes|sses)$/i.test(p)) return p.slice(0, -2);
    if (p.endsWith("s") && !/(ss|us|is)$/i.test(p)) return p.slice(0, -1);
    return p;
}

function getMethodResultSchema(method: RpcMethod): JSONSchema7 | undefined {
    return resolveSchema(method.result, rpcDefinitions) ?? method.result ?? undefined;
}

function resultTypeName(method: RpcMethod): string {
    return getRpcSchemaTypeName(getMethodResultSchema(method), `${typeToClassName(method.rpcMethod)}Result`);
}

/** Returns the C# type for a method's result, accounting for nullable anyOf wrappers. */
function resolvedResultTypeName(method: RpcMethod): string {
    const schema = getMethodResultSchema(method);
    if (!schema) return resultTypeName(method);
    const inner = getNullableInner(schema);
    if (inner) {
        // Nullable wrapper: resolve the inner $ref type name with "?" suffix
        const innerName = inner.$ref
            ? typeToClassName(refTypeName(inner.$ref, rpcDefinitions))
            : getRpcSchemaTypeName(inner, resultTypeName(method));
        return `${innerName}?`;
    }
    return resultTypeName(method);
}

/** Returns the Task<T> or Task string for a method's result type. */
function resultTaskType(method: RpcMethod): string {
    const schema = getMethodResultSchema(method);
    return !isVoidSchema(schema) ? `Task<${resolvedResultTypeName(method)}>` : "Task";
}

function paramsTypeName(method: RpcMethod): string {
    return getRpcSchemaTypeName(resolveMethodParamsSchema(method), `${typeToClassName(method.rpcMethod)}Request`);
}

function resolveMethodParamsSchema(method: RpcMethod): JSONSchema7 | undefined {
    return (
        resolveObjectSchema(method.params, rpcDefinitions) ??
        resolveSchema(method.params, rpcDefinitions) ??
        method.params ??
        undefined
    );
}

function stableStringify(value: unknown): string {
    if (Array.isArray(value)) {
        return `[${value.map((item) => stableStringify(item)).join(",")}]`;
    }
    if (value && typeof value === "object") {
        const entries = Object.entries(value as Record<string, unknown>).sort(([a], [b]) => a.localeCompare(b));
        return `{${entries.map(([key, entryValue]) => `${JSON.stringify(key)}:${stableStringify(entryValue)}`).join(",")}}`;
    }
    return JSON.stringify(value);
}

function resolveRpcType(schema: JSONSchema7, isRequired: boolean, parentClassName: string, propName: string, classes: string[]): string {
    // Handle $ref by resolving against schema definitions and generating the referenced class
    if (schema.$ref) {
        const typeName = typeToClassName(refTypeName(schema.$ref, rpcDefinitions));
        const refSchema = resolveRef(schema.$ref, rpcDefinitions);
        if (!refSchema) {
            return isRequired ? typeName : `${typeName}?`;
        }

        if (refSchema.enum && Array.isArray(refSchema.enum)) {
            const enumName = getOrCreateEnum(typeName, "", refSchema.enum as string[], rpcEnumOutput, refSchema.description, undefined, isSchemaDeprecated(refSchema));
            return isRequired ? enumName : `${enumName}?`;
        }

        if (refSchema.type === "object" && refSchema.properties) {
            const cls = emitRpcClass(typeName, refSchema, "public", classes);
            if (cls) classes.push(cls);
            return isRequired ? typeName : `${typeName}?`;
        }

        return resolveRpcType(refSchema, isRequired, parentClassName, propName, classes);
    }
    // Handle anyOf: [T, null/{not:{}}] → T? (nullable typed property)
    const nullableInner = getNullableInner(schema);
    if (nullableInner) {
        return resolveRpcType(nullableInner, false, parentClassName, propName, classes);
    }
    // Discriminated union: anyOf with multiple variants sharing a const discriminator
    if (schema.anyOf && Array.isArray(schema.anyOf)) {
        const nonNull = schema.anyOf.filter((s) => typeof s === "object" && s !== null && (s as JSONSchema7).type !== "null");
        if (nonNull.length > 1) {
            const variants = (nonNull as JSONSchema7[]).map((v) => {
                if (v.$ref) {
                    const resolved = resolveRef(v.$ref, rpcDefinitions);
                    return resolved ?? v;
                }
                return v;
            });
            const discriminatorInfo = findDiscriminator(variants);
            if (discriminatorInfo) {
                const hasNull = schema.anyOf.length > nonNull.length;
                const baseClassName = (schema.title as string) ?? `${parentClassName}${propName}`;
                if (!emittedRpcClassSchemas.has(baseClassName)) {
                    emittedRpcClassSchemas.set(baseClassName, "polymorphic");
                    const nestedMap = new Map<string, string>();
                    const polymorphicCode = generatePolymorphicClasses(baseClassName, discriminatorInfo.property, variants, rpcKnownTypes, nestedMap, rpcEnumOutput, schema.description);
                    classes.push(polymorphicCode);
                    for (const nested of nestedMap.values()) classes.push(nested);
                }
                return isRequired && !hasNull ? baseClassName : `${baseClassName}?`;
            }
        }
    }
    // Handle enums (string unions like "interactive" | "plan" | "autopilot")
    if (schema.enum && Array.isArray(schema.enum)) {
        const enumName = getOrCreateEnum(
            parentClassName,
            propName,
            schema.enum as string[],
            rpcEnumOutput,
            schema.description,
            schema.title as string | undefined,
            isSchemaDeprecated(schema),
        );
        return isRequired ? enumName : `${enumName}?`;
    }
    if (schema.type === "object" && schema.properties) {
        const className = (schema.title as string) ?? `${parentClassName}${propName}`;
        classes.push(emitRpcClass(className, schema, "public", classes));
        return isRequired ? className : `${className}?`;
    }
    if (schema.type === "array" && schema.items) {
        const items = schema.items as JSONSchema7;
        if (items.type === "object" && items.properties) {
            const itemClass = (items.title as string) ?? `${parentClassName}${singularPascal(propName)}`;
            classes.push(emitRpcClass(itemClass, items, "public", classes));
            return isRequired ? `IList<${itemClass}>` : `IList<${itemClass}>?`;
        }
        const itemType = resolveRpcType(items, true, parentClassName, `${propName}Item`, classes);
        return isRequired ? `IList<${itemType}>` : `IList<${itemType}>?`;
    }
    if (schema.type === "object" && schema.additionalProperties && typeof schema.additionalProperties === "object") {
        const vs = schema.additionalProperties as JSONSchema7;
        const valueType = resolveRpcType(vs, true, parentClassName, `${propName}Value`, classes);
        return isRequired ? `IDictionary<string, ${valueType}>` : `IDictionary<string, ${valueType}>?`;
    }
    return schemaTypeToCSharp(schema, isRequired, rpcKnownTypes);
}

function emitRpcClass(
    className: string,
    schema: JSONSchema7,
    visibility: "public" | "internal",
    extraClasses: string[]
): string {
    const effectiveSchema =
        resolveObjectSchema(schema, rpcDefinitions) ??
        resolveSchema(schema, rpcDefinitions) ??
        schema;
    const schemaKey = stableStringify(effectiveSchema);
    const existingSchema = emittedRpcClassSchemas.get(className);
    if (existingSchema) {
        if (existingSchema !== schemaKey) {
            throw new Error(
                `Conflicting RPC class name "${className}" for different schemas. Add a schema title/withTypeName to disambiguate.`
            );
        }
        return "";
    }

    emittedRpcClassSchemas.set(className, schemaKey);

    const requiredSet = new Set(effectiveSchema.required || []);
    const lines: string[] = [];
    lines.push(...xmlDocComment(schema.description || effectiveSchema.description || `RPC data type for ${className.replace(/(Request|Result|Params)$/, "")} operations.`, ""));
    if (experimentalRpcTypes.has(className)) {
        lines.push(`[Experimental(Diagnostics.Experimental)]`);
    }
    if (isSchemaDeprecated(schema) || isSchemaDeprecated(effectiveSchema)) {
        lines.push(`[Obsolete("This member is deprecated and will be removed in a future version.")]`);
    }
    lines.push(`${visibility} sealed class ${className}`, `{`);

    const props = Object.entries(effectiveSchema.properties || {}).sort(([a], [b]) => a.localeCompare(b));
    for (let i = 0; i < props.length; i++) {
        const [propName, propSchema] = props[i];
        if (typeof propSchema !== "object") continue;
        const prop = propSchema as JSONSchema7;
        const isReq = requiredSet.has(propName);
        const csharpName = toPascalCase(propName);
        const csharpType = resolveRpcType(prop, isReq, className, csharpName, extraClasses);

        lines.push(...xmlDocPropertyComment(prop.description, propName, "    "));
        lines.push(...emitDataAnnotations(prop, "    "));
        if (isSchemaDeprecated(prop)) lines.push(`    [Obsolete("This member is deprecated and will be removed in a future version.")]`);
        if (isDurationProperty(prop)) lines.push(`    [JsonConverter(typeof(MillisecondsTimeSpanConverter))]`);
        lines.push(`    [JsonPropertyName("${propName}")]`);

        let defaultVal = "";
        let propAccessors = "{ get; set; }";
        if (isReq && !csharpType.endsWith("?")) {
            if (csharpType === "string") defaultVal = " = string.Empty;";
            else if (csharpType === "object") defaultVal = " = null!;";
            else if (csharpType.startsWith("IList<")) {
                propAccessors = "{ get => field ??= []; set; }";
            } else if (csharpType.startsWith("IDictionary<")) {
                const concreteType = csharpType.replace("IDictionary<", "Dictionary<");
                propAccessors = `{ get => field ??= new ${concreteType}(); set; }`;
            } else if (emittedRpcClassSchemas.has(csharpType)) {
                propAccessors = "{ get => field ??= new(); set; }";
            }
        }
        lines.push(`    public ${csharpType} ${csharpName} ${propAccessors}${defaultVal}`);
        if (i < props.length - 1) lines.push("");
    }
    lines.push(`}`);
    return lines.join("\n");
}

/**
 * Emit the type for a non-object RPC result schema (e.g., a bare enum).
 * Returns the C# type name to use in method signatures. For enums, ensures the enum
 * is created via getOrCreateEnum. For other primitives, returns the mapped C# type.
 */
function emitNonObjectResultType(typeName: string, schema: JSONSchema7, classes: string[]): string {
    if (schema.enum && Array.isArray(schema.enum)) {
        const enumName = getOrCreateEnum("", typeName, schema.enum as string[], rpcEnumOutput, schema.description, typeName, isSchemaDeprecated(schema));
        emittedRpcEnumResultTypes.add(enumName);
        return enumName;
    }
    // For other non-object types, use the basic type mapping
    return schemaTypeToCSharp(schema, true, rpcKnownTypes);
}

/**
 * Emit ServerRpc as an instance class (like SessionRpc but without sessionId).
 */
function emitServerRpcClasses(node: Record<string, unknown>, classes: string[]): string[] {
    const result: string[] = [];

    // Find top-level groups (e.g. "models", "tools", "account")
    const groups = Object.entries(node).filter(([, v]) => typeof v === "object" && v !== null && !isRpcMethod(v));
    // Find top-level methods (e.g. "ping")
    const topLevelMethods = Object.entries(node).filter(([, v]) => isRpcMethod(v));

    // ServerRpc class
    const srLines: string[] = [];
    srLines.push(`/// <summary>Provides server-scoped RPC methods (no session required).</summary>`);
    srLines.push(`public sealed class ServerRpc`);
    srLines.push(`{`);
    srLines.push(`    private readonly JsonRpc _rpc;`);
    srLines.push("");
    srLines.push(`    internal ServerRpc(JsonRpc rpc)`);
    srLines.push(`    {`);
    srLines.push(`        _rpc = rpc;`);
    for (const [groupName] of groups) {
        srLines.push(`        ${toPascalCase(groupName)} = new Server${toPascalCase(groupName)}Api(rpc);`);
    }
    srLines.push(`    }`);

    // Top-level methods (like ping)
    for (const [key, value] of topLevelMethods) {
        if (!isRpcMethod(value)) continue;
        emitServerInstanceMethod(key, value, srLines, classes, "    ", false, false);
    }

    // Group properties
    for (const [groupName] of groups) {
        srLines.push("");
        srLines.push(`    /// <summary>${toPascalCase(groupName)} APIs.</summary>`);
        srLines.push(`    public Server${toPascalCase(groupName)}Api ${toPascalCase(groupName)} { get; }`);
    }

    srLines.push(`}`);
    result.push(srLines.join("\n"));

    // Per-group API classes
    for (const [groupName, groupNode] of groups) {
        result.push(...emitServerApiClass(`Server${toPascalCase(groupName)}Api`, groupNode as Record<string, unknown>, classes));
    }

    return result;
}

function emitServerApiClass(className: string, node: Record<string, unknown>, classes: string[]): string[] {
    const parts: string[] = [];
    const lines: string[] = [];
    const displayName = className.replace(/^Server/, "").replace(/Api$/, "");
    const subGroups = Object.entries(node).filter(([, v]) => typeof v === "object" && v !== null && !isRpcMethod(v));

    lines.push(`/// <summary>Provides server-scoped ${displayName} APIs.</summary>`);
    const groupExperimental = isNodeFullyExperimental(node);
    const groupDeprecated = isNodeFullyDeprecated(node);
    if (groupExperimental) {
        lines.push(`[Experimental(Diagnostics.Experimental)]`);
    }
    if (groupDeprecated) {
        lines.push(`[Obsolete("This member is deprecated and will be removed in a future version.")]`);
    }
    lines.push(`public sealed class ${className}`);
    lines.push(`{`);
    lines.push(`    private readonly JsonRpc _rpc;`);
    lines.push("");
    lines.push(`    internal ${className}(JsonRpc rpc)`);
    lines.push(`    {`);
    lines.push(`        _rpc = rpc;`);
    for (const [subGroupName] of subGroups) {
        const subClassName = className.replace(/Api$/, "") + toPascalCase(subGroupName) + "Api";
        lines.push(`        ${toPascalCase(subGroupName)} = new ${subClassName}(rpc);`);
    }
    lines.push(`    }`);

    for (const [key, value] of Object.entries(node)) {
        if (!isRpcMethod(value)) continue;
        emitServerInstanceMethod(key, value, lines, classes, "    ", groupExperimental, groupDeprecated);
    }

    for (const [subGroupName] of subGroups) {
        const subClassName = className.replace(/Api$/, "") + toPascalCase(subGroupName) + "Api";
        lines.push("");
        lines.push(`    /// <summary>${toPascalCase(subGroupName)} APIs.</summary>`);
        lines.push(`    public ${subClassName} ${toPascalCase(subGroupName)} { get; }`);
    }

    lines.push(`}`);
    parts.push(lines.join("\n"));

    for (const [subGroupName, subGroupNode] of subGroups) {
        const subClassName = className.replace(/Api$/, "") + toPascalCase(subGroupName) + "Api";
        parts.push(...emitServerApiClass(subClassName, subGroupNode as Record<string, unknown>, classes));
    }

    return parts;
}

function emitServerInstanceMethod(
    name: string,
    method: RpcMethod,
    lines: string[],
    classes: string[],
    indent: string,
    groupExperimental: boolean,
    groupDeprecated: boolean
): void {
    const methodName = toPascalCase(name);
    const resultSchema = getMethodResultSchema(method);
    let resultClassName = !isVoidSchema(resultSchema) ? resultTypeName(method) : "";
    if (!isVoidSchema(resultSchema) && method.stability === "experimental") {
        experimentalRpcTypes.add(resultClassName);
    }
    if (isObjectSchema(resultSchema)) {
        const resultClass = emitRpcClass(resultClassName, resultSchema!, "public", classes);
        if (resultClass) classes.push(resultClass);
    } else if (!isVoidSchema(resultSchema)) {
        resultClassName = emitNonObjectResultType(resultClassName, resultSchema!, classes);
    }

    const effectiveParams = resolveMethodParamsSchema(method);
    const paramEntries = effectiveParams?.properties ? Object.entries(effectiveParams.properties) : [];
    const requiredSet = new Set(effectiveParams?.required || []);

    // Sort so required params come before optional (C# requires defaults at end)
    paramEntries.sort((a, b) => {
        const aReq = requiredSet.has(a[0]) ? 0 : 1;
        const bReq = requiredSet.has(b[0]) ? 0 : 1;
        return aReq - bReq;
    });

    let requestClassName: string | null = null;
    if (paramEntries.length > 0) {
        requestClassName = paramsTypeName(method);
        if (method.stability === "experimental") {
            experimentalRpcTypes.add(requestClassName);
        }
        const reqClass = emitRpcClass(requestClassName, effectiveParams!, "internal", classes);
        if (reqClass) classes.push(reqClass);
    }

    lines.push("");
    lines.push(`${indent}/// <summary>Calls "${method.rpcMethod}".</summary>`);
    if (method.stability === "experimental" && !groupExperimental) {
        lines.push(`${indent}[Experimental(Diagnostics.Experimental)]`);
    }
    if (method.deprecated && !groupDeprecated) {
        lines.push(`${indent}[Obsolete("This member is deprecated and will be removed in a future version.")]`);
    }

    const sigParams: string[] = [];
    const bodyAssignments: string[] = [];

    for (const [pName, pSchema] of paramEntries) {
        if (typeof pSchema !== "object") continue;
        const isReq = requiredSet.has(pName);
        const jsonSchema = pSchema as JSONSchema7;
        const csType = requestClassName
            ? resolveRpcType(jsonSchema, isReq, requestClassName, toPascalCase(pName), classes)
            : schemaTypeToCSharp(jsonSchema, isReq, rpcKnownTypes);
        sigParams.push(`${csType} ${pName}${isReq ? "" : " = null"}`);
        bodyAssignments.push(`${toPascalCase(pName)} = ${pName}`);
    }
    sigParams.push("CancellationToken cancellationToken = default");

    const taskType = !isVoidSchema(resultSchema) ? `Task<${resultClassName}>` : "Task";
    lines.push(`${indent}public async ${taskType} ${methodName}Async(${sigParams.join(", ")})`);
    lines.push(`${indent}{`);
    if (requestClassName && bodyAssignments.length > 0) {
        lines.push(`${indent}    var request = new ${requestClassName} { ${bodyAssignments.join(", ")} };`);
        if (!isVoidSchema(resultSchema)) {
            lines.push(`${indent}    return await CopilotClient.InvokeRpcAsync<${resultClassName}>(_rpc, "${method.rpcMethod}", [request], cancellationToken);`);
        } else {
            lines.push(`${indent}    await CopilotClient.InvokeRpcAsync(_rpc, "${method.rpcMethod}", [request], cancellationToken);`);
        }
    } else {
        if (!isVoidSchema(resultSchema)) {
            lines.push(`${indent}    return await CopilotClient.InvokeRpcAsync<${resultClassName}>(_rpc, "${method.rpcMethod}", [], cancellationToken);`);
        } else {
            lines.push(`${indent}    await CopilotClient.InvokeRpcAsync(_rpc, "${method.rpcMethod}", [], cancellationToken);`);
        }
    }
    lines.push(`${indent}}`);
}

function emitSessionRpcClasses(node: Record<string, unknown>, classes: string[]): string[] {
    const result: string[] = [];
    const groups = Object.entries(node).filter(([, v]) => typeof v === "object" && v !== null && !isRpcMethod(v));
    const topLevelMethods = Object.entries(node).filter(([, v]) => isRpcMethod(v));

    const srLines = [`/// <summary>Provides typed session-scoped RPC methods.</summary>`, `public sealed class SessionRpc`, `{`, `    private readonly JsonRpc _rpc;`, `    private readonly string _sessionId;`, ""];
    srLines.push(`    internal SessionRpc(JsonRpc rpc, string sessionId)`, `    {`, `        _rpc = rpc;`, `        _sessionId = sessionId;`);
    for (const [groupName] of groups) srLines.push(`        ${toPascalCase(groupName)} = new ${toPascalCase(groupName)}Api(rpc, sessionId);`);
    srLines.push(`    }`);
    for (const [groupName] of groups) srLines.push("", `    /// <summary>${toPascalCase(groupName)} APIs.</summary>`, `    public ${toPascalCase(groupName)}Api ${toPascalCase(groupName)} { get; }`);

    // Emit top-level session RPC methods directly on the SessionRpc class
    const topLevelLines: string[] = [];
    for (const [key, value] of topLevelMethods) {
        emitSessionMethod(key, value as RpcMethod, topLevelLines, classes, "    ", false, false);
    }
    srLines.push(...topLevelLines);

    srLines.push(`}`);
    result.push(srLines.join("\n"));

    for (const [groupName, groupNode] of groups) {
        result.push(...emitSessionApiClass(`${toPascalCase(groupName)}Api`, groupNode as Record<string, unknown>, classes));
    }
    return result;
}

function emitSessionMethod(key: string, method: RpcMethod, lines: string[], classes: string[], indent: string, groupExperimental: boolean, groupDeprecated: boolean): void {
    const methodName = toPascalCase(key);
    const resultSchema = getMethodResultSchema(method);
    let resultClassName = !isVoidSchema(resultSchema) ? resultTypeName(method) : "";
    if (!isVoidSchema(resultSchema) && method.stability === "experimental") {
        experimentalRpcTypes.add(resultClassName);
    }
    if (isObjectSchema(resultSchema)) {
        const resultClass = emitRpcClass(resultClassName, resultSchema!, "public", classes);
        if (resultClass) classes.push(resultClass);
    } else if (!isVoidSchema(resultSchema)) {
        resultClassName = emitNonObjectResultType(resultClassName, resultSchema!, classes);
    }

    const effectiveParams = resolveMethodParamsSchema(method);
    const paramEntries = (effectiveParams?.properties ? Object.entries(effectiveParams.properties) : []).filter(([k]) => k !== "sessionId");
    const requiredSet = new Set(effectiveParams?.required || []);

    // Sort so required params come before optional (C# requires defaults at end)
    paramEntries.sort((a, b) => {
        const aReq = requiredSet.has(a[0]) ? 0 : 1;
        const bReq = requiredSet.has(b[0]) ? 0 : 1;
        return aReq - bReq;
    });

    const requestClassName = paramsTypeName(method);
    if (method.stability === "experimental") {
        experimentalRpcTypes.add(requestClassName);
    }
    if (effectiveParams?.properties && Object.keys(effectiveParams.properties).length > 0) {
        const reqClass = emitRpcClass(requestClassName, effectiveParams, "internal", classes);
        if (reqClass) classes.push(reqClass);
    }

    lines.push("", `${indent}/// <summary>Calls "${method.rpcMethod}".</summary>`);
    if (method.stability === "experimental" && !groupExperimental) {
        lines.push(`${indent}[Experimental(Diagnostics.Experimental)]`);
    }
    if (method.deprecated && !groupDeprecated) {
        lines.push(`${indent}[Obsolete("This member is deprecated and will be removed in a future version.")]`);
    }
    const sigParams: string[] = [];
    const bodyAssignments = [`SessionId = _sessionId`];

    for (const [pName, pSchema] of paramEntries) {
        if (typeof pSchema !== "object") continue;
        const isReq = requiredSet.has(pName);
        const csType = resolveRpcType(pSchema as JSONSchema7, isReq, requestClassName, toPascalCase(pName), classes);
        sigParams.push(`${csType} ${pName}${isReq ? "" : " = null"}`);
        bodyAssignments.push(`${toPascalCase(pName)} = ${pName}`);
    }
    sigParams.push("CancellationToken cancellationToken = default");

    const taskType = !isVoidSchema(resultSchema) ? `Task<${resultClassName}>` : "Task";
    lines.push(`${indent}public async ${taskType} ${methodName}Async(${sigParams.join(", ")})`);
    lines.push(`${indent}{`, `${indent}    var request = new ${requestClassName} { ${bodyAssignments.join(", ")} };`);
    if (!isVoidSchema(resultSchema)) {
        lines.push(`${indent}    return await CopilotClient.InvokeRpcAsync<${resultClassName}>(_rpc, "${method.rpcMethod}", [request], cancellationToken);`, `${indent}}`);
    } else {
        lines.push(`${indent}    await CopilotClient.InvokeRpcAsync(_rpc, "${method.rpcMethod}", [request], cancellationToken);`, `${indent}}`);
    }
}

function emitSessionApiClass(className: string, node: Record<string, unknown>, classes: string[]): string[] {
    const parts: string[] = [];
    const displayName = className.replace(/Api$/, "");
    const groupExperimental = isNodeFullyExperimental(node);
    const groupDeprecated = isNodeFullyDeprecated(node);
    const experimentalAttr = groupExperimental ? `[Experimental(Diagnostics.Experimental)]\n` : "";
    const deprecatedAttr = groupDeprecated ? `[Obsolete("This member is deprecated and will be removed in a future version.")]\n` : "";
    const subGroups = Object.entries(node).filter(([, v]) => typeof v === "object" && v !== null && !isRpcMethod(v));

    const lines = [`/// <summary>Provides session-scoped ${displayName} APIs.</summary>`, `${experimentalAttr}${deprecatedAttr}public sealed class ${className}`, `{`, `    private readonly JsonRpc _rpc;`, `    private readonly string _sessionId;`, ""];
    lines.push(`    internal ${className}(JsonRpc rpc, string sessionId)`, `    {`, `        _rpc = rpc;`, `        _sessionId = sessionId;`);
    for (const [subGroupName] of subGroups) {
        const subClassName = className.replace(/Api$/, "") + toPascalCase(subGroupName) + "Api";
        lines.push(`        ${toPascalCase(subGroupName)} = new ${subClassName}(rpc, sessionId);`);
    }
    lines.push(`    }`);

    for (const [key, value] of Object.entries(node)) {
        if (!isRpcMethod(value)) continue;
        emitSessionMethod(key, value, lines, classes, "    ", groupExperimental, groupDeprecated);
    }

    for (const [subGroupName] of subGroups) {
        const subClassName = className.replace(/Api$/, "") + toPascalCase(subGroupName) + "Api";
        lines.push("");
        lines.push(`    /// <summary>${toPascalCase(subGroupName)} APIs.</summary>`);
        lines.push(`    public ${subClassName} ${toPascalCase(subGroupName)} { get; }`);
    }

    lines.push(`}`);
    parts.push(lines.join("\n"));

    for (const [subGroupName, subGroupNode] of subGroups) {
        const subClassName = className.replace(/Api$/, "") + toPascalCase(subGroupName) + "Api";
        parts.push(...emitSessionApiClass(subClassName, subGroupNode as Record<string, unknown>, classes));
    }

    return parts;
}

function collectClientGroups(node: Record<string, unknown>): Array<{ groupName: string; groupNode: Record<string, unknown>; methods: RpcMethod[] }> {
    const groups: Array<{ groupName: string; groupNode: Record<string, unknown>; methods: RpcMethod[] }> = [];
    for (const [groupName, groupNode] of Object.entries(node)) {
        if (typeof groupNode === "object" && groupNode !== null) {
            groups.push({
                groupName,
                groupNode: groupNode as Record<string, unknown>,
                methods: collectRpcMethods(groupNode as Record<string, unknown>),
            });
        }
    }
    return groups;
}

function clientHandlerInterfaceName(groupName: string): string {
    return `I${toPascalCase(groupName)}Handler`;
}

function clientHandlerMethodName(rpcMethod: string): string {
    const parts = rpcMethod.split(".");
    return `${toPascalCase(parts[parts.length - 1])}Async`;
}

function emitClientSessionApiRegistration(clientSchema: Record<string, unknown>, classes: string[]): string[] {
    const lines: string[] = [];
    const groups = collectClientGroups(clientSchema);

    for (const { methods } of groups) {
        for (const method of methods) {
            const resultSchema = getMethodResultSchema(method);
            if (!isVoidSchema(resultSchema)) {
                if (isObjectSchema(resultSchema)) {
                    const resultClass = emitRpcClass(resultTypeName(method), resultSchema!, "public", classes);
                    if (resultClass) classes.push(resultClass);
                } else {
                    emitNonObjectResultType(resultTypeName(method), resultSchema!, classes);
                }
            }

            const effectiveParams = resolveMethodParamsSchema(method);
            if (effectiveParams?.properties && Object.keys(effectiveParams.properties).length > 0) {
                const paramsClass = emitRpcClass(paramsTypeName(method), effectiveParams, "public", classes);
                if (paramsClass) classes.push(paramsClass);
            }
        }
    }

    for (const { groupName, groupNode, methods } of groups) {
        const interfaceName = clientHandlerInterfaceName(groupName);
        const groupExperimental = isNodeFullyExperimental(groupNode);
        const groupDeprecated = isNodeFullyDeprecated(groupNode);
        lines.push(`/// <summary>Handles \`${groupName}\` client session API methods.</summary>`);
        if (groupExperimental) {
            lines.push(`[Experimental(Diagnostics.Experimental)]`);
        }
        if (groupDeprecated) {
            lines.push(`[Obsolete("This member is deprecated and will be removed in a future version.")]`);
        }
        lines.push(`public interface ${interfaceName}`);
        lines.push(`{`);
        for (const method of methods) {
            const effectiveParams = resolveMethodParamsSchema(method);
            const hasParams = !!effectiveParams?.properties && Object.keys(effectiveParams.properties).length > 0;
            const resultSchema = getMethodResultSchema(method);
            const taskType = resultTaskType(method);
            lines.push(`    /// <summary>Handles "${method.rpcMethod}".</summary>`);
            if (method.stability === "experimental" && !groupExperimental) {
                lines.push(`    [Experimental(Diagnostics.Experimental)]`);
            }
            if (method.deprecated && !groupDeprecated) {
                lines.push(`    [Obsolete("This member is deprecated and will be removed in a future version.")]`);
            }
            if (hasParams) {
                lines.push(`    ${taskType} ${clientHandlerMethodName(method.rpcMethod)}(${paramsTypeName(method)} request, CancellationToken cancellationToken = default);`);
            } else {
                lines.push(`    ${taskType} ${clientHandlerMethodName(method.rpcMethod)}(CancellationToken cancellationToken = default);`);
            }
        }
        lines.push(`}`);
        lines.push("");
    }

    lines.push(`/// <summary>Provides all client session API handler groups for a session.</summary>`);
    lines.push(`public sealed class ClientSessionApiHandlers`);
    lines.push(`{`);
    for (const { groupName } of groups) {
        lines.push(`    /// <summary>Optional handler for ${toPascalCase(groupName)} client session API methods.</summary>`);
        lines.push(`    public ${clientHandlerInterfaceName(groupName)}? ${toPascalCase(groupName)} { get; set; }`);
        lines.push("");
    }
    if (lines[lines.length - 1] === "") lines.pop();
    lines.push(`}`);
    lines.push("");

    lines.push(`/// <summary>Registers client session API handlers on a JSON-RPC connection.</summary>`);
    lines.push(`public static class ClientSessionApiRegistration`);
    lines.push(`{`);
    lines.push(`    /// <summary>`);
    lines.push(`    /// Registers handlers for server-to-client session API calls.`);
    lines.push(`    /// Each incoming call includes a <c>sessionId</c> in its params object,`);
    lines.push(`    /// which is used to resolve the session's handler group.`);
    lines.push(`    /// </summary>`);
    lines.push(`    public static void RegisterClientSessionApiHandlers(JsonRpc rpc, Func<string, ClientSessionApiHandlers> getHandlers)`);
    lines.push(`    {`);
    for (const { groupName, methods } of groups) {
        for (const method of methods) {
            const handlerProperty = toPascalCase(groupName);
            const handlerMethod = clientHandlerMethodName(method.rpcMethod);
            const effectiveParams = resolveMethodParamsSchema(method);
            const hasParams = !!effectiveParams?.properties && Object.keys(effectiveParams.properties).length > 0;
            const resultSchema = getMethodResultSchema(method);
            const paramsClass = paramsTypeName(method);
            const taskType = resultTaskType(method);
            const registrationVar = `register${typeToClassName(method.rpcMethod)}Method`;

            if (hasParams) {
                lines.push(`        var ${registrationVar} = (Func<${paramsClass}, CancellationToken, ${taskType}>)(async (request, cancellationToken) =>`);
                lines.push(`        {`);
                lines.push(`            var handler = getHandlers(request.SessionId).${handlerProperty};`);
                lines.push(`            if (handler is null) throw new InvalidOperationException($"No ${groupName} handler registered for session: {request.SessionId}");`);
                if (!isVoidSchema(resultSchema)) {
                    lines.push(`            return await handler.${handlerMethod}(request, cancellationToken);`);
                } else {
                    lines.push(`            await handler.${handlerMethod}(request, cancellationToken);`);
                }
                lines.push(`        });`);
                lines.push(`        rpc.AddLocalRpcMethod(${registrationVar}.Method, ${registrationVar}.Target!, new JsonRpcMethodAttribute("${method.rpcMethod}")`);
                lines.push(`        {`);
                lines.push(`            UseSingleObjectParameterDeserialization = true`);
                lines.push(`        });`);
            } else {
                lines.push(`        rpc.AddLocalRpcMethod("${method.rpcMethod}", (Func<CancellationToken, ${taskType}>)(_ =>`);
                lines.push(`            throw new InvalidOperationException("No params provided for ${method.rpcMethod}")));`);
            }
        }
    }
    lines.push(`    }`);
    lines.push(`}`);

    return lines;
}

function generateRpcCode(schema: ApiSchema): string {
    emittedRpcClassSchemas.clear();
    emittedRpcEnumResultTypes.clear();
    experimentalRpcTypes.clear();
    rpcKnownTypes.clear();
    rpcEnumOutput = [];
    generatedEnums.clear(); // Clear shared enum deduplication map
    rpcDefinitions = collectDefinitionCollections(schema as Record<string, unknown>);
    const classes: string[] = [];

    let serverRpcParts: string[] = [];
    if (schema.server) serverRpcParts = emitServerRpcClasses(schema.server, classes);

    let sessionRpcParts: string[] = [];
    if (schema.session) sessionRpcParts = emitSessionRpcClasses(schema.session, classes);

    let clientSessionParts: string[] = [];
    if (schema.clientSession) clientSessionParts = emitClientSessionApiRegistration(schema.clientSession, classes);

    const lines: string[] = [];
    lines.push(`${COPYRIGHT}

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

#pragma warning disable CS0612 // Type or member is obsolete
#pragma warning disable CS0618 // Type or member is obsolete (with message)

using System.ComponentModel.DataAnnotations;
using System.Diagnostics.CodeAnalysis;
using System.Text.Json;
using System.Text.Json.Serialization;
using StreamJsonRpc;

namespace GitHub.Copilot.SDK.Rpc;

/// <summary>Diagnostic IDs for the Copilot SDK.</summary>
internal static class Diagnostics
{
    /// <summary>Indicates an experimental API that may change or be removed.</summary>
    internal const string Experimental = "GHCP001";
}
`);

    for (const cls of classes) if (cls) lines.push(cls, "");
    for (const enumCode of rpcEnumOutput) lines.push(enumCode, "");
    for (const part of serverRpcParts) lines.push(part, "");
    for (const part of sessionRpcParts) lines.push(part, "");
    if (clientSessionParts.length > 0) lines.push(...clientSessionParts, "");

    // Add JsonSerializerContext for AOT/trimming support
    const typeNames = [...emittedRpcClassSchemas.keys(), ...emittedRpcEnumResultTypes].sort();
    if (typeNames.length > 0) {
        lines.push(`[JsonSourceGenerationOptions(`);
        lines.push(`    JsonSerializerDefaults.Web,`);
        lines.push(`    AllowOutOfOrderMetadataProperties = true,`);
        lines.push(`    DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull)]`);
        for (const t of typeNames) lines.push(`[JsonSerializable(typeof(${t}))]`);
        lines.push(`internal partial class RpcJsonContext : JsonSerializerContext;`);
    }

    return lines.join("\n");
}

export async function generateRpc(schemaPath?: string): Promise<void> {
    console.log("C#: generating RPC types...");
    const resolvedPath = schemaPath ?? (await getApiSchemaPath());
    const schema = fixNullableRequiredRefsInApiSchema(cloneSchemaForCodegen(JSON.parse(await fs.readFile(resolvedPath, "utf-8")) as ApiSchema));
    const code = generateRpcCode(schema);
    const outPath = await writeGeneratedFile("dotnet/src/Generated/Rpc.cs", code);
    console.log(`  ✓ ${outPath}`);
    await formatCSharpFile(outPath);
}

// ══════════════════════════════════════════════════════════════════════════════
// MAIN
// ══════════════════════════════════════════════════════════════════════════════

async function generate(sessionSchemaPath?: string, apiSchemaPath?: string): Promise<void> {
    await generateSessionEvents(sessionSchemaPath);
    try {
        await generateRpc(apiSchemaPath);
    } catch (err) {
        if ((err as NodeJS.ErrnoException).code === "ENOENT" && !apiSchemaPath) {
            console.log("C#: skipping RPC (api.schema.json not found)");
        } else {
            throw err;
        }
    }
}

const sessionArg = process.argv[2] || undefined;
const apiArg = process.argv[3] || undefined;
generate(sessionArg, apiArg).catch((err) => {
    console.error("C# generation failed:", err);
    process.exit(1);
});
