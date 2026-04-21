/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Go code generator for session-events and RPC types.
 */

import { execFile } from "child_process";
import fs from "fs/promises";
import type { JSONSchema7 } from "json-schema";
import { FetchingJSONSchemaStore, InputData, JSONSchemaInput, quicktype } from "quicktype-core";
import { promisify } from "util";
import {
    cloneSchemaForCodegen,
    fixNullableRequiredRefsInApiSchema,
    getApiSchemaPath,
    getRpcSchemaTypeName,
    getSessionEventsSchemaPath,
    hasSchemaPayload,
    isNodeFullyExperimental,
    isNodeFullyDeprecated,
    isSchemaDeprecated,
    isVoidSchema,
    getNullableInner,
    isRpcMethod,
    postProcessSchema,
    writeGeneratedFile,
    collectDefinitionCollections,
    resolveObjectSchema,
    resolveSchema,
    withSharedDefinitions,
    refTypeName,
    resolveRef,
    type ApiSchema,
    type DefinitionCollections,
    type RpcMethod,
} from "./utils.js";

const execFileAsync = promisify(execFile);

// ── Utilities ───────────────────────────────────────────────────────────────

// Go initialisms that should be all-caps
const goInitialisms = new Set(["id", "ui", "uri", "url", "api", "http", "https", "json", "xml", "html", "css", "sql", "ssh", "tcp", "udp", "ip", "rpc", "mime"]);

function toPascalCase(s: string): string {
    return s
        .split(/[._]/)
        .map((w) => goInitialisms.has(w.toLowerCase()) ? w.toUpperCase() : w.charAt(0).toUpperCase() + w.slice(1))
        .join("");
}

function toGoFieldName(jsonName: string): string {
    // Handle camelCase field names like "modelId" -> "ModelID"
    return jsonName
        .replace(/([a-z])([A-Z])/g, "$1_$2")
        .split("_")
        .map((w) => goInitialisms.has(w.toLowerCase()) ? w.toUpperCase() : w.charAt(0).toUpperCase() + w.slice(1).toLowerCase())
        .join("");
}

/**
 * Post-process Go enum constants so every constant follows the canonical
 * Go `TypeNameValue` convention.  quicktype disambiguates collisions with
 * whimsical prefixes (Purple, Fluffy, …) that we replace.
 */
function postProcessEnumConstants(code: string): string {
    const renames = new Map<string, string>();

    // Match constant declarations inside const ( … ) blocks.
    const constLineRe = /^\s+(\w+)\s+(\w+)\s*=\s*"([^"]+)"/gm;
    let m;
    while ((m = constLineRe.exec(code)) !== null) {
        const [, constName, typeName, value] = m;
        if (constName.startsWith(typeName)) continue;

        // Use the same initialism logic as toPascalCase so "url" → "URL", "mcp" → "MCP", etc.
        const valuePascal = value
            .split(/[._-]/)
            .map((w) => goInitialisms.has(w.toLowerCase()) ? w.toUpperCase() : w.charAt(0).toUpperCase() + w.slice(1))
            .join("");
        const desired = typeName + valuePascal;
        if (constName !== desired) {
            renames.set(constName, desired);
        }
    }

    // Replace each const block in place, then fix switch-case references
    // in marshal/unmarshal functions. This avoids renaming struct fields.

    // Phase 1: Rename inside const ( … ) blocks
    code = code.replace(/^(const \([\s\S]*?\n\))/gm, (block) => {
        let b = block;
        for (const [oldName, newName] of renames) {
            b = b.replace(new RegExp(`\\b${oldName}\\b`, "g"), newName);
        }
        return b;
    });

    // Phase 2: Rename inside func bodies (marshal/unmarshal helpers use case statements)
    code = code.replace(/^(func \([\s\S]*?\n\})/gm, (funcBlock) => {
        let b = funcBlock;
        for (const [oldName, newName] of renames) {
            b = b.replace(new RegExp(`\\b${oldName}\\b`, "g"), newName);
        }
        return b;
    });

    return code;
}

function collapsePlaceholderGoStructs(code: string, knownDefinitionNames?: Set<string>): string {
    const structBlockRe = /((?:\/\/.*\r?\n)*)type\s+(\w+)\s+struct\s*\{[\s\S]*?^\}/gm;
    const matches = [...code.matchAll(structBlockRe)].map((match) => ({
        fullBlock: match[0],
        name: match[2],
        normalizedBody: normalizeGoStructBlock(match[0], match[2]),
    }));
    const groups = new Map<string, typeof matches>();

    for (const match of matches) {
        const group = groups.get(match.normalizedBody) ?? [];
        group.push(match);
        groups.set(match.normalizedBody, group);
    }

    for (const group of groups.values()) {
        if (group.length < 2) continue;

        const canonical = chooseCanonicalPlaceholderDuplicate(group.map(({ name }) => name), knownDefinitionNames);
        if (!canonical) continue;

        for (const duplicate of group) {
            if (duplicate.name === canonical) continue;
            // Only collapse types that quicktype invented (Class suffix or not
            // in the schema's named definitions). Preserve intentionally-named types.
            if (!isPlaceholderTypeName(duplicate.name) && knownDefinitionNames?.has(duplicate.name.toLowerCase())) continue;

            code = code.replace(duplicate.fullBlock, "");
            code = code.replace(new RegExp(`\\b${duplicate.name}\\b`, "g"), canonical);
        }
    }

    return code.replace(/\n{3,}/g, "\n\n");
}

function normalizeGoStructBlock(block: string, name: string): string {
    return block
        .replace(/^\s*\/\/.*\r?\n/gm, "")
        .replace(new RegExp(`^type\\s+${name}\\s+struct\\s*\\{`, "m"), "type struct {")
        .split(/\r?\n/)
        .map((line) => line.trim())
        .filter((line) => line.length > 0)
        .join("\n");
}

function chooseCanonicalPlaceholderDuplicate(names: string[], knownDefinitionNames?: Set<string>): string | undefined {
    // Prefer the name that matches a schema definition — it's intentionally named.
    if (knownDefinitionNames) {
        const definedName = names.find((name) => knownDefinitionNames.has(name.toLowerCase()));
        if (definedName) return definedName;
    }
    // Fallback for Class-suffix placeholders: pick the non-placeholder name.
    const specificNames = names.filter((name) => !isPlaceholderTypeName(name));
    if (specificNames.length === 0) return undefined;
    return specificNames[0];
}

function isPlaceholderTypeName(name: string): boolean {
    return name.endsWith("Class");
}

/**
 * Extract a mapping from (structName, jsonFieldName) → goFieldName
 * so the wrapper code references the actual quicktype-generated field names.
 */
function extractFieldNames(qtCode: string): Map<string, Map<string, string>> {
    const result = new Map<string, Map<string, string>>();
    const structRe = /^type\s+(\w+)\s+struct\s*\{([^}]*)\}/gm;
    let sm;
    while ((sm = structRe.exec(qtCode)) !== null) {
        const [, structName, body] = sm;
        const fields = new Map<string, string>();
        const fieldRe = /^\s+(\w+)\s+[^`\n]+`json:"([^",]+)/gm;
        let fm;
        while ((fm = fieldRe.exec(body)) !== null) {
            fields.set(fm[2], fm[1]);
        }
        result.set(structName, fields);
    }
    return result;
}

function extractQuicktypeImports(qtCode: string): { code: string; imports: string[] } {
    const collectedImports: string[] = [];
    let code = qtCode.replace(/^import \(\n([\s\S]*?)^\)\n+/m, (_match, block: string) => {
        for (const line of block.split(/\r?\n/)) {
            const trimmed = line.trim();
            if (trimmed.length > 0) {
                collectedImports.push(trimmed);
            }
        }
        return "";
    });

    code = code.replace(/^import ("[^"]+")\n+/m, (_match, singleImport: string) => {
        collectedImports.push(singleImport.trim());
        return "";
    });

    return { code, imports: collectedImports };
}

async function formatGoFile(filePath: string): Promise<void> {
    try {
        await execFileAsync("go", ["fmt", filePath]);
        console.log(`  ✓ Formatted with go fmt`);
    } catch {
        // go fmt not available, skip
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

let rpcDefinitions: DefinitionCollections = { definitions: {}, $defs: {} };

function withRootTitle(schema: JSONSchema7, title: string): JSONSchema7 {
    return { ...schema, title };
}

function goRequestFallbackName(method: RpcMethod): string {
    return toPascalCase(method.rpcMethod) + "Request";
}

function schemaSourceForNamedDefinition(
    schema: JSONSchema7 | null | undefined,
    resolvedSchema: JSONSchema7 | undefined
): JSONSchema7 {
    if (schema?.$ref && resolvedSchema) {
        return resolvedSchema;
    }
    return schema ?? resolvedSchema ?? { type: "object" };
}

function isNamedGoObjectSchema(schema: JSONSchema7 | undefined): schema is JSONSchema7 {
    return !!schema && schema.type === "object" && (schema.properties !== undefined || schema.additionalProperties === false);
}

function getMethodResultSchema(method: RpcMethod): JSONSchema7 | undefined {
    return resolveSchema(method.result, rpcDefinitions) ?? method.result ?? undefined;
}

function getMethodParamsSchema(method: RpcMethod): JSONSchema7 | undefined {
    return (
        resolveObjectSchema(method.params, rpcDefinitions) ??
        resolveSchema(method.params, rpcDefinitions) ??
        method.params ??
        undefined
    );
}

function goResultTypeName(method: RpcMethod): string {
    return getRpcSchemaTypeName(getMethodResultSchema(method), toPascalCase(method.rpcMethod) + "Result");
}

function goNullableResultTypeName(method: RpcMethod, innerSchema: JSONSchema7): string {
    if (innerSchema.$ref) {
        const refName = innerSchema.$ref.split("/").pop();
        if (refName) return toPascalCase(refName);
    }
    return getRpcSchemaTypeName(innerSchema, toPascalCase(method.rpcMethod) + "Result");
}

function goParamsTypeName(method: RpcMethod): string {
    const fallback = goRequestFallbackName(method);
    if (method.rpcMethod.startsWith("session.") && method.params?.$ref) {
        return fallback;
    }
    return getRpcSchemaTypeName(getMethodParamsSchema(method), fallback);
}

// ── Session Events (custom codegen — per-event-type data structs) ───────────

interface GoEventVariant {
    typeName: string;
    dataClassName: string;
    dataSchema: JSONSchema7;
    dataDescription?: string;
}

interface GoCodegenCtx {
    structs: string[];
    enums: string[];
    enumsByName: Map<string, string>; // enumName → enumName (dedup by type name, not values)
    generatedNames: Set<string>;
    definitions?: DefinitionCollections;
}

function extractGoEventVariants(schema: JSONSchema7): GoEventVariant[] {
    const definitionCollections = collectDefinitionCollections(schema as Record<string, unknown>);
    const sessionEvent =
        resolveSchema({ $ref: "#/definitions/SessionEvent" }, definitionCollections) ??
        resolveSchema({ $ref: "#/$defs/SessionEvent" }, definitionCollections);
    if (!sessionEvent?.anyOf) throw new Error("Schema must have SessionEvent definition with anyOf");

    return (sessionEvent.anyOf as JSONSchema7[])
        .map((variant) => {
            const resolvedVariant =
                resolveObjectSchema(variant as JSONSchema7, definitionCollections) ??
                resolveSchema(variant as JSONSchema7, definitionCollections) ??
                (variant as JSONSchema7);
            if (typeof resolvedVariant !== "object" || !resolvedVariant.properties) throw new Error("Invalid variant");
            const typeSchema = resolvedVariant.properties.type as JSONSchema7;
            const typeName = typeSchema?.const as string;
            if (!typeName) throw new Error("Variant must have type.const");
            const dataSchema =
                resolveObjectSchema(resolvedVariant.properties.data as JSONSchema7, definitionCollections) ??
                resolveSchema(resolvedVariant.properties.data as JSONSchema7, definitionCollections) ??
                ((resolvedVariant.properties.data as JSONSchema7) || {});
            return {
                typeName,
                dataClassName: `${toPascalCase(typeName)}Data`,
                dataSchema,
                dataDescription: dataSchema.description,
            };
        });
}

/**
 * Find a const-valued discriminator property shared by all anyOf variants.
 */
function findGoDiscriminator(
    variants: JSONSchema7[]
): { property: string; mapping: Map<string, JSONSchema7> } | null {
    if (variants.length === 0) return null;
    const firstVariant = variants[0];
    if (!firstVariant.properties) return null;

    for (const [propName, propSchema] of Object.entries(firstVariant.properties)) {
        if (typeof propSchema !== "object") continue;
        if ((propSchema as JSONSchema7).const === undefined) continue;

        const mapping = new Map<string, JSONSchema7>();
        let valid = true;
        for (const variant of variants) {
            if (!variant.properties) { valid = false; break; }
            const vp = variant.properties[propName];
            if (typeof vp !== "object" || (vp as JSONSchema7).const === undefined) { valid = false; break; }
            mapping.set(String((vp as JSONSchema7).const), variant);
        }
        if (valid && mapping.size === variants.length) {
            return { property: propName, mapping };
        }
    }
    return null;
}

/**
 * Get or create a Go enum type, deduplicating by type name (not by value set).
 * Two enums with the same values but different names are distinct types.
 */
function getOrCreateGoEnum(
    enumName: string,
    values: string[],
    ctx: GoCodegenCtx,
    description?: string,
    deprecated?: boolean
): string {
    const existing = ctx.enumsByName.get(enumName);
    if (existing) return existing;

    const lines: string[] = [];
    if (description) {
        for (const line of description.split(/\r?\n/)) {
            lines.push(`// ${line}`);
        }
    }
    if (deprecated) {
        lines.push(`// Deprecated: ${enumName} is deprecated and will be removed in a future version.`);
    }
    lines.push(`type ${enumName} string`);
    lines.push(``);
    lines.push(`const (`);
    for (const value of values) {
        const constSuffix = value
            .split(/[-_.]/)
            .map((w) =>
                goInitialisms.has(w.toLowerCase())
                    ? w.toUpperCase()
                    : w.charAt(0).toUpperCase() + w.slice(1)
            )
            .join("");
        lines.push(`\t${enumName}${constSuffix} ${enumName} = "${value}"`);
    }
    lines.push(`)`);

    ctx.enumsByName.set(enumName, enumName);
    ctx.enums.push(lines.join("\n"));
    return enumName;
}

/**
 * Resolve a JSON Schema property to a Go type string.
 * Emits nested struct/enum definitions into ctx as a side effect.
 */
function resolveGoPropertyType(
    propSchema: JSONSchema7,
    parentTypeName: string,
    jsonPropName: string,
    isRequired: boolean,
    ctx: GoCodegenCtx
): string {
    const nestedName = parentTypeName + toGoFieldName(jsonPropName);

    // Handle $ref — resolve the reference and generate the referenced type
    if (propSchema.$ref && typeof propSchema.$ref === "string") {
        const typeName = toGoFieldName(refTypeName(propSchema.$ref, ctx.definitions));
        const resolved = resolveRef(propSchema.$ref, ctx.definitions);
        if (resolved) {
            if (resolved.enum) {
                const enumType = getOrCreateGoEnum(typeName, resolved.enum as string[], ctx, resolved.description, isSchemaDeprecated(resolved));
                return isRequired ? enumType : `*${enumType}`;
            }
            if (isNamedGoObjectSchema(resolved)) {
                emitGoStruct(typeName, resolved, ctx);
                return isRequired ? typeName : `*${typeName}`;
            }
            return resolveGoPropertyType(resolved, parentTypeName, jsonPropName, isRequired, ctx);
        }
        // Fallback: use the type name directly
        return isRequired ? typeName : `*${typeName}`;
    }

    // Handle anyOf
    if (propSchema.anyOf) {
        const nullableInnerSchema = getNullableInner(propSchema);
        if (nullableInnerSchema) {
            // anyOf [T, null/{not:{}}] → nullable T
            const innerType = resolveGoPropertyType(nullableInnerSchema, parentTypeName, jsonPropName, true, ctx);
            if (isRequired) return innerType;
            // Pointer-wrap if not already a pointer, slice, or map
            if (innerType.startsWith("*") || innerType.startsWith("[]") || innerType.startsWith("map[")) {
                return innerType;
            }
            return `*${innerType}`;
        }
        const nonNull = (propSchema.anyOf as JSONSchema7[]).filter((s) => s.type !== "null");
        const hasNull = (propSchema.anyOf as JSONSchema7[]).some((s) => s.type === "null");

        if (nonNull.length === 1) {
            // anyOf [T, null] → nullable T
            const innerType = resolveGoPropertyType(nonNull[0], parentTypeName, jsonPropName, true, ctx);
            if (isRequired && !hasNull) return innerType;
            if (innerType.startsWith("*") || innerType.startsWith("[]") || innerType.startsWith("map[")) {
                return innerType;
            }
            return `*${innerType}`;
        }

        if (nonNull.length > 1) {
            // Resolve $refs in variants before discriminator analysis
            const resolvedVariants = nonNull.map((v) => {
                if (v.$ref && typeof v.$ref === "string") {
                    return resolveRef(v.$ref, ctx.definitions) ?? v;
                }
                return v;
            });
            // Check for discriminated union
            const disc = findGoDiscriminator(resolvedVariants);
            if (disc) {
                const unionName = (propSchema.title as string) || nestedName;
                emitGoFlatDiscriminatedUnion(unionName, disc.property, disc.mapping, ctx, propSchema.description);
                return isRequired && !hasNull ? unionName : `*${unionName}`;
            }
            // Non-discriminated multi-type union → any
            return "any";
        }
    }

    // Handle enum
    if (propSchema.enum && Array.isArray(propSchema.enum)) {
        const enumType = getOrCreateGoEnum((propSchema.title as string) || nestedName, propSchema.enum as string[], ctx, propSchema.description, isSchemaDeprecated(propSchema));
        return isRequired ? enumType : `*${enumType}`;
    }

    // Handle const (discriminator markers) — just use string
    if (propSchema.const !== undefined) {
        return isRequired ? "string" : "*string";
    }

    const type = propSchema.type;
    const format = propSchema.format;

    // Handle type arrays like ["string", "null"]
    if (Array.isArray(type)) {
        const nonNullTypes = (type as string[]).filter((t) => t !== "null");
        if (nonNullTypes.length === 1) {
            const inner = resolveGoPropertyType(
                { ...propSchema, type: nonNullTypes[0] as JSONSchema7["type"] },
                parentTypeName,
                jsonPropName,
                true,
                ctx
            );
            if (inner.startsWith("*") || inner.startsWith("[]") || inner.startsWith("map[")) return inner;
            return `*${inner}`;
        }
    }

    // Simple types
    if (type === "string") {
        if (format === "date-time") {
            return isRequired ? "time.Time" : "*time.Time";
        }
        return isRequired ? "string" : "*string";
    }
    if (type === "number") return isRequired ? "float64" : "*float64";
    if (type === "integer") return isRequired ? "int64" : "*int64";
    if (type === "boolean") return isRequired ? "bool" : "*bool";

    // Array type
    if (type === "array") {
        const items = propSchema.items as JSONSchema7 | undefined;
        if (items) {
            // Discriminated union items
            if (items.anyOf) {
                const itemVariants = (items.anyOf as JSONSchema7[]).filter((v) => v.type !== "null");
                const disc = findGoDiscriminator(itemVariants);
                if (disc) {
                    const itemTypeName = (items.title as string) || (nestedName + "Item");
                    emitGoFlatDiscriminatedUnion(itemTypeName, disc.property, disc.mapping, ctx, items.description);
                    return `[]${itemTypeName}`;
                }
            }
            const itemType = resolveGoPropertyType(items, parentTypeName, jsonPropName + "Item", true, ctx);
            return `[]${itemType}`;
        }
        return "[]any";
    }

    // Object type
    if (type === "object" || (propSchema.properties && !type)) {
        if (propSchema.properties && Object.keys(propSchema.properties).length > 0) {
            const structName = (propSchema.title as string) || nestedName;
            emitGoStruct(structName, propSchema, ctx);
            return isRequired ? structName : `*${structName}`;
        }
        if (propSchema.additionalProperties) {
            if (
                typeof propSchema.additionalProperties === "object" &&
                Object.keys(propSchema.additionalProperties as Record<string, unknown>).length > 0
            ) {
                const ap = propSchema.additionalProperties as JSONSchema7;
                if (ap.type === "object" && ap.properties) {
                    const valueName = (ap.title as string) || `${nestedName}Value`;
                    emitGoStruct(valueName, ap, ctx);
                    return `map[string]${valueName}`;
                }
                const valueType = resolveGoPropertyType(ap, parentTypeName, jsonPropName + "Value", true, ctx);
                return `map[string]${valueType}`;
            }
            return "map[string]any";
        }
        // Empty object or untyped
        return "any";
    }

    return "any";
}

/**
 * Emit a Go struct definition from an object schema.
 */
function emitGoStruct(
    typeName: string,
    schema: JSONSchema7,
    ctx: GoCodegenCtx,
    description?: string
): void {
    if (ctx.generatedNames.has(typeName)) return;
    ctx.generatedNames.add(typeName);

    const required = new Set(schema.required || []);
    const lines: string[] = [];
    const desc = description || schema.description;
    if (desc) {
        for (const line of desc.split(/\r?\n/)) {
            lines.push(`// ${line}`);
        }
    }
    if (isSchemaDeprecated(schema)) {
        lines.push(`// Deprecated: ${typeName} is deprecated and will be removed in a future version.`);
    }
    lines.push(`type ${typeName} struct {`);

    for (const [propName, propSchema] of Object.entries(schema.properties || {}).sort(([a], [b]) => a.localeCompare(b))) {
        if (typeof propSchema !== "object") continue;
        const prop = propSchema as JSONSchema7;
        const isReq = required.has(propName);
        const goName = toGoFieldName(propName);
        const goType = resolveGoPropertyType(prop, typeName, propName, isReq, ctx);
        const omit = isReq ? "" : ",omitempty";

        if (prop.description) {
            lines.push(`\t// ${prop.description}`);
        }
        if (isSchemaDeprecated(prop)) {
            lines.push(`\t// Deprecated: ${goName} is deprecated.`);
        }
        lines.push(`\t${goName} ${goType} \`json:"${propName}${omit}"\``);
    }

    lines.push(`}`);
    ctx.structs.push(lines.join("\n"));
}

/**
 * Emit a flat Go struct for a discriminated union (anyOf with const discriminator).
 * Merges all variant properties into a single struct.
 */
function emitGoFlatDiscriminatedUnion(
    typeName: string,
    discriminatorProp: string,
    mapping: Map<string, JSONSchema7>,
    ctx: GoCodegenCtx,
    description?: string
): void {
    if (ctx.generatedNames.has(typeName)) return;
    ctx.generatedNames.add(typeName);

    // Collect all properties across variants, determining which are required in all
    const allProps = new Map<
        string,
        { schema: JSONSchema7; requiredInAll: boolean }
    >();

    for (const [, variant] of mapping) {
        const required = new Set(variant.required || []);
        for (const [propName, propSchema] of Object.entries(variant.properties || {})) {
            if (typeof propSchema !== "object") continue;
            if (!allProps.has(propName)) {
                allProps.set(propName, {
                    schema: propSchema as JSONSchema7,
                    requiredInAll: required.has(propName),
                });
            } else {
                const existing = allProps.get(propName)!;
                if (!required.has(propName)) {
                    existing.requiredInAll = false;
                }
            }
        }
    }

    // Properties not present in all variants must be optional
    const variantCount = mapping.size;
    for (const [propName, info] of allProps) {
        let presentCount = 0;
        for (const [, variant] of mapping) {
            if (variant.properties && propName in variant.properties) {
                presentCount++;
            }
        }
        if (presentCount < variantCount) {
            info.requiredInAll = false;
        }
    }

    // Discriminator field: generate an enum from the const values
    const discGoName = toGoFieldName(discriminatorProp);
    const discValues = [...mapping.keys()];
    const discEnumName = getOrCreateGoEnum(
        typeName + discGoName,
        discValues,
        ctx,
        `${discGoName} discriminator for ${typeName}.`
    );

    const lines: string[] = [];
    if (description) {
        for (const line of description.split(/\r?\n/)) {
            lines.push(`// ${line}`);
        }
    }
    lines.push(`type ${typeName} struct {`);

    // Emit discriminator field first
    lines.push(`\t// ${discGoName} discriminator`);
    lines.push(`\t${discGoName} ${discEnumName} \`json:"${discriminatorProp}"\``);

    // Emit remaining fields
    for (const [propName, info] of [...allProps.entries()].sort(([a], [b]) => a.localeCompare(b))) {
        if (propName === discriminatorProp) continue;
        const goName = toGoFieldName(propName);
        const goType = resolveGoPropertyType(info.schema, typeName, propName, info.requiredInAll, ctx);
        const omit = info.requiredInAll ? "" : ",omitempty";
        if (info.schema.description) {
            lines.push(`\t// ${info.schema.description}`);
        }
        if (isSchemaDeprecated(info.schema)) {
            lines.push(`\t// Deprecated: ${goName} is deprecated.`);
        }
        lines.push(`\t${goName} ${goType} \`json:"${propName}${omit}"\``);
    }

    lines.push(`}`);
    ctx.structs.push(lines.join("\n"));
}

/**
 * Generate the complete Go session-events file content.
 */
function generateGoSessionEventsCode(schema: JSONSchema7): string {
    const variants = extractGoEventVariants(schema);
    const ctx: GoCodegenCtx = {
        structs: [],
        enums: [],
        enumsByName: new Map(),
        generatedNames: new Set(),
        definitions: collectDefinitionCollections(schema as Record<string, unknown>),
    };

    // Generate per-event data structs
    const dataStructs: string[] = [];
    for (const variant of variants) {
        const required = new Set(variant.dataSchema.required || []);
        const lines: string[] = [];

        if (variant.dataDescription) {
            for (const line of variant.dataDescription.split(/\r?\n/)) {
                lines.push(`// ${line}`);
            }
        } else {
            lines.push(`// ${variant.dataClassName} holds the payload for ${variant.typeName} events.`);
        }
        lines.push(`type ${variant.dataClassName} struct {`);

        for (const [propName, propSchema] of Object.entries(variant.dataSchema.properties || {}).sort(([a], [b]) => a.localeCompare(b))) {
            if (typeof propSchema !== "object") continue;
            const prop = propSchema as JSONSchema7;
            const isReq = required.has(propName);
            const goName = toGoFieldName(propName);
            const goType = resolveGoPropertyType(prop, variant.dataClassName, propName, isReq, ctx);
            const omit = isReq ? "" : ",omitempty";

            if (prop.description) {
                lines.push(`\t// ${prop.description}`);
            }
            if (isSchemaDeprecated(prop)) {
                lines.push(`\t// Deprecated: ${goName} is deprecated.`);
            }
            lines.push(`\t${goName} ${goType} \`json:"${propName}${omit}"\``);
        }

        lines.push(`}`);
        lines.push(``);
        lines.push(`func (*${variant.dataClassName}) sessionEventData() {}`);

        dataStructs.push(lines.join("\n"));
    }

    // Generate SessionEventType enum
    const eventTypeEnum: string[] = [];
    eventTypeEnum.push(`// SessionEventType identifies the kind of session event.`);
    eventTypeEnum.push(`type SessionEventType string`);
    eventTypeEnum.push(``);
    eventTypeEnum.push(`const (`);
    for (const variant of variants) {
        const constName =
            "SessionEventType" +
            variant.typeName
                .split(/[._]/)
                .map((w) =>
                    goInitialisms.has(w.toLowerCase())
                        ? w.toUpperCase()
                        : w.charAt(0).toUpperCase() + w.slice(1)
                )
                .join("");
        eventTypeEnum.push(`\t${constName} SessionEventType = "${variant.typeName}"`);
    }
    eventTypeEnum.push(`)`);

    // Assemble file
    const out: string[] = [];
    out.push(`// AUTO-GENERATED FILE - DO NOT EDIT`);
    out.push(`// Generated from: session-events.schema.json`);
    out.push(``);
    out.push(`package copilot`);
    out.push(``);

    // Imports — time is always needed for SessionEvent.Timestamp
    out.push(`import (`);
    out.push(`\t"encoding/json"`);
    out.push(`\t"time"`);
    out.push(`)`);
    out.push(``);

    // SessionEventData interface
    out.push(`// SessionEventData is the interface implemented by all per-event data types.`);
    out.push(`type SessionEventData interface {`);
    out.push(`\tsessionEventData()`);
    out.push(`}`);
    out.push(``);

    // RawSessionEventData for unknown event types
    out.push(`// RawSessionEventData holds unparsed JSON data for unrecognized event types.`);
    out.push(`type RawSessionEventData struct {`);
    out.push(`\tRaw json.RawMessage`);
    out.push(`}`);
    out.push(``);
    out.push(`func (RawSessionEventData) sessionEventData() {}`);
    out.push(``);
    out.push(`// MarshalJSON returns the original raw JSON so round-tripping preserves the payload.`);
    out.push(`func (r RawSessionEventData) MarshalJSON() ([]byte, error) { return r.Raw, nil }`);
    out.push(``);

    // SessionEvent struct
    out.push(`// SessionEvent represents a single session event with a typed data payload.`);
    out.push(`type SessionEvent struct {`);
    out.push(`\t// Unique event identifier (UUID v4), generated when the event is emitted.`);
    out.push(`\tID string \`json:"id"\``);
    out.push(`\t// ISO 8601 timestamp when the event was created.`);
    out.push(`\tTimestamp time.Time \`json:"timestamp"\``);
    // parentId: string or null
    out.push(`\t// ID of the preceding event in the session. Null for the first event.`);
    out.push(`\tParentID *string \`json:"parentId"\``);
    out.push(`\t// When true, the event is transient and not persisted.`);
    out.push(`\tEphemeral *bool \`json:"ephemeral,omitempty"\``);
    out.push(`\t// The event type discriminator.`);
    out.push(`\tType SessionEventType \`json:"type"\``);
    out.push(`\t// Typed event payload. Use a type switch to access per-event fields.`);
    out.push(`\tData SessionEventData \`json:"-"\``);
    out.push(`}`);
    out.push(``);

    // UnmarshalSessionEvent
    out.push(`// UnmarshalSessionEvent parses JSON bytes into a SessionEvent.`);
    out.push(`func UnmarshalSessionEvent(data []byte) (SessionEvent, error) {`);
    out.push(`\tvar r SessionEvent`);
    out.push(`\terr := json.Unmarshal(data, &r)`);
    out.push(`\treturn r, err`);
    out.push(`}`);
    out.push(``);

    // Marshal
    out.push(`// Marshal serializes the SessionEvent to JSON.`);
    out.push(`func (r *SessionEvent) Marshal() ([]byte, error) {`);
    out.push(`\treturn json.Marshal(r)`);
    out.push(`}`);
    out.push(``);

    // Custom UnmarshalJSON
    out.push(`func (e *SessionEvent) UnmarshalJSON(data []byte) error {`);
    out.push(`\ttype rawEvent struct {`);
    out.push(`\t\tID        string           \`json:"id"\``);
    out.push(`\t\tTimestamp time.Time        \`json:"timestamp"\``);
    out.push(`\t\tParentID  *string          \`json:"parentId"\``);
    out.push(`\t\tEphemeral *bool            \`json:"ephemeral,omitempty"\``);
    out.push(`\t\tType      SessionEventType \`json:"type"\``);
    out.push(`\t\tData      json.RawMessage  \`json:"data"\``);
    out.push(`\t}`);
    out.push(`\tvar raw rawEvent`);
    out.push(`\tif err := json.Unmarshal(data, &raw); err != nil {`);
    out.push(`\t\treturn err`);
    out.push(`\t}`);
    out.push(`\te.ID = raw.ID`);
    out.push(`\te.Timestamp = raw.Timestamp`);
    out.push(`\te.ParentID = raw.ParentID`);
    out.push(`\te.Ephemeral = raw.Ephemeral`);
    out.push(`\te.Type = raw.Type`);
    out.push(``);
    out.push(`\tswitch raw.Type {`);
    for (const variant of variants) {
        const constName =
            "SessionEventType" +
            variant.typeName
                .split(/[._]/)
                .map((w) =>
                    goInitialisms.has(w.toLowerCase())
                        ? w.toUpperCase()
                        : w.charAt(0).toUpperCase() + w.slice(1)
                )
                .join("");
        out.push(`\tcase ${constName}:`);
        out.push(`\t\tvar d ${variant.dataClassName}`);
        out.push(`\t\tif err := json.Unmarshal(raw.Data, &d); err != nil {`);
        out.push(`\t\t\treturn err`);
        out.push(`\t\t}`);
        out.push(`\t\te.Data = &d`);
    }
    out.push(`\tdefault:`);
    out.push(`\t\te.Data = &RawSessionEventData{Raw: raw.Data}`);
    out.push(`\t}`);
    out.push(`\treturn nil`);
    out.push(`}`);
    out.push(``);

    // Custom MarshalJSON
    out.push(`func (e SessionEvent) MarshalJSON() ([]byte, error) {`);
    out.push(`\ttype rawEvent struct {`);
    out.push(`\t\tID        string           \`json:"id"\``);
    out.push(`\t\tTimestamp time.Time        \`json:"timestamp"\``);
    out.push(`\t\tParentID  *string          \`json:"parentId"\``);
    out.push(`\t\tEphemeral *bool            \`json:"ephemeral,omitempty"\``);
    out.push(`\t\tType      SessionEventType \`json:"type"\``);
    out.push(`\t\tData      any              \`json:"data"\``);
    out.push(`\t}`);
    out.push(`\treturn json.Marshal(rawEvent{`);
    out.push(`\t\tID:        e.ID,`);
    out.push(`\t\tTimestamp: e.Timestamp,`);
    out.push(`\t\tParentID:  e.ParentID,`);
    out.push(`\t\tEphemeral: e.Ephemeral,`);
    out.push(`\t\tType:      e.Type,`);
    out.push(`\t\tData:      e.Data,`);
    out.push(`\t})`);
    out.push(`}`);
    out.push(``);

    // Event type enum
    out.push(eventTypeEnum.join("\n"));
    out.push(``);

    // Per-event data structs
    for (const ds of dataStructs.sort()) {
        out.push(ds);
        out.push(``);
    }

    // Nested structs
    for (const s of ctx.structs.sort()) {
        out.push(s);
        out.push(``);
    }

    // Enums
    for (const e of ctx.enums.sort()) {
        out.push(e);
        out.push(``);
    }

    // Type aliases for types referenced by non-generated SDK code under their short names.
    const TYPE_ALIASES: Record<string, string> = {
        PermissionRequestCommand: "PermissionRequestShellCommand",
        PossibleURL: "PermissionRequestShellPossibleURL",
        Attachment: "UserMessageAttachment",
        AttachmentType: "UserMessageAttachmentType",
    };
    const CONST_ALIASES: Record<string, string> = {
        AttachmentTypeFile: "UserMessageAttachmentTypeFile",
        AttachmentTypeDirectory: "UserMessageAttachmentTypeDirectory",
        AttachmentTypeSelection: "UserMessageAttachmentTypeSelection",
        AttachmentTypeGithubReference: "UserMessageAttachmentTypeGithubReference",
        AttachmentTypeBlob: "UserMessageAttachmentTypeBlob",
    };
    out.push(`// Type aliases for convenience.`);
    out.push(`type (`);
    for (const [alias, target] of Object.entries(TYPE_ALIASES)) {
        out.push(`\t${alias} = ${target}`);
    }
    out.push(`)`);
    out.push(``);
    out.push(`// Constant aliases for convenience.`);
    out.push(`const (`);
    for (const [alias, target] of Object.entries(CONST_ALIASES)) {
        out.push(`\t${alias} = ${target}`);
    }
    out.push(`)`);
    out.push(``);

    return out.join("\n");
}

async function generateSessionEvents(schemaPath?: string): Promise<void> {
    console.log("Go: generating session-events...");

    const resolvedPath = schemaPath ?? (await getSessionEventsSchemaPath());
    const schema = cloneSchemaForCodegen(JSON.parse(await fs.readFile(resolvedPath, "utf-8")) as JSONSchema7);
    const processed = postProcessSchema(schema);

    const code = generateGoSessionEventsCode(processed);

    const outPath = await writeGeneratedFile("go/generated_session_events.go", code);
    console.log(`  ✓ ${outPath}`);

    await formatGoFile(outPath);
}

// ── RPC Types ───────────────────────────────────────────────────────────────

async function generateRpc(schemaPath?: string): Promise<void> {
    console.log("Go: generating RPC types...");

    const resolvedPath = schemaPath ?? (await getApiSchemaPath());
    const schema = fixNullableRequiredRefsInApiSchema(cloneSchemaForCodegen(JSON.parse(await fs.readFile(resolvedPath, "utf-8")) as ApiSchema));

    const allMethods = [
        ...collectRpcMethods(schema.server || {}),
        ...collectRpcMethods(schema.session || {}),
        ...collectRpcMethods(schema.clientSession || {}),
    ];

    // Build a combined schema for quicktype — prefix types to avoid conflicts.
    // Include shared definitions from the API schema for $ref resolution.
    rpcDefinitions = collectDefinitionCollections(schema as Record<string, unknown>);
    const combinedSchema = withSharedDefinitions(
        {
            $schema: "http://json-schema.org/draft-07/schema#",
        },
        rpcDefinitions
    );

    for (const method of allMethods) {
        const resultSchema = getMethodResultSchema(method);
        const nullableInner = resultSchema ? getNullableInner(resultSchema) : undefined;
        if (nullableInner) {
            // Nullable results (e.g., *SessionFSError) don't need a wrapper type;
            // the inner type is already in definitions via shared hoisting.
        } else if (isVoidSchema(resultSchema)) {
            // Emit an empty struct for void results (forward-compatible with adding fields later)
            combinedSchema.definitions![goResultTypeName(method)] = {
                title: goResultTypeName(method),
                type: "object",
                properties: {},
                additionalProperties: false,
            };
        } else if (method.result) {
            combinedSchema.definitions![goResultTypeName(method)] = withRootTitle(
                schemaSourceForNamedDefinition(method.result, resultSchema),
                goResultTypeName(method)
            );
        }
        const resolvedParams = getMethodParamsSchema(method);
        if (method.params && hasSchemaPayload(resolvedParams)) {
            // For session methods, filter out sessionId from params type
            if (method.rpcMethod.startsWith("session.") && resolvedParams?.properties) {
                const filtered: JSONSchema7 = {
                    ...resolvedParams,
                    properties: Object.fromEntries(
                        Object.entries(resolvedParams.properties).filter(([k]) => k !== "sessionId")
                    ),
                    required: resolvedParams.required?.filter((r) => r !== "sessionId"),
                };
                if (hasSchemaPayload(filtered)) {
                    combinedSchema.definitions![goParamsTypeName(method)] = withRootTitle(
                        filtered,
                        goParamsTypeName(method)
                    );
                }
            } else {
                combinedSchema.definitions![goParamsTypeName(method)] = withRootTitle(
                    schemaSourceForNamedDefinition(method.params, resolvedParams),
                    goParamsTypeName(method)
                );
            }
        }
    }

    const allDefinitions = combinedSchema.definitions! as Record<string, JSONSchema7>;
    const allDefinitionCollections: DefinitionCollections = {
        definitions: { ...(combinedSchema.$defs ?? {}), ...allDefinitions },
        $defs: { ...allDefinitions, ...(combinedSchema.$defs ?? {}) },
    };

    // Generate types via quicktype — use a single combined schema source so quicktype
    // sees each definition exactly once, preventing whimsical prefix disambiguation.
    const schemaInput = new JSONSchemaInput(new FetchingJSONSchemaStore());
    const singleSchema: JSONSchema7 = {
        $schema: "http://json-schema.org/draft-07/schema#",
        type: "object",
        definitions: allDefinitions as Record<string, JSONSchema7>,
        properties: Object.fromEntries(
            Object.keys(allDefinitions).map((name) => [name, { $ref: `#/definitions/${name}` }])
        ),
        required: Object.keys(allDefinitions),
    };
    await schemaInput.addSource({ name: "RpcTypes", schema: JSON.stringify(singleSchema) });

    const inputData = new InputData();
    inputData.addInput(schemaInput);

    const qtResult = await quicktype({
        inputData,
        lang: "go",
        rendererOptions: { package: "copilot", "just-types": "true" },
    });

    // Post-process quicktype output: hoist quicktype's imports into the file-level import block
    let qtCode = qtResult.lines.filter((l) => !l.startsWith("package ")).join("\n");
    const quicktypeImports = extractQuicktypeImports(qtCode);
    qtCode = quicktypeImports.code;
    qtCode = postProcessEnumConstants(qtCode);
    const knownDefNames = new Set(Object.keys(allDefinitions).map((n) => n.toLowerCase()));
    qtCode = collapsePlaceholderGoStructs(qtCode, knownDefNames);
    // Strip trailing whitespace from quicktype output (gofmt requirement)
    qtCode = qtCode.replace(/[ \t]+$/gm, "");

    // Extract actual type names generated by quicktype (may differ from toPascalCase)
    const actualTypeNames = new Map<string, string>();
    const typeRe = /^type\s+(\w+)\b/gm;
    let sm;
    while ((sm = typeRe.exec(qtCode)) !== null) {
        actualTypeNames.set(sm[1].toLowerCase(), sm[1]);
    }
    const resolveType = (name: string): string => actualTypeNames.get(name.toLowerCase()) ?? name;

    // Extract field name mappings (quicktype may rename fields to avoid Go keyword conflicts)
    const fieldNames = extractFieldNames(qtCode);

    // Annotate experimental data types
    const experimentalTypeNames = new Set<string>();
    for (const method of allMethods) {
        if (method.stability !== "experimental") continue;
        experimentalTypeNames.add(goResultTypeName(method));
        const paramsTypeName = goParamsTypeName(method);
        if (allDefinitions[paramsTypeName]) {
            experimentalTypeNames.add(paramsTypeName);
        }
    }
    for (const typeName of experimentalTypeNames) {
        qtCode = qtCode.replace(
            new RegExp(`^(type ${typeName} struct)`, "m"),
            `// Experimental: ${typeName} is part of an experimental API and may change or be removed.\n$1`
        );
    }

    // Annotate deprecated data types
    const deprecatedTypeNames = new Set<string>();
    for (const method of allMethods) {
        if (!method.deprecated) continue;
        if (!method.result?.$ref) {
            deprecatedTypeNames.add(goResultTypeName(method));
        }
        if (!method.params?.$ref) {
            const paramsTypeName = goParamsTypeName(method);
            if (allDefinitions[paramsTypeName]) {
                deprecatedTypeNames.add(paramsTypeName);
            }
        }
    }
    for (const typeName of deprecatedTypeNames) {
        qtCode = qtCode.replace(
            new RegExp(`^(type ${typeName} struct)`, "m"),
            `// Deprecated: ${typeName} is deprecated and will be removed in a future version.\n$1`
        );
    }
    // Remove trailing blank lines from quicktype output before appending
    qtCode = qtCode.replace(/\n+$/, "");
    // Replace interface{} with any (quicktype emits the pre-1.18 form)
    qtCode = qtCode.replace(/\binterface\{\}/g, "any");

    // Build method wrappers
    const lines: string[] = [];
    lines.push(`// AUTO-GENERATED FILE - DO NOT EDIT`);
    lines.push(`// Generated from: api.schema.json`);
    lines.push(``);
    lines.push(`package rpc`);
    lines.push(``);
    const imports = [`"context"`, `"encoding/json"`];
    for (const imp of quicktypeImports.imports) {
        if (!imports.includes(imp)) {
            imports.push(imp);
        }
    }
    if (schema.clientSession) {
        imports.push(`"errors"`, `"fmt"`);
    }
    imports.push(`"github.com/github/copilot-sdk/go/internal/jsonrpc2"`);

    lines.push(`import (`);
    for (const imp of imports) {
        lines.push(`\t${imp}`);
    }
    lines.push(`)`);
    lines.push(``);

    lines.push(qtCode);
    lines.push(``);

    // Emit ServerRpc
    if (schema.server) {
        emitRpcWrapper(lines, schema.server, false, resolveType, fieldNames);
    }

    // Emit SessionRpc
    if (schema.session) {
        emitRpcWrapper(lines, schema.session, true, resolveType, fieldNames);
    }

    if (schema.clientSession) {
        emitClientSessionApiRegistration(lines, schema.clientSession, resolveType);
    }

    const outPath = await writeGeneratedFile("go/rpc/generated_rpc.go", lines.join("\n"));
    console.log(`  ✓ ${outPath}`);

    await formatGoFile(outPath);
}

function emitApiGroup(
    lines: string[],
    apiName: string,
    node: Record<string, unknown>,
    isSession: boolean,
    serviceName: string,
    resolveType: (name: string) => string,
    fieldNames: Map<string, Map<string, string>>,
    groupExperimental: boolean,
    groupDeprecated: boolean = false
): void {
    const subGroups = Object.entries(node).filter(([, v]) => typeof v === "object" && v !== null && !isRpcMethod(v));

    if (groupDeprecated) {
        lines.push(`// Deprecated: ${apiName} contains deprecated APIs that will be removed in a future version.`);
    }
    if (groupExperimental) {
        lines.push(`// Experimental: ${apiName} contains experimental APIs that may change or be removed.`);
    }
    lines.push(`type ${apiName} ${serviceName}`);
    lines.push(``);

    for (const [key, value] of Object.entries(node)) {
        if (!isRpcMethod(value)) continue;
        emitMethod(lines, apiName, key, value, isSession, resolveType, fieldNames, groupExperimental, false, groupDeprecated);
    }

    for (const [subGroupName, subGroupNode] of subGroups) {
        const subApiName = apiName.replace(/Api$/, "") + toPascalCase(subGroupName) + "Api";
        const subGroupExperimental = isNodeFullyExperimental(subGroupNode as Record<string, unknown>);
        const subGroupDeprecated = isNodeFullyDeprecated(subGroupNode as Record<string, unknown>);
        emitApiGroup(lines, subApiName, subGroupNode as Record<string, unknown>, isSession, serviceName, resolveType, fieldNames, subGroupExperimental, subGroupDeprecated);

        if (subGroupExperimental) {
            lines.push(`// Experimental: ${toPascalCase(subGroupName)} returns experimental APIs that may change or be removed.`);
        }
        lines.push(`func (s *${apiName}) ${toPascalCase(subGroupName)}() *${subApiName} {`);
        lines.push(`\treturn (*${subApiName})(s)`);
        lines.push(`}`);
        lines.push(``);
    }
}

function emitRpcWrapper(lines: string[], node: Record<string, unknown>, isSession: boolean, resolveType: (name: string) => string, fieldNames: Map<string, Map<string, string>>): void {
    const groups = Object.entries(node).filter(([, v]) => typeof v === "object" && v !== null && !isRpcMethod(v));
    const topLevelMethods = Object.entries(node).filter(([, v]) => isRpcMethod(v));

    const wrapperName = isSession ? "SessionRpc" : "ServerRpc";
    const apiSuffix = "Api";
    const serviceName = isSession ? "sessionApi" : "serverApi";

    // Emit the common service struct (unexported, shared by all API groups via type cast)
    lines.push(`type ${serviceName} struct {`);
    lines.push(`\tclient *jsonrpc2.Client`);
    if (isSession) lines.push(`\tsessionID string`);
    lines.push(`}`);
    lines.push(``);

    // Emit API types for groups
    for (const [groupName, groupNode] of groups) {
        const prefix = isSession ? "" : "Server";
        const apiName = prefix + toPascalCase(groupName) + apiSuffix;
        const groupExperimental = isNodeFullyExperimental(groupNode as Record<string, unknown>);
        const groupDeprecated = isNodeFullyDeprecated(groupNode as Record<string, unknown>);
        emitApiGroup(lines, apiName, groupNode as Record<string, unknown>, isSession, serviceName, resolveType, fieldNames, groupExperimental, groupDeprecated);
    }

    // Compute field name lengths for gofmt-compatible column alignment
    const groupPascalNames = groups.map(([g]) => toPascalCase(g));
    const allFieldNames = isSession ? ["common", ...groupPascalNames] : ["common", ...groupPascalNames];
    const maxFieldLen = Math.max(...allFieldNames.map((n) => n.length));
    const pad = (name: string) => name.padEnd(maxFieldLen);

    // Emit wrapper struct
    lines.push(`// ${wrapperName} provides typed ${isSession ? "session" : "server"}-scoped RPC methods.`);
    lines.push(`type ${wrapperName} struct {`);
    lines.push(`\t${pad("common")} ${serviceName} // Reuse a single struct instead of allocating one for each service on the heap.`);
    lines.push(``);
    for (const [groupName] of groups) {
        const prefix = isSession ? "" : "Server";
        lines.push(`\t${pad(toPascalCase(groupName))} *${prefix}${toPascalCase(groupName)}${apiSuffix}`);
    }
    lines.push(`}`);
    lines.push(``);

    // Top-level methods on the wrapper use the common service fields
    for (const [key, value] of topLevelMethods) {
        if (!isRpcMethod(value)) continue;
        emitMethod(lines, wrapperName, key, value, isSession, resolveType, fieldNames, false, true);
    }

    // Constructor
    const ctorParams = isSession ? "client *jsonrpc2.Client, sessionID string" : "client *jsonrpc2.Client";
    lines.push(`func New${wrapperName}(${ctorParams}) *${wrapperName} {`);
    lines.push(`\tr := &${wrapperName}{}`);
    if (isSession) {
        lines.push(`\tr.common = ${serviceName}{client: client, sessionID: sessionID}`);
    } else {
        lines.push(`\tr.common = ${serviceName}{client: client}`);
    }
    for (const [groupName] of groups) {
        const prefix = isSession ? "" : "Server";
        lines.push(`\tr.${toPascalCase(groupName)} = (*${prefix}${toPascalCase(groupName)}${apiSuffix})(&r.common)`);
    }
    lines.push(`\treturn r`);
    lines.push(`}`);
    lines.push(``);
}

function emitMethod(lines: string[], receiver: string, name: string, method: RpcMethod, isSession: boolean, resolveType: (name: string) => string, fieldNames: Map<string, Map<string, string>>, groupExperimental = false, isWrapper = false, groupDeprecated = false): void {
    const methodName = toPascalCase(name);
    const resultSchema = getMethodResultSchema(method);
    const nullableInner = resultSchema ? getNullableInner(resultSchema) : undefined;
    const resultType = nullableInner
        ? resolveType(goNullableResultTypeName(method, nullableInner))
        : resolveType(goResultTypeName(method));

    const effectiveParams = getMethodParamsSchema(method);
    const paramProps = effectiveParams?.properties || {};
    const requiredParams = new Set(effectiveParams?.required || []);
    const nonSessionParams = Object.keys(paramProps).filter((k) => k !== "sessionId");
    const hasParams = isSession ? nonSessionParams.length > 0 : hasSchemaPayload(effectiveParams);
    const paramsType = hasParams ? resolveType(goParamsTypeName(method)) : "";

    // For wrapper-level methods, access fields through a.common; for service type aliases, use a directly
    const clientRef = isWrapper ? "a.common.client" : "a.client";
    const sessionIDRef = isWrapper ? "a.common.sessionID" : "a.sessionID";

    if (method.deprecated && !groupDeprecated) {
        lines.push(`// Deprecated: ${methodName} is deprecated and will be removed in a future version.`);
    }
    if (method.stability === "experimental" && !groupExperimental) {
        lines.push(`// Experimental: ${methodName} is an experimental API and may change or be removed in future versions.`);
    }
    const sig = hasParams
        ? `func (a *${receiver}) ${methodName}(ctx context.Context, params *${paramsType}) (*${resultType}, error)`
        : `func (a *${receiver}) ${methodName}(ctx context.Context) (*${resultType}, error)`;

    lines.push(sig + ` {`);

    if (isSession) {
        lines.push(`\treq := map[string]any{"sessionId": ${sessionIDRef}}`);
        if (hasParams) {
            lines.push(`\tif params != nil {`);
            for (const pName of nonSessionParams) {
                const goField = fieldNames.get(paramsType)?.get(pName) ?? toGoFieldName(pName);
                const isOptional = !requiredParams.has(pName);
                if (isOptional) {
                    // Optional fields are pointers - only add when non-nil and dereference
                    lines.push(`\t\tif params.${goField} != nil {`);
                    lines.push(`\t\t\treq["${pName}"] = *params.${goField}`);
                    lines.push(`\t\t}`);
                } else {
                    lines.push(`\t\treq["${pName}"] = params.${goField}`);
                }
            }
            lines.push(`\t}`);
        }
        lines.push(`\traw, err := ${clientRef}.Request("${method.rpcMethod}", req)`);
    } else {
        const arg = hasParams ? "params" : "nil";
        lines.push(`\traw, err := ${clientRef}.Request("${method.rpcMethod}", ${arg})`);
    }

    lines.push(`\tif err != nil {`);
    lines.push(`\t\treturn nil, err`);
    lines.push(`\t}`);
    lines.push(`\tvar result ${resultType}`);
    lines.push(`\tif err := json.Unmarshal(raw, &result); err != nil {`);
    lines.push(`\t\treturn nil, err`);
    lines.push(`\t}`);
    lines.push(`\treturn &result, nil`);
    lines.push(`}`);
    lines.push(``);
}

interface ClientGroup {
    groupName: string;
    groupNode: Record<string, unknown>;
    methods: RpcMethod[];
}

function collectClientGroups(node: Record<string, unknown>): ClientGroup[] {
    const groups: ClientGroup[] = [];
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
    return `${toPascalCase(groupName)}Handler`;
}

function clientHandlerMethodName(rpcMethod: string): string {
    return toPascalCase(rpcMethod.split(".").at(-1)!);
}

function emitClientSessionApiRegistration(lines: string[], clientSchema: Record<string, unknown>, resolveType: (name: string) => string): void {
    const groups = collectClientGroups(clientSchema);

    for (const { groupName, groupNode, methods } of groups) {
        const interfaceName = clientHandlerInterfaceName(groupName);
        const groupExperimental = isNodeFullyExperimental(groupNode);
        const groupDeprecated = isNodeFullyDeprecated(groupNode);
        if (groupDeprecated) {
            lines.push(`// Deprecated: ${interfaceName} contains deprecated APIs that will be removed in a future version.`);
        }
        if (groupExperimental) {
            lines.push(`// Experimental: ${interfaceName} contains experimental APIs that may change or be removed.`);
        }
        lines.push(`type ${interfaceName} interface {`);
        for (const method of methods) {
            if (method.deprecated && !groupDeprecated) {
                lines.push(`\t// Deprecated: ${clientHandlerMethodName(method.rpcMethod)} is deprecated and will be removed in a future version.`);
            }
            if (method.stability === "experimental" && !groupExperimental) {
                lines.push(`\t// Experimental: ${clientHandlerMethodName(method.rpcMethod)} is an experimental API and may change or be removed in future versions.`);
            }
            const paramsType = resolveType(goParamsTypeName(method));
            const resultSchema = getMethodResultSchema(method);
            const nullableInner = resultSchema ? getNullableInner(resultSchema) : undefined;
            const resultType = nullableInner
                ? resolveType(goNullableResultTypeName(method, nullableInner))
                : resolveType(goResultTypeName(method));
            lines.push(`\t${clientHandlerMethodName(method.rpcMethod)}(request *${paramsType}) (*${resultType}, error)`);
        }
        lines.push(`}`);
        lines.push(``);
    }

    lines.push(`// ClientSessionApiHandlers provides all client session API handler groups for a session.`);
    lines.push(`type ClientSessionApiHandlers struct {`);
    for (const { groupName } of groups) {
        lines.push(`\t${toPascalCase(groupName)} ${clientHandlerInterfaceName(groupName)}`);
    }
    lines.push(`}`);
    lines.push(``);

    lines.push(`func clientSessionHandlerError(err error) *jsonrpc2.Error {`);
    lines.push(`\tif err == nil {`);
    lines.push(`\t\treturn nil`);
    lines.push(`\t}`);
    lines.push(`\tvar rpcErr *jsonrpc2.Error`);
    lines.push(`\tif errors.As(err, &rpcErr) {`);
    lines.push(`\t\treturn rpcErr`);
    lines.push(`\t}`);
    lines.push(`\treturn &jsonrpc2.Error{Code: -32603, Message: err.Error()}`);
    lines.push(`}`);
    lines.push(``);

    lines.push(`// RegisterClientSessionApiHandlers registers handlers for server-to-client session API calls.`);
    lines.push(`func RegisterClientSessionApiHandlers(client *jsonrpc2.Client, getHandlers func(sessionID string) *ClientSessionApiHandlers) {`);
    for (const { groupName, methods } of groups) {
        const handlerField = toPascalCase(groupName);
        for (const method of methods) {
            const paramsType = resolveType(goParamsTypeName(method));
            lines.push(`\tclient.SetRequestHandler("${method.rpcMethod}", func(params json.RawMessage) (json.RawMessage, *jsonrpc2.Error) {`);
            lines.push(`\t\tvar request ${paramsType}`);
            lines.push(`\t\tif err := json.Unmarshal(params, &request); err != nil {`);
            lines.push(`\t\t\treturn nil, &jsonrpc2.Error{Code: -32602, Message: fmt.Sprintf("Invalid params: %v", err)}`);
            lines.push(`\t\t}`);
            lines.push(`\t\thandlers := getHandlers(request.SessionID)`);
            lines.push(`\t\tif handlers == nil || handlers.${handlerField} == nil {`);
            lines.push(`\t\t\treturn nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("No ${groupName} handler registered for session: %s", request.SessionID)}`);
            lines.push(`\t\t}`);
            lines.push(`\t\tresult, err := handlers.${handlerField}.${clientHandlerMethodName(method.rpcMethod)}(&request)`);
            lines.push(`\t\tif err != nil {`);
            lines.push(`\t\t\treturn nil, clientSessionHandlerError(err)`);
            lines.push(`\t\t}`);
            lines.push(`\t\traw, err := json.Marshal(result)`);
            lines.push(`\t\tif err != nil {`);
            lines.push(`\t\t\treturn nil, &jsonrpc2.Error{Code: -32603, Message: fmt.Sprintf("Failed to marshal response: %v", err)}`);
            lines.push(`\t\t}`);
            lines.push(`\t\treturn raw, nil`);
            lines.push(`\t})`);
        }
    }
    lines.push(`}`);
    lines.push(``);
}

// ── Main ────────────────────────────────────────────────────────────────────

async function generate(sessionSchemaPath?: string, apiSchemaPath?: string): Promise<void> {
    await generateSessionEvents(sessionSchemaPath);
    try {
        await generateRpc(apiSchemaPath);
    } catch (err) {
        if ((err as NodeJS.ErrnoException).code === "ENOENT" && !apiSchemaPath) {
            console.log("Go: skipping RPC (api.schema.json not found)");
        } else {
            throw err;
        }
    }
}

const sessionArg = process.argv[2] || undefined;
const apiArg = process.argv[3] || undefined;
generate(sessionArg, apiArg).catch((err) => {
    console.error("Go generation failed:", err);
    process.exit(1);
});
