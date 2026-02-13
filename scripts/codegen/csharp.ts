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
    getSessionEventsSchemaPath,
    getApiSchemaPath,
    writeGeneratedFile,
    isRpcMethod,
    EXCLUDED_EVENT_TYPES,
    REPO_ROOT,
    type ApiSchema,
    type RpcMethod,
} from "./utils.js";

const execFileAsync = promisify(execFile);

// ── C# utilities ────────────────────────────────────────────────────────────

function toPascalCase(name: string): string {
    if (name.includes("_")) {
        return name.split("_").map((p) => p.charAt(0).toUpperCase() + p.slice(1)).join("");
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
    if (type === "number" || type === "integer") return required ? "double" : "double?";
    if (type === "boolean") return required ? "bool" : "bool?";
    if (type === "array") {
        const items = schema.items as JSONSchema7 | undefined;
        const itemType = items ? schemaTypeToCSharp(items, true, knownTypes) : "object";
        return required ? `${itemType}[]` : `${itemType}[]?`;
    }
    if (type === "object") {
        if (schema.additionalProperties && typeof schema.additionalProperties === "object") {
            const valueType = schemaTypeToCSharp(schema.additionalProperties as JSONSchema7, true, knownTypes);
            return required ? `Dictionary<string, ${valueType}>` : `Dictionary<string, ${valueType}>?`;
        }
        return required ? "object" : "object?";
    }
    return required ? "object" : "object?";
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
}

let generatedEnums = new Map<string, { enumName: string; values: string[] }>();

function getOrCreateEnum(parentClassName: string, propName: string, values: string[], enumOutput: string[]): string {
    const valuesKey = [...values].sort().join("|");
    for (const [, existing] of generatedEnums) {
        if ([...existing.values].sort().join("|") === valuesKey) return existing.enumName;
    }
    const enumName = `${parentClassName}${propName}`;
    generatedEnums.set(enumName, { enumName, values });

    const lines = [`[JsonConverter(typeof(JsonStringEnumConverter<${enumName}>))]`, `public enum ${enumName}`, `{`];
    for (const value of values) {
        lines.push(`    [JsonStringEnumMemberName("${value}")]`, `    ${toPascalCaseEnumMember(value)},`);
    }
    lines.push(`}`, "");
    enumOutput.push(lines.join("\n"));
    return enumName;
}

function extractEventVariants(schema: JSONSchema7): EventVariant[] {
    const sessionEvent = schema.definitions?.SessionEvent as JSONSchema7;
    if (!sessionEvent?.anyOf) throw new Error("Schema must have SessionEvent definition with anyOf");

    return sessionEvent.anyOf
        .map((variant) => {
            if (typeof variant !== "object" || !variant.properties) throw new Error("Invalid variant");
            const typeSchema = variant.properties.type as JSONSchema7;
            const typeName = typeSchema?.const as string;
            if (!typeName) throw new Error("Variant must have type.const");
            const baseName = typeToClassName(typeName);
            return {
                typeName,
                className: `${baseName}Event`,
                dataClassName: `${baseName}Data`,
                dataSchema: variant.properties.data as JSONSchema7,
            };
        })
        .filter((v) => !EXCLUDED_EVENT_TYPES.has(v.typeName));
}

/**
 * Find a discriminator property shared by all variants in an anyOf.
 */
function findDiscriminator(variants: JSONSchema7[]): { property: string; mapping: Map<string, JSONSchema7> } | null {
    if (variants.length === 0) return null;
    const firstVariant = variants[0];
    if (!firstVariant.properties) return null;

    for (const [propName, propSchema] of Object.entries(firstVariant.properties)) {
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
    enumOutput: string[]
): string {
    const lines: string[] = [];
    const discriminatorInfo = findDiscriminator(variants)!;

    lines.push(`[JsonPolymorphic(`);
    lines.push(`    TypeDiscriminatorPropertyName = "${discriminatorProperty}",`);
    lines.push(`    UnknownDerivedTypeHandling = JsonUnknownDerivedTypeHandling.FallBackToBaseType)]`);

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

    for (const [constValue, variant] of discriminatorInfo.mapping) {
        const derivedClassName = `${baseClassName}${toPascalCase(constValue)}`;
        const derivedCode = generateDerivedClass(derivedClassName, baseClassName, discriminatorProperty, constValue, variant, knownTypes, nestedClasses, enumOutput);
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
    lines.push(`    [JsonIgnore]`);
    lines.push(`    public override string ${toPascalCase(discriminatorProperty)} => "${discriminatorValue}";`);
    lines.push("");

    if (schema.properties) {
        for (const [propName, propSchema] of Object.entries(schema.properties)) {
            if (typeof propSchema !== "object") continue;
            if (propName === discriminatorProperty) continue;

            const isReq = required.has(propName);
            const csharpName = toPascalCase(propName);
            const csharpType = resolveSessionPropertyType(propSchema as JSONSchema7, className, csharpName, isReq, knownTypes, nestedClasses, enumOutput);

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
    const lines = [`public partial class ${className}`, `{`];

    for (const [propName, propSchema] of Object.entries(schema.properties || {})) {
        if (typeof propSchema !== "object") continue;
        const prop = propSchema as JSONSchema7;
        const isReq = required.has(propName);
        const csharpName = toPascalCase(propName);
        const csharpType = resolveSessionPropertyType(prop, className, csharpName, isReq, knownTypes, nestedClasses, enumOutput);

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
    if (propSchema.anyOf) {
        const hasNull = propSchema.anyOf.some((s) => typeof s === "object" && (s as JSONSchema7).type === "null");
        const nonNull = propSchema.anyOf.filter((s) => typeof s === "object" && (s as JSONSchema7).type !== "null");
        if (nonNull.length === 1) {
            return resolveSessionPropertyType(nonNull[0] as JSONSchema7, parentClassName, propName, isRequired && !hasNull, knownTypes, nestedClasses, enumOutput);
        }
        return hasNull || !isRequired ? "object?" : "object";
    }
    if (propSchema.enum && Array.isArray(propSchema.enum)) {
        const enumName = getOrCreateEnum(parentClassName, propName, propSchema.enum as string[], enumOutput);
        return isRequired ? enumName : `${enumName}?`;
    }
    if (propSchema.type === "object" && propSchema.properties) {
        const nestedClassName = `${parentClassName}${propName}`;
        nestedClasses.set(nestedClassName, generateNestedClass(nestedClassName, propSchema, knownTypes, nestedClasses, enumOutput));
        return isRequired ? nestedClassName : `${nestedClassName}?`;
    }
    if (propSchema.type === "array" && propSchema.items) {
        const items = propSchema.items as JSONSchema7;
        // Array of discriminated union (anyOf with shared discriminator)
        if (items.anyOf && Array.isArray(items.anyOf)) {
            const variants = items.anyOf.filter((v): v is JSONSchema7 => typeof v === "object");
            const discriminatorInfo = findDiscriminator(variants);
            if (discriminatorInfo) {
                const baseClassName = `${parentClassName}${propName}Item`;
                const polymorphicCode = generatePolymorphicClasses(baseClassName, discriminatorInfo.property, variants, knownTypes, nestedClasses, enumOutput);
                nestedClasses.set(baseClassName, polymorphicCode);
                return isRequired ? `${baseClassName}[]` : `${baseClassName}[]?`;
            }
        }
        if (items.type === "object" && items.properties) {
            const itemClassName = `${parentClassName}${propName}Item`;
            nestedClasses.set(itemClassName, generateNestedClass(itemClassName, items, knownTypes, nestedClasses, enumOutput));
            return isRequired ? `${itemClassName}[]` : `${itemClassName}[]?`;
        }
        if (items.enum && Array.isArray(items.enum)) {
            const enumName = getOrCreateEnum(parentClassName, `${propName}Item`, items.enum as string[], enumOutput);
            return isRequired ? `${enumName}[]` : `${enumName}[]?`;
        }
        const itemType = schemaTypeToCSharp(items, true, knownTypes);
        return isRequired ? `${itemType}[]` : `${itemType}[]?`;
    }
    return schemaTypeToCSharp(propSchema, isRequired, knownTypes);
}

function generateDataClass(variant: EventVariant, knownTypes: Map<string, string>, nestedClasses: Map<string, string>, enumOutput: string[]): string {
    if (!variant.dataSchema?.properties) return `public partial class ${variant.dataClassName} { }`;

    const required = new Set(variant.dataSchema.required || []);
    const lines = [`public partial class ${variant.dataClassName}`, `{`];

    for (const [propName, propSchema] of Object.entries(variant.dataSchema.properties)) {
        if (typeof propSchema !== "object") continue;
        const isReq = required.has(propName);
        const csharpName = toPascalCase(propName);
        const csharpType = resolveSessionPropertyType(propSchema as JSONSchema7, variant.dataClassName, csharpName, isReq, knownTypes, nestedClasses, enumOutput);

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
    const variants = extractEventVariants(schema);
    const knownTypes = new Map<string, string>();
    const nestedClasses = new Map<string, string>();
    const enumOutput: string[] = [];

    const lines: string[] = [];
    lines.push(`${COPYRIGHT}

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

using System.Text.Json;
using System.Text.Json.Serialization;

namespace GitHub.Copilot.SDK;
`);

    // Base class with XML doc
    lines.push(`/// <summary>`);
    lines.push(`/// Base class for all session events with polymorphic JSON serialization.`);
    lines.push(`/// </summary>`);
    lines.push(`[JsonPolymorphic(`, `    TypeDiscriminatorPropertyName = "type",`, `    UnknownDerivedTypeHandling = JsonUnknownDerivedTypeHandling.FailSerialization)]`);
    for (const variant of [...variants].sort((a, b) => a.typeName.localeCompare(b.typeName))) {
        lines.push(`[JsonDerivedType(typeof(${variant.className}), "${variant.typeName}")]`);
    }
    lines.push(`public abstract partial class SessionEvent`, `{`, `    [JsonPropertyName("id")]`, `    public Guid Id { get; set; }`, "");
    lines.push(`    [JsonPropertyName("timestamp")]`, `    public DateTimeOffset Timestamp { get; set; }`, "");
    lines.push(`    [JsonPropertyName("parentId")]`, `    public Guid? ParentId { get; set; }`, "");
    lines.push(`    [JsonIgnore(Condition = JsonIgnoreCondition.WhenWritingNull)]`, `    [JsonPropertyName("ephemeral")]`, `    public bool? Ephemeral { get; set; }`, "");
    lines.push(`    /// <summary>`, `    /// The event type discriminator.`, `    /// </summary>`);
    lines.push(`    [JsonIgnore]`, `    public abstract string Type { get; }`, "");
    lines.push(`    public static SessionEvent FromJson(string json) =>`, `        JsonSerializer.Deserialize(json, SessionEventsJsonContext.Default.SessionEvent)!;`, "");
    lines.push(`    public string ToJson() =>`, `        JsonSerializer.Serialize(this, SessionEventsJsonContext.Default.SessionEvent);`, `}`, "");

    // Event classes with XML docs
    for (const variant of variants) {
        lines.push(`/// <summary>`, `/// Event: ${variant.typeName}`, `/// </summary>`);
        lines.push(`public partial class ${variant.className} : SessionEvent`, `{`, `    [JsonIgnore]`, `    public override string Type => "${variant.typeName}";`, "");
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
    lines.push(`internal partial class SessionEventsJsonContext : JsonSerializerContext;`);

    return lines.join("\n");
}

export async function generateSessionEvents(schemaPath?: string): Promise<void> {
    console.log("C#: generating session-events...");
    const resolvedPath = schemaPath ?? (await getSessionEventsSchemaPath());
    const schema = JSON.parse(await fs.readFile(resolvedPath, "utf-8")) as JSONSchema7;
    const code = generateSessionEventsCode(schema);
    const outPath = await writeGeneratedFile("dotnet/src/Generated/SessionEvents.cs", code);
    console.log(`  ✓ ${outPath}`);
    await formatCSharpFile(outPath);
}

// ══════════════════════════════════════════════════════════════════════════════
// RPC TYPES
// ══════════════════════════════════════════════════════════════════════════════

let emittedRpcClasses = new Set<string>();
let rpcKnownTypes = new Map<string, string>();

function singularPascal(s: string): string {
    const p = toPascalCase(s);
    return p.endsWith("s") ? p.slice(0, -1) : p;
}

function resolveRpcType(schema: JSONSchema7, isRequired: boolean, parentClassName: string, propName: string, classes: string[]): string {
    if (schema.type === "object" && schema.properties) {
        const className = `${parentClassName}${propName}`;
        classes.push(emitRpcClass(className, schema, "public", classes));
        return isRequired ? className : `${className}?`;
    }
    if (schema.type === "array" && schema.items) {
        const items = schema.items as JSONSchema7;
        if (items.type === "object" && items.properties) {
            const itemClass = singularPascal(propName);
            if (!emittedRpcClasses.has(itemClass)) classes.push(emitRpcClass(itemClass, items, "public", classes));
            return isRequired ? `List<${itemClass}>` : `List<${itemClass}>?`;
        }
        const itemType = schemaTypeToCSharp(items, true, rpcKnownTypes);
        return isRequired ? `List<${itemType}>` : `List<${itemType}>?`;
    }
    if (schema.type === "object" && schema.additionalProperties && typeof schema.additionalProperties === "object") {
        const vs = schema.additionalProperties as JSONSchema7;
        if (vs.type === "object" && vs.properties) {
            const valClass = `${parentClassName}${propName}Value`;
            classes.push(emitRpcClass(valClass, vs, "public", classes));
            return isRequired ? `Dictionary<string, ${valClass}>` : `Dictionary<string, ${valClass}>?`;
        }
        const valueType = schemaTypeToCSharp(vs, true, rpcKnownTypes);
        return isRequired ? `Dictionary<string, ${valueType}>` : `Dictionary<string, ${valueType}>?`;
    }
    return schemaTypeToCSharp(schema, isRequired, rpcKnownTypes);
}

function emitRpcClass(className: string, schema: JSONSchema7, visibility: "public" | "internal", extraClasses: string[]): string {
    if (emittedRpcClasses.has(className)) return "";
    emittedRpcClasses.add(className);

    const requiredSet = new Set(schema.required || []);
    const lines: string[] = [];
    if (schema.description) lines.push(`/// <summary>${schema.description}</summary>`);
    lines.push(`${visibility} class ${className}`, `{`);

    const props = Object.entries(schema.properties || {});
    for (let i = 0; i < props.length; i++) {
        const [propName, propSchema] = props[i];
        if (typeof propSchema !== "object") continue;
        const prop = propSchema as JSONSchema7;
        const isReq = requiredSet.has(propName);
        const csharpName = toPascalCase(propName);
        const csharpType = resolveRpcType(prop, isReq, className, csharpName, extraClasses);

        if (prop.description && visibility === "public") lines.push(`    /// <summary>${prop.description}</summary>`);
        lines.push(`    [JsonPropertyName("${propName}")]`);

        let defaultVal = "";
        if (isReq && !csharpType.endsWith("?")) {
            if (csharpType === "string") defaultVal = " = string.Empty;";
            else if (csharpType.startsWith("List<") || csharpType.startsWith("Dictionary<") || emittedRpcClasses.has(csharpType)) defaultVal = " = new();";
        }
        lines.push(`    public ${csharpType} ${csharpName} { get; set; }${defaultVal}`);
        if (i < props.length - 1) lines.push("");
    }
    lines.push(`}`);
    return lines.join("\n");
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
    srLines.push(`/// <summary>Typed server-scoped RPC methods (no session required).</summary>`);
    srLines.push(`public class ServerRpc`);
    srLines.push(`{`);
    srLines.push(`    private readonly JsonRpc _rpc;`);
    srLines.push("");
    srLines.push(`    internal ServerRpc(JsonRpc rpc)`);
    srLines.push(`    {`);
    srLines.push(`        _rpc = rpc;`);
    for (const [groupName] of groups) {
        srLines.push(`        ${toPascalCase(groupName)} = new ${toPascalCase(groupName)}Api(rpc);`);
    }
    srLines.push(`    }`);

    // Top-level methods (like ping)
    for (const [key, value] of topLevelMethods) {
        if (!isRpcMethod(value)) continue;
        emitServerInstanceMethod(key, value, srLines, classes, "    ");
    }

    // Group properties
    for (const [groupName] of groups) {
        srLines.push("");
        srLines.push(`    /// <summary>${toPascalCase(groupName)} APIs.</summary>`);
        srLines.push(`    public ${toPascalCase(groupName)}Api ${toPascalCase(groupName)} { get; }`);
    }

    srLines.push(`}`);
    result.push(srLines.join("\n"));

    // Per-group API classes
    for (const [groupName, groupNode] of groups) {
        result.push(emitServerApiClass(`${toPascalCase(groupName)}Api`, groupNode as Record<string, unknown>, classes));
    }

    return result;
}

function emitServerApiClass(className: string, node: Record<string, unknown>, classes: string[]): string {
    const lines: string[] = [];
    lines.push(`/// <summary>Server-scoped ${className.replace("Api", "")} APIs.</summary>`);
    lines.push(`public class ${className}`);
    lines.push(`{`);
    lines.push(`    private readonly JsonRpc _rpc;`);
    lines.push("");
    lines.push(`    internal ${className}(JsonRpc rpc)`);
    lines.push(`    {`);
    lines.push(`        _rpc = rpc;`);
    lines.push(`    }`);

    for (const [key, value] of Object.entries(node)) {
        if (!isRpcMethod(value)) continue;
        emitServerInstanceMethod(key, value, lines, classes, "    ");
    }

    lines.push(`}`);
    return lines.join("\n");
}

function emitServerInstanceMethod(
    name: string,
    method: { rpcMethod: string; params: JSONSchema7 | null; result: JSONSchema7 },
    lines: string[],
    classes: string[],
    indent: string
): void {
    const methodName = toPascalCase(name);
    const resultClassName = `${typeToClassName(method.rpcMethod)}Result`;
    const resultClass = emitRpcClass(resultClassName, method.result, "public", classes);
    if (resultClass) classes.push(resultClass);

    const paramEntries = method.params?.properties ? Object.entries(method.params.properties) : [];
    const requiredSet = new Set(method.params?.required || []);

    let requestClassName: string | null = null;
    if (paramEntries.length > 0) {
        requestClassName = `${methodName}Request`;
        const reqClass = emitRpcClass(requestClassName, method.params!, "internal", classes);
        if (reqClass) classes.push(reqClass);
    }

    lines.push("");
    lines.push(`${indent}/// <summary>Calls "${method.rpcMethod}".</summary>`);

    const sigParams: string[] = [];
    const bodyAssignments: string[] = [];

    for (const [pName, pSchema] of paramEntries) {
        if (typeof pSchema !== "object") continue;
        const isReq = requiredSet.has(pName);
        const csType = schemaTypeToCSharp(pSchema as JSONSchema7, isReq, rpcKnownTypes);
        sigParams.push(`${csType} ${pName}${isReq ? "" : " = null"}`);
        bodyAssignments.push(`${toPascalCase(pName)} = ${pName}`);
    }
    sigParams.push("CancellationToken cancellationToken = default");

    lines.push(`${indent}public async Task<${resultClassName}> ${methodName}Async(${sigParams.join(", ")})`);
    lines.push(`${indent}{`);
    if (requestClassName && bodyAssignments.length > 0) {
        lines.push(`${indent}    var request = new ${requestClassName} { ${bodyAssignments.join(", ")} };`);
        lines.push(`${indent}    return await CopilotClient.InvokeRpcAsync<${resultClassName}>(_rpc, "${method.rpcMethod}", [request], cancellationToken);`);
    } else {
        lines.push(`${indent}    return await CopilotClient.InvokeRpcAsync<${resultClassName}>(_rpc, "${method.rpcMethod}", [], cancellationToken);`);
    }
    lines.push(`${indent}}`);
}

function emitSessionRpcClasses(node: Record<string, unknown>, classes: string[]): string[] {
    const result: string[] = [];
    const groups = Object.entries(node).filter(([, v]) => typeof v === "object" && v !== null && !isRpcMethod(v));

    const srLines = [`/// <summary>Typed session-scoped RPC methods.</summary>`, `public class SessionRpc`, `{`, `    private readonly JsonRpc _rpc;`, `    private readonly string _sessionId;`, ""];
    srLines.push(`    internal SessionRpc(JsonRpc rpc, string sessionId)`, `    {`, `        _rpc = rpc;`, `        _sessionId = sessionId;`);
    for (const [groupName] of groups) srLines.push(`        ${toPascalCase(groupName)} = new ${toPascalCase(groupName)}Api(rpc, sessionId);`);
    srLines.push(`    }`);
    for (const [groupName] of groups) srLines.push("", `    public ${toPascalCase(groupName)}Api ${toPascalCase(groupName)} { get; }`);
    srLines.push(`}`);
    result.push(srLines.join("\n"));

    for (const [groupName, groupNode] of groups) {
        result.push(emitSessionApiClass(`${toPascalCase(groupName)}Api`, groupNode as Record<string, unknown>, classes));
    }
    return result;
}

function emitSessionApiClass(className: string, node: Record<string, unknown>, classes: string[]): string {
    const lines = [`public class ${className}`, `{`, `    private readonly JsonRpc _rpc;`, `    private readonly string _sessionId;`, ""];
    lines.push(`    internal ${className}(JsonRpc rpc, string sessionId)`, `    {`, `        _rpc = rpc;`, `        _sessionId = sessionId;`, `    }`);

    for (const [key, value] of Object.entries(node)) {
        if (!isRpcMethod(value)) continue;
        const method = value;
        const methodName = toPascalCase(key);
        const resultClassName = `${typeToClassName(method.rpcMethod)}Result`;
        const resultClass = emitRpcClass(resultClassName, method.result, "public", classes);
        if (resultClass) classes.push(resultClass);

        const paramEntries = (method.params?.properties ? Object.entries(method.params.properties) : []).filter(([k]) => k !== "sessionId");
        const requiredSet = new Set(method.params?.required || []);

        const requestClassName = `${methodName}Request`;
        if (method.params) {
            const reqClass = emitRpcClass(requestClassName, method.params, "internal", classes);
            if (reqClass) classes.push(reqClass);
        }

        lines.push("", `    /// <summary>Calls "${method.rpcMethod}".</summary>`);
        const sigParams: string[] = [];
        const bodyAssignments = [`SessionId = _sessionId`];

        for (const [pName, pSchema] of paramEntries) {
            if (typeof pSchema !== "object") continue;
            const csType = schemaTypeToCSharp(pSchema as JSONSchema7, requiredSet.has(pName), rpcKnownTypes);
            sigParams.push(`${csType} ${pName}`);
            bodyAssignments.push(`${toPascalCase(pName)} = ${pName}`);
        }
        sigParams.push("CancellationToken cancellationToken = default");

        lines.push(`    public async Task<${resultClassName}> ${methodName}Async(${sigParams.join(", ")})`);
        lines.push(`    {`, `        var request = new ${requestClassName} { ${bodyAssignments.join(", ")} };`);
        lines.push(`        return await CopilotClient.InvokeRpcAsync<${resultClassName}>(_rpc, "${method.rpcMethod}", [request], cancellationToken);`, `    }`);
    }
    lines.push(`}`);
    return lines.join("\n");
}

function generateRpcCode(schema: ApiSchema): string {
    emittedRpcClasses.clear();
    rpcKnownTypes.clear();
    const classes: string[] = [];

    let serverRpcParts: string[] = [];
    if (schema.server) serverRpcParts = emitServerRpcClasses(schema.server, classes);

    let sessionRpcParts: string[] = [];
    if (schema.session) sessionRpcParts = emitSessionRpcClasses(schema.session, classes);

    const lines: string[] = [];
    lines.push(`${COPYRIGHT}

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

using System.Text.Json;
using System.Text.Json.Serialization;
using StreamJsonRpc;

namespace GitHub.Copilot.SDK.Rpc;
`);

    for (const cls of classes) if (cls) lines.push(cls, "");
    for (const part of serverRpcParts) lines.push(part, "");
    for (const part of sessionRpcParts) lines.push(part, "");

    // Add JsonSerializerContext for AOT/trimming support
    const typeNames = [...emittedRpcClasses].sort();
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
    const schema = JSON.parse(await fs.readFile(resolvedPath, "utf-8")) as ApiSchema;
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
