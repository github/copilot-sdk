/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Go code generator for session-events and RPC types.
 */

import { execFile } from "child_process";
import fs from "fs/promises";
import type { JSONSchema7 } from "json-schema";
import { promisify } from "util";
import wordwrap from "wordwrap";
import {
    cloneSchemaForCodegen,
    collectDefinitionCollections,
    filterNodeByVisibility,
    fixNullableRequiredRefsInApiSchema,
    getApiSchemaPath,
    getNullableInner,
    getRpcSchemaTypeName,
    getSessionEventsSchemaPath,
    getSessionEventVariantSchemas,
    getSharedSessionEventEnvelopeProperties,
    hasSchemaPayload,
    isNodeFullyDeprecated,
    isNodeFullyExperimental,
    isRpcMethod,
    isSchemaDeprecated,
    isSchemaExperimental,
    isVoidSchema,
    postProcessSchema,
    refTypeName,
    resolveObjectSchema,
    resolveRef,
    resolveSchema,
    writeGeneratedFile,
    type ApiSchema,
    type DefinitionCollections,
    type RpcMethod,
    type SessionEventEnvelopeProperty,
} from "./utils.js";

const execFileAsync = promisify(execFile);

// ── Utilities ───────────────────────────────────────────────────────────────

// Go initialisms that should be all-caps
const goInitialisms = new Set(["id", "ui", "uri", "url", "api", "http", "https", "json", "xml", "html", "css", "sql", "ssh", "tcp", "udp", "ip", "rpc", "mime"]);
const goCommentTextWrapLength = 90;
const wrapGoCommentText = wordwrap(goCommentTextWrapLength);

function toPascalCase(s: string): string {
    return s
        .split(/[^A-Za-z0-9]+/)
        .filter((word) => word.length > 0)
        .map((w) => goInitialisms.has(w.toLowerCase()) ? w.toUpperCase() : w.charAt(0).toUpperCase() + w.slice(1))
        .join("");
}

function toGoSchemaTypeName(s: string): string {
    return toPascalCase(splitGoIdentifierWords(s).join("_"));
}

function toGoFieldName(jsonName: string): string {
    // Handle camelCase field names like "modelId" -> "ModelID"
    return splitGoIdentifierWords(jsonName)
        .map((w) => goInitialisms.has(w.toLowerCase()) ? w.toUpperCase() : w.charAt(0).toUpperCase() + w.slice(1).toLowerCase())
        .join("");
}

function compareGoFieldNames(left: string, right: string): number {
    return left.localeCompare(right);
}

function sortByGoFieldName<T>(entries: [string, T][]): [string, T][] {
    return entries.sort(([left], [right]) => compareGoFieldNames(toGoFieldName(left), toGoFieldName(right)));
}

function sortByPascalName<T>(entries: [string, T][]): [string, T][] {
    return entries.sort(([left], [right]) => toPascalCase(left).localeCompare(toPascalCase(right)));
}

function compareGoTypeNames(left: string, right: string): number {
    return left.localeCompare(right);
}

function compareRpcMethodsByGoName(left: RpcMethod, right: RpcMethod): number {
    return clientHandlerMethodName(left.rpcMethod).localeCompare(clientHandlerMethodName(right.rpcMethod));
}

function splitGoIdentifierWords(name: string): string[] {
    return name
        .replace(/([A-Z]+)([A-Z][a-z])/g, "$1_$2")
        .replace(/([a-z0-9])([A-Z])/g, "$1_$2")
        .split(/[^A-Za-z0-9]+/)
        .filter((word) => word.length > 0);
}

function isStringEnumDefinition(definition: JSONSchema7): definition is JSONSchema7 & { enum: string[] } {
    return Array.isArray(definition.enum) && definition.enum.every((value) => typeof value === "string");
}

function pushGoComment(lines: string[], text: string, indent = "", wrap = true): void {
    lines.push(...goCommentLines(text, indent, wrap));
}

function pushGoCommentForContext(lines: string[], text: string, ctx: GoCodegenCtx, indent = ""): void {
    pushGoComment(lines, text, indent, ctx.wrapComments !== false);
}

function goExperimentalTypeComment(typeName: string): string {
    return `Experimental: ${typeName} is part of an experimental API and may change or be removed.`;
}

function pushGoExperimentalTypeComment(lines: string[], typeName: string, ctx: GoCodegenCtx): void {
    pushGoCommentForContext(lines, goExperimentalTypeComment(typeName), ctx);
}

function pushGoExperimentalEventComment(lines: string[], constName: string, indent = ""): void {
    pushGoComment(lines, `Experimental: ${constName} identifies an experimental event that may change or be removed.`, indent);
}

function pushGoExperimentalApiComment(lines: string[], name: string, indent = ""): void {
    pushGoComment(lines, `Experimental: ${name} contains experimental APIs that may change or be removed.`, indent);
}

function pushGoExperimentalSubApiComment(lines: string[], name: string, indent = ""): void {
    pushGoComment(lines, `Experimental: ${name} returns experimental APIs that may change or be removed.`, indent);
}

function pushGoExperimentalMethodComment(lines: string[], methodName: string, indent = ""): void {
    pushGoComment(lines, `Experimental: ${methodName} is an experimental API and may change or be removed in future versions.`, indent);
}

function goCommentLines(text: string, indent = "", wrap = true): string[] {
    const prefix = `${indent}//`;
    const lines: string[] = [];

    for (const paragraph of text.split(/\r?\n/)) {
        const trimmed = paragraph.trim();
        if (trimmed.length === 0) {
            lines.push(prefix);
            continue;
        }
        const commentLines = wrap
            ? wrapGoCommentText(trimmed).split("\n").map((wrappedLine: string) => wrappedLine.trim())
            : [trimmed];
        for (const line of commentLines) {
            lines.push(`${prefix} ${line}`);
        }
    }

    return lines;
}

function wrapGeneratedGoComments(code: string): string {
    return code
        .split(/\r?\n/)
        .flatMap((line) => {
            const match = /^(\s*)\/\/\s?(.*)$/.exec(line);
            if (!match) return [line];
            const [, indent, text] = match;
            if (text.length <= goCommentTextWrapLength) return [line];
            return goCommentLines(text, indent);
        })
        .join("\n");
}

interface GoExtractedField {
    name: string;
    type: string;
}

/**
 * Extract a mapping from (structName, jsonFieldName) to generated Go field
 * metadata so wrapper code can reference emitted field names and nil behavior.
 */
function extractFields(generatedTypeCode: string): Map<string, Map<string, GoExtractedField>> {
    const result = new Map<string, Map<string, GoExtractedField>>();
    const structRe = /^type\s+(\w+)\s+struct\s*\{([^}]*)\}/gm;
    let sm;
    while ((sm = structRe.exec(generatedTypeCode)) !== null) {
        const [, structName, body] = sm;
        const fields = new Map<string, GoExtractedField>();
        const fieldRe = /^\s+(\w+)\s+([^\s`]+)\s+`json:"([^",]+)/gm;
        let fm;
        while ((fm = fieldRe.exec(body)) !== null) {
            fields.set(fm[3], { name: fm[1], type: fm[2] });
        }
        result.set(structName, fields);
    }
    return result;
}

function goTypeIsPointer(goType: string | undefined): boolean {
    return goType?.startsWith("*") ?? false;
}

function goTypeIsSlice(goType: string | undefined): boolean {
    return goType?.startsWith("[]") ?? false;
}

function goTypeIsMap(goType: string | undefined): boolean {
    return goType?.startsWith("map[") ?? false;
}

function goTypeIsNilable(goType: string | undefined, ctx?: GoCodegenCtx): boolean {
    if (!goType) return false;
    if (goTypeIsPointer(goType) || goTypeIsSlice(goType) || goTypeIsMap(goType)) return true;
    return ctx ? goDiscriminatedUnionInfoForType(goType, ctx) !== undefined : false;
}

function goOptionalFieldNeedsDereference(goType: string | undefined): boolean {
    return goType === undefined || goTypeIsPointer(goType);
}

function goTypeWithOptionalPointer(goType: string, ctx?: GoCodegenCtx): string {
    return goTypeIsNilable(goType, ctx) ? goType : `*${goType}`;
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
    for (const [, value] of sortByPascalName(Object.entries(node))) {
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
    // When a method wrapper is named the same as the referenced schema inside an
    // anyOf/oneOf, store the resolved object shape so the definition map does not
    // create a self-referential alias.
    if ((schema?.anyOf || schema?.oneOf) && resolvedSchema?.properties) {
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
    eventExperimental: boolean;
    dataExperimental: boolean;
}

interface GoEventEnvelopeProperty extends SessionEventEnvelopeProperty {
    fieldName: string;
    typeName: string;
    jsonTag: string;
    description?: string;
}

interface GoDiscriminatedUnionInfo {
    typeName: string;
    unmarshalFuncName: string;
}

type GoDiscriminatorValue = string | boolean;
type GoDiscriminatorValueKind = "string" | "boolean";

interface GoDiscriminatedUnionVariant {
    schema: JSONSchema7;
    typeName: string;
    discriminatorValues: GoDiscriminatorValue[];
}

interface GoDiscriminatorInfo {
    property: string;
    valueKind: GoDiscriminatorValueKind;
    mapping: Map<GoDiscriminatorValue, GoDiscriminatedUnionVariant[]>;
    variants: GoDiscriminatedUnionVariant[];
}

interface GoRequiredFieldDiscriminatorInfo {
    variants: GoDiscriminatedUnionVariant[];
}

interface GoPrimitiveUnionVariant {
    typeName: string;
    goType: string;
}

interface GoUntaggedUnionVariant {
    typeName: string;
    goType: string;
    jsonKind: string;
    typeDefinition?: string;
    returnExpr: string;
}

type GoUnionPlan =
    | { kind: "discriminated"; typeName: string; schema: JSONSchema7; description?: string; discriminator: GoDiscriminatorInfo }
    | { kind: "requiredFieldDiscriminated"; typeName: string; schema: JSONSchema7; description?: string; discriminator: GoRequiredFieldDiscriminatorInfo }
    | { kind: "primitive"; typeName: string; schema: JSONSchema7; description?: string; variants: GoPrimitiveUnionVariant[] }
    | { kind: "flattenedObject"; typeName: string; schema: JSONSchema7; description?: string; variants: JSONSchema7[] }
    | { kind: "untagged"; typeName: string; schema: JSONSchema7; description?: string; variants: GoUntaggedUnionVariant[] }
    | { kind: "wrapper"; typeName: string; schema: JSONSchema7; description?: string };

interface GoCodegenCtx {
    structs: string[];
    encoding: string[];
    enums: string[];
    enumsByName: Map<string, string>; // enumName → enumName (dedup by type name, not values)
    discriminatedUnions: Map<string, GoDiscriminatedUnionInfo>;
    generatedNames: Set<string>;
    definitions?: DefinitionCollections;
    wrapComments?: boolean;
    discriminatedUnionRawVariantSuffix?: string;
    skipDefinitionTypeNames?: Set<string>;
}

function extractGoEventVariants(schema: JSONSchema7): GoEventVariant[] {
    const definitionCollections = collectDefinitionCollections(schema as Record<string, unknown>);
    return getSessionEventVariantSchemas(schema, definitionCollections)
        .map((variant) => {
            const typeSchema = variant.properties!.type as JSONSchema7;
            const typeName = typeSchema?.const as string;
            if (!typeName) throw new Error("Variant must have type.const");
            const dataSchema =
                resolveObjectSchema(variant.properties!.data as JSONSchema7, definitionCollections) ??
                resolveSchema(variant.properties!.data as JSONSchema7, definitionCollections) ??
                ((variant.properties!.data as JSONSchema7) || {});
            return {
                typeName,
                dataClassName: `${toPascalCase(typeName)}Data`,
                dataSchema,
                dataDescription: dataSchema.description,
                eventExperimental: isSchemaExperimental(variant),
                dataExperimental: isSchemaExperimental(dataSchema),
            };
        });
}

function getGoSharedEventEnvelopeProperties(schema: JSONSchema7, ctx: GoCodegenCtx): GoEventEnvelopeProperty[] {
    return getSharedSessionEventEnvelopeProperties(schema, ctx.definitions)
        .map((property) => {
            const { name, schema, required } = property;
            const typeName = resolveGoPropertyType(schema, "SessionEvent", name, required && !getNullableInner(schema), ctx);
            const omit = required ? "" : ",omitempty";

            return {
                name,
                schema,
                required,
                fieldName: toGoFieldName(name),
                typeName,
                jsonTag: `json:"${name}${omit}"`,
                description: schema.description,
            };
        });
}

function emitGoEnvelopeStructField(property: GoEventEnvelopeProperty, includeComment: boolean, wrapComments = true): string[] {
    const lines: string[] = [];
    if (includeComment && property.description) {
        pushGoComment(lines, property.description, "\t", wrapComments);
    }
    lines.push(`\t${property.fieldName} ${property.typeName} \`${property.jsonTag}\``);
    return lines;
}

function sortedGoEventEnvelopeProperties(properties: GoEventEnvelopeProperty[]): GoEventEnvelopeProperty[] {
    return [...properties].sort((left, right) => compareGoFieldNames(left.fieldName, right.fieldName));
}

interface GoDiscriminatorValues {
    kind: GoDiscriminatorValueKind;
    values: GoDiscriminatorValue[];
}

function goDiscriminatorValues(schema: JSONSchema7, ctx: GoCodegenCtx): GoDiscriminatorValues | undefined {
    const stringValues = goStringEnumValues(schema, ctx);
    if (stringValues) return { kind: "string", values: stringValues };

    const booleanValues = goBooleanDiscriminatorValues(schema, ctx);
    if (booleanValues) return { kind: "boolean", values: booleanValues };

    return undefined;
}

/**
 * Find a literal-valued discriminator property shared by all anyOf variants.
 */
function findGoDiscriminator(
    variants: JSONSchema7[],
    ctx: GoCodegenCtx,
    unionTypeName: string
): GoDiscriminatorInfo | null {
    if (variants.length === 0) return null;
    const firstVariant = resolveGoUnionMember(variants[0], ctx.definitions);
    if (!firstVariant.properties) return null;

    for (const [propName, propSchema] of Object.entries(firstVariant.properties)) {
        if (typeof propSchema !== "object") continue;
        const firstDiscriminatorValues = goDiscriminatorValues(propSchema as JSONSchema7, ctx);
        if (!firstDiscriminatorValues || firstDiscriminatorValues.values.length === 0) continue;

        const mapping = new Map<GoDiscriminatorValue, GoDiscriminatedUnionVariant[]>();
        const unionVariants: GoDiscriminatedUnionVariant[] = [];
        let valid = true;
        for (const variantSource of variants) {
            const variant = resolveGoUnionMember(variantSource, ctx.definitions);
            if (!variant.properties) { valid = false; break; }
            if (!(variant.required || []).includes(propName)) { valid = false; break; }
            const vp = variant.properties[propName];
            if (typeof vp !== "object") { valid = false; break; }
            const discriminatorValues = goDiscriminatorValues(vp as JSONSchema7, ctx);
            if (!discriminatorValues || discriminatorValues.values.length === 0 || discriminatorValues.kind !== firstDiscriminatorValues.kind) { valid = false; break; }
            const dedupedValues = [...new Set(discriminatorValues.values)];
            if (discriminatorValues.kind === "boolean" && dedupedValues.length > 1) { valid = false; break; }
            const unionVariant = {
                schema: variant,
                typeName: goDiscriminatedUnionVariantTypeName(unionTypeName, dedupedValues[0], variantSource, variant, ctx),
                discriminatorValues: dedupedValues,
            };
            unionVariants.push(unionVariant);
            for (const discriminatorValue of dedupedValues) {
                const existing = mapping.get(discriminatorValue) ?? [];
                existing.push(unionVariant);
                mapping.set(discriminatorValue, existing);
            }
        }
        if (valid && mapping.size > 0 && unionVariants.length === variants.length) {
            return { property: propName, valueKind: firstDiscriminatorValues.kind, mapping, variants: unionVariants };
        }
    }
    return null;
}

function findGoRequiredFieldDiscriminator(
    variants: JSONSchema7[],
    ctx: GoCodegenCtx,
    unionTypeName: string
): GoRequiredFieldDiscriminatorInfo | null {
    if (variants.length === 0) return null;

    const objectVariants = variants.map((variantSource) => ({
        source: variantSource,
        schema: goObjectUnionMemberSchema(variantSource, ctx),
    }));
    if (objectVariants.some((variant) => variant.schema === undefined)) return null;

    const requiredSets = objectVariants.map((variant) => new Set(variant.schema!.required || []));
    const propertySets = objectVariants.map((variant) => new Set(Object.keys(variant.schema!.properties || {})));
    const unionVariants: GoDiscriminatedUnionVariant[] = [];
    const seenTypeNames = new Set<string>();
    for (const [index, variant] of objectVariants.entries()) {
        const required = requiredSets[index];
        if (required.size === 0) return null;

        const uniqueRequired = [...required]
            .filter((propName) => !propertySets.some((peerProperties, peerIndex) => peerIndex !== index && peerProperties.has(propName)))
            .sort(compareGoFieldNames);
        if (uniqueRequired.length === 0) return null;

        const typeName = goDiscriminatedUnionVariantTypeName(unionTypeName, uniqueRequired[0], variant.source, variant.schema!, ctx);
        if (seenTypeNames.has(typeName)) return null;
        seenTypeNames.add(typeName);
        unionVariants.push({
            schema: variant.schema!,
            typeName,
            discriminatorValues: uniqueRequired,
        });
    }

    return { variants: unionVariants };
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
    deprecated?: boolean,
    experimental?: boolean
): string {
    const existing = ctx.enumsByName.get(enumName);
    if (existing) return existing;

    const lines: string[] = [];
    if (description) {
        pushGoCommentForContext(lines, description, ctx);
    }
    if (experimental) {
        pushGoExperimentalTypeComment(lines, enumName, ctx);
    }
    if (deprecated) {
        pushGoCommentForContext(lines, `Deprecated: ${enumName} is deprecated and will be removed in a future version.`, ctx);
    }
    lines.push(`type ${enumName} string`);
    lines.push(``);
    lines.push(`const (`);
    const consts = values
        .map((value) => ({ value, constSuffix: goEnumConstSuffix(value) }))
        .sort((left, right) => `${enumName}${left.constSuffix}`.localeCompare(`${enumName}${right.constSuffix}`));
    const usedConstNames = new Map<string, string>();
    for (const { value, constSuffix } of consts) {
        const constName = `${enumName}${constSuffix}`;
        const existingValue = usedConstNames.get(constName);
        if (existingValue !== undefined) {
            throw new Error(
                `Generated Go enum const identifier "${constName}" is not unique for values "${existingValue}" and "${value}". Add an explicit naming rule instead of stabilizing an arbitrary public const name.`
            );
        }
        usedConstNames.set(constName, value);
        lines.push(`\t${constName} ${enumName} = "${value}"`);
    }
    lines.push(`)`);

    ctx.enumsByName.set(enumName, enumName);
    ctx.enums.push(lines.join("\n"));
    return enumName;
}

function goEnumConstSuffix(value: string): string {
    const suffix = splitGoIdentifierWords(value)
        .map((word) =>
            goInitialisms.has(word.toLowerCase())
                ? word.toUpperCase()
                : word.charAt(0).toUpperCase() + word.slice(1)
        )
        .join("");
    return suffix || "Value";
}

function goDiscriminatedUnionVariantTypeName(
    unionTypeName: string,
    discriminatorValue: GoDiscriminatorValue,
    variantSource: JSONSchema7,
    variant: JSONSchema7,
    ctx: GoCodegenCtx
): string {
    if (variantSource.$ref && typeof variantSource.$ref === "string") {
        return goDefinitionName(refTypeName(variantSource.$ref, ctx.definitions));
    }
    const definitionRef = goDefinitionRefForEquivalentSchema(variant, ctx);
    if (definitionRef) {
        return goDefinitionName(refTypeName(definitionRef, ctx.definitions));
    }
    return `${unionTypeName}${goDiscriminatorConstSuffix(discriminatorValue)}`;
}

function goDiscriminatorConstSuffix(value: GoDiscriminatorValue): string {
    return typeof value === "boolean" ? (value ? "True" : "False") : goEnumConstSuffix(value);
}

function compareGoDiscriminatorValues(left: GoDiscriminatorValue, right: GoDiscriminatorValue): number {
    if (typeof left === "boolean" && typeof right === "boolean") {
        return Number(left) - Number(right);
    }
    return String(left).localeCompare(String(right));
}

function goDiscriminatorValueExpr(value: GoDiscriminatorValue, enumName: string | undefined): string {
    if (typeof value === "boolean") return value ? "true" : "false";
    if (!enumName) throw new Error(`Missing enum name for string discriminator value ${value}`);
    return `${enumName}${goEnumConstSuffix(value)}`;
}

function schemaForConstValue(value: unknown): JSONSchema7 {
    if (value === null) return { type: "null" };
    if (Array.isArray(value)) return { type: "array", items: {} };

    switch (typeof value) {
        case "boolean":
            return { type: "boolean" };
        case "number":
            return { type: Number.isInteger(value) ? "integer" : "number" };
        case "string":
            return { type: "string" };
        case "object":
            return { type: "object", additionalProperties: true };
        default:
            return {};
    }
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
                const enumType = getOrCreateGoEnum(typeName, resolved.enum as string[], ctx, resolved.description, isSchemaDeprecated(resolved), isSchemaExperimental(resolved));
                return isRequired ? enumType : `*${enumType}`;
            }
            if (isNamedGoObjectSchema(resolved)) {
                emitGoStruct(typeName, resolved, ctx);
                return isRequired ? typeName : `*${typeName}`;
            }
            const resolvedUnion = resolved as JSONSchema7;
            if (resolvedUnion.anyOf || resolvedUnion.oneOf) {
                emitGoRpcDefinition(refTypeName(propSchema.$ref, ctx.definitions), resolved, ctx);
                if (goDiscriminatedUnionInfoForType(typeName, ctx)) {
                    return typeName;
                }
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
            // Pointer-wrap if not already a pointer, slice, or map
            return goTypeWithOptionalPointer(innerType, ctx);
        }
        const nonNull = (propSchema.anyOf as JSONSchema7[]).filter((s) => s.type !== "null");
        const hasNull = (propSchema.anyOf as JSONSchema7[]).some((s) => s.type === "null");

        if (nonNull.length === 1) {
            // anyOf [T, null] → nullable T
            const innerType = resolveGoPropertyType(nonNull[0], parentTypeName, jsonPropName, true, ctx);
            if (isRequired && !hasNull) return innerType;
            return goTypeWithOptionalPointer(innerType, ctx);
        }

        if (nonNull.length > 1) {
            const unionName = (propSchema.title as string) || nestedName;
            const plan = planGoUnion(unionName, propSchema, ctx);
            if (plan) {
                emitGoUnionPlan(plan, ctx);
                return goUnionPlanPropertyType(plan, isRequired, hasNull);
            }
            // Non-discriminated multi-type union → any
            return "any";
        }
    }

    // Handle enum
    if (propSchema.enum && Array.isArray(propSchema.enum)) {
        const enumType = getOrCreateGoEnum((propSchema.title as string) || nestedName, propSchema.enum as string[], ctx, propSchema.description, isSchemaDeprecated(propSchema), isSchemaExperimental(propSchema));
        return isRequired ? enumType : `*${enumType}`;
    }

    // Handle const values. String consts stay enum-like to preserve generated names for
    // discriminators; other const values use their underlying JSON type.
    if (propSchema.const !== undefined) {
        if (typeof propSchema.const !== "string") {
            return resolveGoPropertyType(schemaForConstValue(propSchema.const), parentTypeName, jsonPropName, isRequired, ctx);
        }
        const enumType = getOrCreateGoEnum((propSchema.title as string) || nestedName, [propSchema.const], ctx, propSchema.description, isSchemaDeprecated(propSchema), isSchemaExperimental(propSchema));
        return isRequired ? enumType : `*${enumType}`;
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
            return goTypeWithOptionalPointer(inner, ctx);
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
            if (items.anyOf) {
                const itemTypeName = (items.title as string) || (nestedName + "Item");
                const plan = planGoUnion(itemTypeName, items, ctx);
                if (plan) {
                    emitGoUnionPlan(plan, ctx);
                    return `[]${goUnionPlanPropertyType(plan, true, false)}`;
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
                let valueType = resolveGoPropertyType(ap, parentTypeName, jsonPropName + "Value", true, ctx);
                const resolvedValueType = ap.$ref ? resolveRef(ap.$ref, ctx.definitions) : undefined;
                if (resolvedValueType?.anyOf || resolvedValueType?.oneOf) {
                    const unionMembers = goNonNullUnionMembers(resolvedValueType)
                        .map((member) => resolveGoUnionMember(member, ctx.definitions));
                    if (!canFlattenGoObjectUnion(unionMembers, ctx) && !goTypeIsNilable(valueType, ctx)) {
                        valueType = `*${valueType}`;
                    }
                }
                return `map[string]${valueType}`;
            }
            return "map[string]any";
        }
        // Empty object or untyped
        return "any";
    }

    return "any";
}

interface GoStructField {
    propName: string;
    goName: string;
    goType: string;
    jsonTag: string;
}

interface GoDiscriminatedUnionField {
    kind: "single" | "slice" | "map";
    unionInfo: GoDiscriminatedUnionInfo;
}

function goUnexportedFunctionName(prefix: string, typeName: string): string {
    return prefix + typeName;
}

function goDiscriminatedUnionInfoForType(typeName: string, ctx: GoCodegenCtx): GoDiscriminatedUnionInfo | undefined {
    return ctx.discriminatedUnions.get(typeName);
}

function goDiscriminatedUnionField(goType: string, ctx: GoCodegenCtx): GoDiscriminatedUnionField | undefined {
    const single = goDiscriminatedUnionInfoForType(goType, ctx);
    if (single) return { kind: "single", unionInfo: single };

    if (goTypeIsSlice(goType)) {
        const itemType = goType.slice(2);
        const item = goDiscriminatedUnionInfoForType(itemType, ctx);
        if (item) return { kind: "slice", unionInfo: item };
    }

    const mapMatch = /^map\[string\](.+)$/.exec(goType);
    if (mapMatch) {
        const value = goDiscriminatedUnionInfoForType(mapMatch[1], ctx);
        if (value) return { kind: "map", unionInfo: value };
    }

    return undefined;
}

function pushGoEncodingBlock(blockLines: string[], ctx: GoCodegenCtx): void {
    if (blockLines.length === 0) return;
    ctx.encoding.push(blockLines.join("\n"));
}

function pushGoStructUnmarshalJSON(lines: string[], typeName: string, fields: GoStructField[], ctx: GoCodegenCtx): void {
    const unionFields = fields
        .map((field) => ({ field, unionField: goDiscriminatedUnionField(field.goType, ctx) }))
        .filter((entry): entry is { field: GoStructField; unionField: GoDiscriminatedUnionField } => entry.unionField !== undefined);
    if (unionFields.length === 0) return;

    const blockLines: string[] = [];
    blockLines.push(`func (r *${typeName}) UnmarshalJSON(data []byte) error {`);
    blockLines.push(`\ttype raw${typeName} struct {`);
    for (const field of fields) {
        const unionField = goDiscriminatedUnionField(field.goType, ctx);
        let rawType = field.goType;
        if (unionField?.kind === "single") rawType = "json.RawMessage";
        if (unionField?.kind === "slice") rawType = "[]json.RawMessage";
        if (unionField?.kind === "map") rawType = "map[string]json.RawMessage";
        blockLines.push(`\t\t${field.goName} ${rawType} \`${field.jsonTag}\``);
    }
    blockLines.push(`\t}`);
    blockLines.push(`\tvar raw raw${typeName}`);
    blockLines.push(`\tif err := json.Unmarshal(data, &raw); err != nil {`);
    blockLines.push(`\t\treturn err`);
    blockLines.push(`\t}`);

    for (const field of fields) {
        const unionField = goDiscriminatedUnionField(field.goType, ctx);
        if (!unionField) {
            blockLines.push(`\tr.${field.goName} = raw.${field.goName}`);
            continue;
        }

        if (unionField.kind === "single") {
            blockLines.push(`\tif raw.${field.goName} != nil {`);
            blockLines.push(`\t\tvalue, err := ${unionField.unionInfo.unmarshalFuncName}(raw.${field.goName})`);
            blockLines.push(`\t\tif err != nil {`);
            blockLines.push(`\t\t\treturn err`);
            blockLines.push(`\t\t}`);
            blockLines.push(`\t\tr.${field.goName} = value`);
            blockLines.push(`\t}`);
        } else if (unionField.kind === "slice") {
            blockLines.push(`\tif raw.${field.goName} != nil {`);
            blockLines.push(`\t\tr.${field.goName} = make([]${unionField.unionInfo.typeName}, 0, len(raw.${field.goName}))`);
            blockLines.push(`\t\tfor _, rawItem := range raw.${field.goName} {`);
            blockLines.push(`\t\t\tvalue, err := ${unionField.unionInfo.unmarshalFuncName}(rawItem)`);
            blockLines.push(`\t\t\tif err != nil {`);
            blockLines.push(`\t\t\t\treturn err`);
            blockLines.push(`\t\t\t}`);
            blockLines.push(`\t\t\tr.${field.goName} = append(r.${field.goName}, value)`);
            blockLines.push(`\t\t}`);
            blockLines.push(`\t}`);
        } else {
            blockLines.push(`\tif raw.${field.goName} != nil {`);
            blockLines.push(`\t\tr.${field.goName} = make(map[string]${unionField.unionInfo.typeName}, len(raw.${field.goName}))`);
            blockLines.push(`\t\tfor key, rawValue := range raw.${field.goName} {`);
            blockLines.push(`\t\t\tvalue, err := ${unionField.unionInfo.unmarshalFuncName}(rawValue)`);
            blockLines.push(`\t\t\tif err != nil {`);
            blockLines.push(`\t\t\t\treturn err`);
            blockLines.push(`\t\t\t}`);
            blockLines.push(`\t\t\tr.${field.goName}[key] = value`);
            blockLines.push(`\t\t}`);
            blockLines.push(`\t}`);
        }
    }
    blockLines.push(`\treturn nil`);
    blockLines.push(`}`);
    pushGoEncodingBlock(blockLines, ctx);
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
        pushGoCommentForContext(lines, desc, ctx);
    }
    if (isSchemaExperimental(schema)) {
        pushGoExperimentalTypeComment(lines, typeName, ctx);
    }
    if (isSchemaDeprecated(schema)) {
        pushGoCommentForContext(lines, `Deprecated: ${typeName} is deprecated and will be removed in a future version.`, ctx);
    }
    lines.push(`type ${typeName} struct {`);

    const fields: GoStructField[] = [];

    for (const [propName, propSchema] of sortByGoFieldName(Object.entries(schema.properties || {}))) {
        if (typeof propSchema !== "object") continue;
        const prop = propSchema as JSONSchema7;
        const isReq = required.has(propName);
        const goName = toGoFieldName(propName);
        const goType = resolveGoPropertyType(prop, typeName, propName, isReq, ctx);
        const omit = isReq ? "" : ",omitempty";

        if (prop.description) {
            pushGoCommentForContext(lines, prop.description, ctx, "\t");
        }
        if (isSchemaDeprecated(prop)) {
            pushGoCommentForContext(lines, `Deprecated: ${goName} is deprecated.`, ctx, "\t");
        }
        const jsonTag = `json:"${propName}${omit}"`;
        lines.push(`\t${goName} ${goType} \`${jsonTag}\``);
        fields.push({ propName, goName, goType, jsonTag });
    }

    lines.push(`}`);
    pushGoStructUnmarshalJSON(lines, typeName, fields, ctx);
    ctx.structs.push(lines.join("\n"));
}

function goObjectSchemaForMatch(schema: JSONSchema7, ctx: GoCodegenCtx): JSONSchema7 | undefined {
    const resolved = resolveSchema(schema, ctx.definitions) ?? schema;
    const objectSchema = resolveObjectSchema(resolved, ctx.definitions) ?? resolved;
    if (objectSchema?.properties || objectSchema?.type === "object" || objectSchema?.additionalProperties === false) {
        return objectSchema;
    }
    return undefined;
}

function goSchemaNeedsJSONMatch(schema: JSONSchema7, ctx: GoCodegenCtx): boolean {
    if (goObjectSchemaForMatch(schema, ctx)) return true;
    return goStringEnumValues(schema, ctx) !== undefined;
}

function pushGoJSONStringMatchLines(
    lines: string[],
    rawExpr: string,
    values: string[],
    indent: string,
    varPrefix: string
): void {
    const stringVar = `${varPrefix}String`;
    lines.push(`${indent}var ${stringVar} string`);
    lines.push(`${indent}if err := json.Unmarshal(${rawExpr}, &${stringVar}); err != nil {`);
    lines.push(`${indent}\treturn false`);
    lines.push(`${indent}}`);
    lines.push(`${indent}switch ${stringVar} {`);
    lines.push(`${indent}case ${[...new Set(values)].sort().map((value) => JSON.stringify(value)).join(", ")}:`);
    lines.push(`${indent}default:`);
    lines.push(`${indent}\treturn false`);
    lines.push(`${indent}}`);
}

function pushGoJSONObjectMatchLines(
    lines: string[],
    schema: JSONSchema7,
    rawVar: string,
    ctx: GoCodegenCtx,
    indent: string,
    varPrefix: string
): void {
    const properties = schema.properties || {};
    const propertyNames = Object.keys(properties).sort();
    const required = [...new Set(schema.required || [])].sort();

    for (const requiredProp of required) {
        lines.push(`${indent}if _, ok := ${rawVar}[${JSON.stringify(requiredProp)}]; !ok {`);
        lines.push(`${indent}\treturn false`);
        lines.push(`${indent}}`);
    }

    if (schema.additionalProperties === false) {
        if (propertyNames.length === 0) {
            lines.push(`${indent}if len(${rawVar}) != 0 {`);
            lines.push(`${indent}\treturn false`);
            lines.push(`${indent}}`);
        } else {
            lines.push(`${indent}for key := range ${rawVar} {`);
            lines.push(`${indent}\tswitch key {`);
            lines.push(`${indent}\tcase ${propertyNames.map((propertyName) => JSON.stringify(propertyName)).join(", ")}:`);
            lines.push(`${indent}\tdefault:`);
            lines.push(`${indent}\t\treturn false`);
            lines.push(`${indent}\t}`);
            lines.push(`${indent}}`);
        }
    }

    for (const [propName, propSchema] of Object.entries(properties).sort(([left], [right]) => left.localeCompare(right))) {
        if (typeof propSchema !== "object") continue;
        const prop = propSchema as JSONSchema7;
        if (!goSchemaNeedsJSONMatch(prop, ctx)) continue;
        const valueVar = `${varPrefix}${toGoFieldName(propName)}`;
        lines.push(`${indent}if ${valueVar}, ok := ${rawVar}[${JSON.stringify(propName)}]; ok {`);
        pushGoJSONSchemaMatchLines(lines, prop, valueVar, ctx, `${indent}\t`, valueVar);
        lines.push(`${indent}}`);
    }
}

function pushGoJSONSchemaMatchLines(
    lines: string[],
    schema: JSONSchema7,
    rawExpr: string,
    ctx: GoCodegenCtx,
    indent: string,
    varPrefix: string
): void {
    const objectSchema = goObjectSchemaForMatch(schema, ctx);
    if (objectSchema) {
        const objectVar = `${varPrefix}Object`;
        lines.push(`${indent}var ${objectVar} map[string]json.RawMessage`);
        lines.push(`${indent}if err := json.Unmarshal(${rawExpr}, &${objectVar}); err != nil {`);
        lines.push(`${indent}\treturn false`);
        lines.push(`${indent}}`);
        pushGoJSONObjectMatchLines(lines, objectSchema, objectVar, ctx, indent, varPrefix);
        return;
    }

    const stringValues = goStringEnumValues(schema, ctx);
    if (stringValues) {
        pushGoJSONStringMatchLines(lines, rawExpr, stringValues, indent, varPrefix);
    }
}

function goVariantMatchFuncName(variantTypeName: string): string {
    return goUnexportedFunctionName("matches", variantTypeName);
}

// Minimal checks used to distinguish variants that share the same discriminator.
// Paths and values come from the JSON schema; these two operation names are the
// only matcher primitives we currently need for const-aware tie breaking.
type GoJSONMatchTerm =
    | { kind: "propertyExists"; path: string[] }
    | { kind: "stringValue"; path: string[]; values: string[] };

interface GoVariantMatchSpec {
    positiveTerms: GoJSONMatchTerm[];
    negativeExistsPaths: string[][];
}

interface GoJSONMatchTermGroup {
    parentPath: string[];
    positiveTerms: GoJSONMatchTerm[];
    negativeProperties: string[];
}

function goJSONMatchPathKey(path: string[]): string {
    return path.join("\0");
}

function goJSONMatchTermKey(term: GoJSONMatchTerm): string {
    const base = `${term.kind}:${goJSONMatchPathKey(term.path)}`;
    if (term.kind === "stringValue") {
        return `${base}:${[...new Set(term.values)].sort().join("\0")}`;
    }
    return base;
}

function dedupeGoJSONMatchTerms(terms: GoJSONMatchTerm[]): GoJSONMatchTerm[] {
    const seen = new Set<string>();
    const result: GoJSONMatchTerm[] = [];
    for (const term of terms) {
        const key = goJSONMatchTermKey(term);
        if (seen.has(key)) continue;
        seen.add(key);
        result.push(term);
    }
    return result;
}

function compareGoJSONPaths(left: string[], right: string[]): number {
    return goJSONMatchPathKey(left).localeCompare(goJSONMatchPathKey(right));
}

function compareGoJSONMatchTerms(left: GoJSONMatchTerm, right: GoJSONMatchTerm): number {
    const pathComparison = compareGoJSONPaths(left.path, right.path);
    if (pathComparison !== 0) return pathComparison;
    return left.kind.localeCompare(right.kind);
}

function goCollectRequiredJSONMatchTerms(
    schema: JSONSchema7,
    ctx: GoCodegenCtx,
    discriminatorProp: string,
    path: string[] = []
): GoJSONMatchTerm[] {
    const objectSchema = goObjectSchemaForMatch(schema, ctx);
    if (!objectSchema) return [];

    const properties = objectSchema.properties || {};
    const terms: GoJSONMatchTerm[] = [];
    for (const propName of [...new Set(objectSchema.required || [])].sort()) {
        if (path.length === 0 && propName === discriminatorProp) continue;
        const propSchema = properties[propName];
        if (typeof propSchema !== "object") continue;

        const propPath = [...path, propName];
        const prop = propSchema as JSONSchema7;
        terms.push({ kind: "propertyExists", path: propPath });

        const stringValues = goStringEnumValues(prop, ctx);
        if (stringValues) {
            terms.push({ kind: "stringValue", path: propPath, values: [...new Set(stringValues)].sort() });
        }

        terms.push(...goCollectRequiredJSONMatchTerms(prop, ctx, discriminatorProp, propPath));
    }

    return dedupeGoJSONMatchTerms(terms);
}

function removeRedundantGoJSONExistsTerms(terms: GoJSONMatchTerm[]): GoJSONMatchTerm[] {
    const stringPaths = new Set(terms
        .filter((term) => term.kind === "stringValue")
        .map((term) => goJSONMatchPathKey(term.path)));
    return terms.filter((term) => term.kind !== "propertyExists" || !stringPaths.has(goJSONMatchPathKey(term.path)));
}

function goVariantTargetedMatchSpec(
    variant: GoDiscriminatedUnionVariant,
    groupVariants: GoDiscriminatedUnionVariant[],
    discriminatorProp: string,
    ctx: GoCodegenCtx
): GoVariantMatchSpec {
    const termsByVariant = new Map<string, GoJSONMatchTerm[]>();
    const termCounts = new Map<string, number>();

    for (const groupVariant of groupVariants) {
        const terms = goCollectRequiredJSONMatchTerms(groupVariant.schema, ctx, discriminatorProp);
        termsByVariant.set(groupVariant.typeName, terms);
        for (const term of terms) {
            const key = goJSONMatchTermKey(term);
            termCounts.set(key, (termCounts.get(key) ?? 0) + 1);
        }
    }

    const variantTerms = termsByVariant.get(variant.typeName) ?? [];
    const uniqueTerms = variantTerms.filter((term) => (termCounts.get(goJSONMatchTermKey(term)) ?? 0) < groupVariants.length);
    const positiveTerms = removeRedundantGoJSONExistsTerms(uniqueTerms).sort(compareGoJSONMatchTerms);

    const variantPositivePathKeys = new Set(positiveTerms.map((term) => goJSONMatchPathKey(term.path)));
    const peerPositivePathKeys = new Set<string>();
    const peerPositivePaths: string[][] = [];
    for (const groupVariant of groupVariants) {
        if (groupVariant.typeName === variant.typeName) continue;
        const groupTerms = termsByVariant.get(groupVariant.typeName) ?? [];
        const peerUniqueTerms = removeRedundantGoJSONExistsTerms(
            groupTerms.filter((term) => (termCounts.get(goJSONMatchTermKey(term)) ?? 0) < groupVariants.length)
        );
        for (const term of peerUniqueTerms) {
            const pathKey = goJSONMatchPathKey(term.path);
            if (variantPositivePathKeys.has(pathKey) || peerPositivePathKeys.has(pathKey)) continue;
            peerPositivePathKeys.add(pathKey);
            peerPositivePaths.push(term.path);
        }
    }

    return {
        positiveTerms,
        negativeExistsPaths: peerPositivePaths.sort(compareGoJSONPaths),
    };
}

function goJSONMatchTermParentPath(term: GoJSONMatchTerm): string[] {
    return term.path.slice(0, -1);
}

function goJSONMatchPathParentPath(path: string[]): string[] {
    return path.slice(0, -1);
}

function goJSONMatchPathProperty(path: string[]): string {
    return path[path.length - 1];
}

function groupGoJSONMatchTerms(spec: GoVariantMatchSpec): GoJSONMatchTermGroup[] {
    const groups = new Map<string, GoJSONMatchTermGroup>();
    const getGroup = (parentPath: string[]): GoJSONMatchTermGroup => {
        const key = goJSONMatchPathKey(parentPath);
        const existing = groups.get(key);
        if (existing) return existing;
        const group = { parentPath, positiveTerms: [], negativeProperties: [] };
        groups.set(key, group);
        return group;
    };

    for (const term of spec.positiveTerms) {
        getGroup(goJSONMatchTermParentPath(term)).positiveTerms.push(term);
    }
    for (const path of spec.negativeExistsPaths) {
        const group = getGroup(goJSONMatchPathParentPath(path));
        group.negativeProperties.push(goJSONMatchPathProperty(path));
    }

    return [...groups.values()]
        .map((group) => ({
            parentPath: group.parentPath,
            positiveTerms: group.positiveTerms.sort(compareGoJSONMatchTerms),
            negativeProperties: [...new Set(group.negativeProperties)].sort(),
        }))
        .sort((left, right) => compareGoJSONPaths(left.parentPath, right.parentPath));
}

function goJSONRawStructFields(propNames: string[]): Map<string, string> {
    const fieldNames = new Map<string, string>();
    const used = new Set<string>();
    for (const propName of [...new Set(propNames)].sort()) {
        const baseName = toGoFieldName(propName) || "Field";
        let fieldName = baseName;
        let suffix = 2;
        while (used.has(fieldName)) {
            fieldName = `${baseName}${suffix++}`;
        }
        used.add(fieldName);
        fieldNames.set(propName, fieldName);
    }
    return fieldNames;
}

function pushGoJSONRawStructDeclLines(
    lines: string[],
    structVar: string,
    propNames: string[],
    indent: string
): Map<string, string> {
    const fieldNames = goJSONRawStructFields(propNames);
    lines.push(`${indent}var ${structVar} struct {`);
    for (const [propName, fieldName] of fieldNames) {
        lines.push(`${indent}\t${fieldName} json.RawMessage \`json:"${propName}"\``);
    }
    lines.push(`${indent}}`);
    return fieldNames;
}

function pushGoJSONRawStructUnmarshalLines(
    lines: string[],
    rawExpr: string,
    structVar: string,
    propNames: string[],
    indent: string
): Map<string, string> {
    const fieldNames = pushGoJSONRawStructDeclLines(lines, structVar, propNames, indent);
    lines.push(`${indent}if err := json.Unmarshal(${rawExpr}, &${structVar}); err != nil {`);
    lines.push(`${indent}\treturn false`);
    lines.push(`${indent}}`);
    return fieldNames;
}

function goJSONPathVarName(varPrefix: string, path: string[]): string {
    return `${varPrefix}${path.map(toGoFieldName).join("")}`;
}

function pushGoJSONRequiredRawPathLines(
    lines: string[],
    rootRawExpr: string,
    path: string[],
    indent: string,
    varPrefix: string
): string {
    let rawExpr = rootRawExpr;
    for (let index = 0; index < path.length; index++) {
        const structVar = goJSONPathVarName(varPrefix, path.slice(0, index));
        const fieldNames = pushGoJSONRawStructUnmarshalLines(lines, rawExpr, structVar, [path[index]], indent);
        const fieldExpr = `${structVar}.${fieldNames.get(path[index])!}`;
        lines.push(`${indent}if ${fieldExpr} == nil {`);
        lines.push(`${indent}\treturn false`);
        lines.push(`${indent}}`);
        rawExpr = fieldExpr;
    }
    return rawExpr;
}

function pushGoJSONOptionalRawPathLines(
    lines: string[],
    rawExpr: string,
    path: string[],
    indent: string,
    varPrefix: string,
    pushInnerLines: (innerRawExpr: string, innerVarPrefix: string, innerIndent: string) => void,
    pathPrefix: string[] = [],
    requireObject: boolean = true
): void {
    if (path.length === 0) {
        pushInnerLines(rawExpr, goJSONPathVarName(varPrefix, pathPrefix), indent);
        return;
    }

    const [head, ...tail] = path;
    const structVar = goJSONPathVarName(varPrefix, pathPrefix);
    const fieldNames = pushGoJSONRawStructDeclLines(lines, structVar, [head], indent);
    if (requireObject) {
        lines.push(`${indent}if err := json.Unmarshal(${rawExpr}, &${structVar}); err != nil {`);
        lines.push(`${indent}\treturn false`);
        lines.push(`${indent}}`);
        lines.push(`${indent}if ${structVar}.${fieldNames.get(head)!} != nil {`);
    } else {
        lines.push(`${indent}if err := json.Unmarshal(${rawExpr}, &${structVar}); err == nil && ${structVar}.${fieldNames.get(head)!} != nil {`);
    }
    pushGoJSONOptionalRawPathLines(
        lines,
        `${structVar}.${fieldNames.get(head)!}`,
        tail,
        `${indent}\t`,
        varPrefix,
        pushInnerLines,
        [...pathPrefix, head],
        false
    );
    lines.push(`${indent}}`);
}

function pushGoJSONPositiveTermLines(
    lines: string[],
    structVar: string,
    fieldNames: Map<string, string>,
    term: GoJSONMatchTerm,
    indent: string,
    varPrefix: string
): void {
    const propName = goJSONMatchPathProperty(term.path);
    const fieldExpr = `${structVar}.${fieldNames.get(propName)!}`;
    if (term.kind === "propertyExists") {
        lines.push(`${indent}if ${fieldExpr} == nil {`);
        lines.push(`${indent}\treturn false`);
        lines.push(`${indent}}`);
        return;
    }

    lines.push(`${indent}if ${fieldExpr} == nil {`);
    lines.push(`${indent}\treturn false`);
    lines.push(`${indent}}`);
    pushGoJSONStringMatchLines(lines, fieldExpr, term.values, indent, varPrefix);
}

function pushGoJSONNegativePropertyLines(
    lines: string[],
    structVar: string,
    fieldNames: Map<string, string>,
    properties: string[],
    indent: string,
    emitFinalReturn: boolean = false
): string | undefined {
    const propertyChecks = emitFinalReturn ? properties.slice(0, -1) : properties;
    for (const propName of propertyChecks) {
        lines.push(`${indent}if ${structVar}.${fieldNames.get(propName)!} != nil {`);
        lines.push(`${indent}\treturn false`);
        lines.push(`${indent}}`);
    }
    if (!emitFinalReturn || properties.length === 0) return undefined;
    return `${structVar}.${fieldNames.get(properties[properties.length - 1])!} == nil`;
}

function pushGoJSONTargetedMatchSpecLines(
    lines: string[],
    rootRawExpr: string,
    spec: GoVariantMatchSpec,
    indent: string
): string | undefined {
    const groups = groupGoJSONMatchTerms(spec);
    for (const [index, group] of groups.entries()) {
        const emitFinalReturn = index === groups.length - 1;
        const groupVarPrefix = `rawGroup${index}`;
        const groupProperties = [
            ...group.positiveTerms.map((term) => goJSONMatchPathProperty(term.path)),
            ...group.negativeProperties,
        ];
        if (group.positiveTerms.length > 0) {
            const rawExpr = pushGoJSONRequiredRawPathLines(lines, rootRawExpr, group.parentPath, indent, groupVarPrefix);
            const structVar = goJSONPathVarName(groupVarPrefix, group.parentPath);
            const fieldNames = pushGoJSONRawStructUnmarshalLines(lines, rawExpr, structVar, groupProperties, indent);
            for (const term of group.positiveTerms) {
                pushGoJSONPositiveTermLines(lines, structVar, fieldNames, term, indent, groupVarPrefix);
            }
            const finalReturn = pushGoJSONNegativePropertyLines(lines, structVar, fieldNames, group.negativeProperties, indent, emitFinalReturn);
            if (finalReturn) return finalReturn;
            continue;
        }

        if (group.parentPath.length === 0) {
            const structVar = goJSONPathVarName(groupVarPrefix, group.parentPath);
            const fieldNames = pushGoJSONRawStructUnmarshalLines(lines, rootRawExpr, structVar, groupProperties, indent);
            const finalReturn = pushGoJSONNegativePropertyLines(lines, structVar, fieldNames, group.negativeProperties, indent, emitFinalReturn);
            if (finalReturn) return finalReturn;
            continue;
        }

        pushGoJSONOptionalRawPathLines(lines, rootRawExpr, group.parentPath, indent, groupVarPrefix, (rawExpr, structVar, innerIndent) => {
            const fieldNames = pushGoJSONRawStructDeclLines(lines, structVar, groupProperties, innerIndent);
            lines.push(`${innerIndent}if err := json.Unmarshal(${rawExpr}, &${structVar}); err == nil {`);
            pushGoJSONNegativePropertyLines(lines, structVar, fieldNames, group.negativeProperties, `${innerIndent}\t`);
            lines.push(`${innerIndent}}`);
        });
    }
    return undefined;
}

function goVariantMatchFunctionLines(
    variant: GoDiscriminatedUnionVariant,
    groupVariants: GoDiscriminatedUnionVariant[],
    discriminatorProp: string,
    ctx: GoCodegenCtx
): string[] {
    const lines: string[] = [];
    lines.push(`func ${goVariantMatchFuncName(variant.typeName)}(data []byte) bool {`);
    const spec = goVariantTargetedMatchSpec(variant, groupVariants, discriminatorProp, ctx);
    if (spec.positiveTerms.length === 0 && spec.negativeExistsPaths.length === 0) {
        pushGoJSONSchemaMatchLines(lines, variant.schema, "data", ctx, "\t", "raw");
        lines.push(`\treturn true`);
        lines.push(`}`);
        return lines;
    }

    const finalReturn = pushGoJSONTargetedMatchSpecLines(lines, "data", spec, "\t");
    lines.push(`\treturn ${finalReturn ?? "true"}`);
    lines.push(`}`);
    return lines;
}

/**
 * Emit a Go interface for a discriminated union (anyOf with const discriminator).
 */
function emitGoFlatDiscriminatedUnion(
    typeName: string,
    discriminator: GoDiscriminatorInfo,
    ctx: GoCodegenCtx,
    description?: string,
    experimental = false
): void {
    if (ctx.generatedNames.has(typeName)) return;
    ctx.generatedNames.add(typeName);

    const discriminatorProp = discriminator.property;
    const mapping = discriminator.mapping;
    const unionVariants = [...discriminator.variants].sort((left, right) => compareGoTypeNames(left.typeName, right.typeName));
    const discGoName = toGoFieldName(discriminatorProp);
    const discriminatorMethodName = discGoName;
    let discEnumName: string | undefined;
    let discGoType = "bool";
    if (discriminator.valueKind === "string") {
        const discValues = [...mapping.keys()].filter((value): value is string => typeof value === "string");
        discEnumName = getOrCreateGoEnum(
            typeName + discGoName,
            discValues,
            ctx,
            `${discGoName} discriminator for ${typeName}.`,
            false,
            experimental
        );
        discGoType = discEnumName;
    }

    const unmarshalFuncName = goUnexportedFunctionName("unmarshal", typeName);
    const rawDataName = `Raw${typeName}${ctx.discriminatedUnionRawVariantSuffix ?? "Data"}`;
    const hasRawVariant = discriminator.valueKind === "string";
    const markerName = `${typeName.charAt(0).toLowerCase()}${typeName.slice(1)}`;
    ctx.discriminatedUnions.set(typeName, { typeName, unmarshalFuncName });

    const lines: string[] = [];
    if (description) {
        pushGoCommentForContext(lines, description, ctx);
    }
    if (experimental) {
        pushGoExperimentalTypeComment(lines, typeName, ctx);
    }
    lines.push(`type ${typeName} interface {`);
    lines.push(`\t${markerName}()`);
    lines.push(`\t${discriminatorMethodName}() ${discGoType}`);
    lines.push(`}`);
    lines.push(``);

    const ambiguousGroupsByVariantTypeName = new Map<string, GoDiscriminatedUnionVariant[]>();
    for (const groupVariants of mapping.values()) {
        if (groupVariants.length <= 1) continue;
        const sortedGroupVariants = [...groupVariants].sort((left, right) => compareGoTypeNames(left.typeName, right.typeName));
        for (const variant of groupVariants) {
            ambiguousGroupsByVariantTypeName.set(variant.typeName, sortedGroupVariants);
        }
    }
    for (const variant of unionVariants) {
        const groupVariants = ambiguousGroupsByVariantTypeName.get(variant.typeName);
        if (groupVariants) {
            pushGoEncodingBlock(goVariantMatchFunctionLines(variant, groupVariants, discriminatorProp, ctx), ctx);
        }
    }

    const unmarshalLines: string[] = [];
    unmarshalLines.push(`func ${unmarshalFuncName}(data []byte) (${typeName}, error) {`);
    unmarshalLines.push(`\tif string(data) == "null" {`);
    unmarshalLines.push(`\t\treturn nil, nil`);
    unmarshalLines.push(`\t}`);
    unmarshalLines.push(`\ttype rawUnion struct {`);
    const rawDiscGoType = discriminator.valueKind === "boolean" ? `*${discGoType}` : discGoType;
    const rawDiscExpr = discriminator.valueKind === "boolean" ? `*raw.${discGoName}` : `raw.${discGoName}`;
    unmarshalLines.push(`\t\t${discGoName} ${rawDiscGoType} \`json:"${discriminatorProp}"\``);
    unmarshalLines.push(`\t}`);
    unmarshalLines.push(`\tvar raw rawUnion`);
    unmarshalLines.push(`\tif err := json.Unmarshal(data, &raw); err != nil {`);
    unmarshalLines.push(`\t\treturn nil, err`);
    unmarshalLines.push(`\t}`);
    if (discriminator.valueKind === "boolean") {
        unmarshalLines.push(`\tif raw.${discGoName} == nil {`);
        unmarshalLines.push(`\t\treturn nil, errors.New("data did not match any union variant for ${typeName}")`);
        unmarshalLines.push(`\t}`);
    }
    unmarshalLines.push(``);
    unmarshalLines.push(`\tswitch ${rawDiscExpr} {`);
    for (const discriminatorValue of [...mapping.keys()].sort(compareGoDiscriminatorValues)) {
        const constName = goDiscriminatorValueExpr(discriminatorValue, discEnumName);
        const mappedVariants = [...mapping.get(discriminatorValue)!].sort((left, right) => compareGoTypeNames(left.typeName, right.typeName));
        unmarshalLines.push(`\tcase ${constName}:`);
        if (mappedVariants.length === 1) {
            const variantTypeName = mappedVariants[0].typeName;
            unmarshalLines.push(`\t\tvar d ${variantTypeName}`);
            unmarshalLines.push(`\t\tif err := json.Unmarshal(data, &d); err != nil {`);
            unmarshalLines.push(`\t\t\treturn nil, err`);
            unmarshalLines.push(`\t\t}`);
            unmarshalLines.push(`\t\treturn &d, nil`);
        } else {
            for (const mappedVariant of mappedVariants) {
                unmarshalLines.push(`\t\tif ${goVariantMatchFuncName(mappedVariant.typeName)}(data) {`);
                unmarshalLines.push(`\t\t\tvar d ${mappedVariant.typeName}`);
                unmarshalLines.push(`\t\t\tif err := json.Unmarshal(data, &d); err != nil {`);
                unmarshalLines.push(`\t\t\t\treturn nil, err`);
                unmarshalLines.push(`\t\t\t}`);
                unmarshalLines.push(`\t\t\treturn &d, nil`);
                unmarshalLines.push(`\t\t}`);
            }
            if (hasRawVariant) {
                unmarshalLines.push(`\t\treturn &${rawDataName}{Discriminator: ${rawDiscExpr}, Raw: data}, nil`);
            } else {
                unmarshalLines.push(`\t\treturn nil, errors.New("data did not match any union variant for ${typeName}")`);
            }
        }
    }
    if (hasRawVariant) {
        unmarshalLines.push(`\tdefault:`);
        unmarshalLines.push(`\t\treturn &${rawDataName}{Discriminator: ${rawDiscExpr}, Raw: data}, nil`);
    }
    unmarshalLines.push(`\t}`);
    if (discriminator.valueKind === "boolean") {
        unmarshalLines.push(`\treturn nil, errors.New("data did not match any union variant for ${typeName}")`);
    }
    unmarshalLines.push(`}`);
    pushGoEncodingBlock(unmarshalLines, ctx);

    if (hasRawVariant) {
        lines.push(`type ${rawDataName} struct {`);
        lines.push(`\tDiscriminator ${discGoType}`);
        lines.push(`\tRaw           json.RawMessage`);
        lines.push(`}`);
        lines.push(``);
        lines.push(`func (${rawDataName}) ${markerName}() {}`);
        lines.push(`func (r ${rawDataName}) ${discriminatorMethodName}() ${discGoType} {`);
        lines.push(`\treturn r.Discriminator`);
        lines.push(`}`);
        pushGoEncodingBlock([
            `func (r ${rawDataName}) MarshalJSON() ([]byte, error) {`,
            `\tif r.Raw != nil {`,
            `\t\treturn r.Raw, nil`,
            `\t}`,
            `\treturn json.Marshal(struct {`,
            `\t\t${discGoName} ${discGoType} \`json:"${discriminatorProp}"\``,
            `\t}{`,
            `\t\t${discGoName}: r.Discriminator,`,
            `\t})`,
            `}`,
        ], ctx);
    }

    for (const mappedVariant of unionVariants) {
        const variant = mappedVariant.schema;
        const variantTypeName = mappedVariant.typeName;
        if (variant.description) {
            pushGoCommentForContext(lines, variant.description, ctx);
        }
        ctx.generatedNames.add(variantTypeName);
        lines.push(`type ${variantTypeName} struct {`);
        const required = new Set(variant.required || []);
        const fields: GoStructField[] = [];
        for (const [propName, propSchema] of sortByGoFieldName(Object.entries(variant.properties || {}))) {
            if (typeof propSchema !== "object") continue;
            const prop = propSchema as JSONSchema7;
            if (propName === discriminatorProp) {
                if (mappedVariant.discriminatorValues.length <= 1) continue;
                const goType = resolveGoPropertyType(prop, variantTypeName, propName, true, ctx);
                const jsonTag = `json:"${propName},omitempty"`;
                lines.push(`\tDiscriminator ${goType} \`${jsonTag}\``);
                fields.push({ propName, goName: "Discriminator", goType, jsonTag });
                continue;
            }
            const goName = toGoFieldName(propName);
            const goType = resolveGoPropertyType(prop, variantTypeName, propName, required.has(propName), ctx);
            const omit = required.has(propName) ? "" : ",omitempty";
            if (prop.description) {
                pushGoCommentForContext(lines, prop.description, ctx, "\t");
            }
            if (isSchemaDeprecated(prop)) {
                pushGoCommentForContext(lines, `Deprecated: ${goName} is deprecated.`, ctx, "\t");
            }
            const jsonTag = `json:"${propName}${omit}"`;
            lines.push(`\t${goName} ${goType} \`${jsonTag}\``);
            fields.push({ propName, goName, goType, jsonTag });
        }
        lines.push(`}`);
        pushGoStructUnmarshalJSON(lines, variantTypeName, fields, ctx);
        lines.push(``);
        lines.push(`func (${variantTypeName}) ${markerName}() {}`);
        const defaultConstName = goDiscriminatorValueExpr(mappedVariant.discriminatorValues[0], discEnumName);
        if (mappedVariant.discriminatorValues.length <= 1) {
            lines.push(`func (${variantTypeName}) ${discriminatorMethodName}() ${discGoType} {`);
            lines.push(`\treturn ${defaultConstName}`);
        } else if (discriminator.valueKind === "boolean") {
            lines.push(`func (r ${variantTypeName}) ${discriminatorMethodName}() ${discGoType} {`);
            lines.push(`\treturn r.Discriminator`);
        } else {
            lines.push(`func (r ${variantTypeName}) ${discriminatorMethodName}() ${discGoType} {`);
            lines.push(`\tif r.Discriminator == "" {`);
            lines.push(`\t\treturn ${defaultConstName}`);
            lines.push(`\t}`);
            lines.push(`\treturn ${discGoType}(r.Discriminator)`);
        }
        lines.push(`}`);
        pushGoEncodingBlock([
            `func (r ${variantTypeName}) MarshalJSON() ([]byte, error) {`,
            `\ttype alias ${variantTypeName}`,
            `\treturn json.Marshal(struct {`,
            `\t\t${discGoName} ${discGoType} \`json:"${discriminatorProp}"\``,
            `\t\talias`,
            `\t}{`,
            `\t\t${discGoName}: r.${discriminatorMethodName}(),`,
            `\t\talias: alias(r),`,
            `\t})`,
            `}`,
        ], ctx);
    }

    ctx.structs.push(lines.join("\n"));
}

function emitGoRequiredFieldDiscriminatedUnion(
    typeName: string,
    discriminator: GoRequiredFieldDiscriminatorInfo,
    ctx: GoCodegenCtx,
    description?: string,
    experimental = false
): void {
    if (ctx.generatedNames.has(typeName)) return;
    ctx.generatedNames.add(typeName);

    const unionVariants = [...discriminator.variants].sort((left, right) => compareGoTypeNames(left.typeName, right.typeName));
    const unmarshalFuncName = goUnexportedFunctionName("unmarshal", typeName);
    const rawDataName = `Raw${typeName}${ctx.discriminatedUnionRawVariantSuffix ?? "Data"}`;
    const markerName = `${typeName.charAt(0).toLowerCase()}${typeName.slice(1)}`;
    ctx.discriminatedUnions.set(typeName, { typeName, unmarshalFuncName });

    const lines: string[] = [];
    if (description) {
        pushGoCommentForContext(lines, description, ctx);
    }
    if (experimental) {
        pushGoExperimentalTypeComment(lines, typeName, ctx);
    }
    lines.push(`type ${typeName} interface {`);
    lines.push(`\t${markerName}()`);
    lines.push(`}`);
    lines.push(``);

    for (const variant of unionVariants) {
        pushGoEncodingBlock(goVariantMatchFunctionLines(variant, unionVariants, "", ctx), ctx);
    }

    const unmarshalLines: string[] = [];
    unmarshalLines.push(`func ${unmarshalFuncName}(data []byte) (${typeName}, error) {`);
    unmarshalLines.push(`\tif string(data) == "null" {`);
    unmarshalLines.push(`\t\treturn nil, nil`);
    unmarshalLines.push(`\t}`);
    for (const variant of unionVariants) {
        unmarshalLines.push(`\tif ${goVariantMatchFuncName(variant.typeName)}(data) {`);
        unmarshalLines.push(`\t\tvar d ${variant.typeName}`);
        unmarshalLines.push(`\t\tif err := json.Unmarshal(data, &d); err != nil {`);
        unmarshalLines.push(`\t\t\treturn nil, err`);
        unmarshalLines.push(`\t\t}`);
        unmarshalLines.push(`\t\treturn &d, nil`);
        unmarshalLines.push(`\t}`);
    }
    unmarshalLines.push(`\treturn &${rawDataName}{Raw: data}, nil`);
    unmarshalLines.push(`}`);
    pushGoEncodingBlock(unmarshalLines, ctx);

    lines.push(`type ${rawDataName} struct {`);
    lines.push(`\tRaw json.RawMessage`);
    lines.push(`}`);
    lines.push(``);
    lines.push(`func (${rawDataName}) ${markerName}() {}`);
    pushGoEncodingBlock([
        `func (r ${rawDataName}) MarshalJSON() ([]byte, error) {`,
        `\tif r.Raw != nil {`,
        `\t\treturn r.Raw, nil`,
        `\t}`,
        `\treturn []byte("null"), nil`,
        `}`,
    ], ctx);

    for (const mappedVariant of unionVariants) {
        const variant = mappedVariant.schema;
        const variantTypeName = mappedVariant.typeName;
        if (variant.description) {
            pushGoCommentForContext(lines, variant.description, ctx);
        }
        ctx.generatedNames.add(variantTypeName);
        lines.push(`type ${variantTypeName} struct {`);
        const required = new Set(variant.required || []);
        const fields: GoStructField[] = [];
        for (const [propName, propSchema] of sortByGoFieldName(Object.entries(variant.properties || {}))) {
            if (typeof propSchema !== "object") continue;
            const prop = propSchema as JSONSchema7;
            const goName = toGoFieldName(propName);
            const goType = resolveGoPropertyType(prop, variantTypeName, propName, required.has(propName), ctx);
            const omit = required.has(propName) ? "" : ",omitempty";
            if (prop.description) {
                pushGoCommentForContext(lines, prop.description, ctx, "\t");
            }
            if (isSchemaDeprecated(prop)) {
                pushGoCommentForContext(lines, `Deprecated: ${goName} is deprecated.`, ctx, "\t");
            }
            const jsonTag = `json:"${propName}${omit}"`;
            lines.push(`\t${goName} ${goType} \`${jsonTag}\``);
            fields.push({ propName, goName, goType, jsonTag });
        }
        lines.push(`}`);
        pushGoStructUnmarshalJSON(lines, variantTypeName, fields, ctx);
        lines.push(``);
        lines.push(`func (${variantTypeName}) ${markerName}() {}`);
        lines.push(``);
    }

    ctx.structs.push(lines.join("\n"));
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

function normalizeSchemaForMatch(schema: JSONSchema7, ctx: GoCodegenCtx): unknown {
    const resolved = resolveSchema(schema, ctx.definitions) ?? schema;
    if (Array.isArray(resolved)) {
        return resolved.map((item) => typeof item === "object" && item !== null
            ? normalizeSchemaForMatch(item as JSONSchema7, ctx)
            : item);
    }
    if (!resolved || typeof resolved !== "object") return resolved;

    const entries = Object.entries(resolved)
        .filter(([key]) => !["title", "description", "default"].includes(key))
        .map(([key, value]) => {
            if ((key === "anyOf" || key === "oneOf") && Array.isArray(value)) {
                const members = value
                    .map((member) => normalizeSchemaForMatch(member as JSONSchema7, ctx))
                    .sort((left, right) => stableStringify(left).localeCompare(stableStringify(right)));
                return [key, members] as const;
            }
            if (key === "enum" && Array.isArray(value)) {
                return [key, [...value].sort()] as const;
            }
            if (key === "type" && Array.isArray(value)) {
                return [key, [...value].sort()] as const;
            }
            if (value && typeof value === "object") {
                return [key, normalizeSchemaForMatch(value as JSONSchema7, ctx)] as const;
            }
            return [key, value] as const;
        });

    return Object.fromEntries(entries.sort(([left], [right]) => left.localeCompare(right)));
}

function dedupeGoSchemasForMatch(schemas: JSONSchema7[], ctx: GoCodegenCtx): JSONSchema7[] {
    const seen = new Set<string>();
    const result: JSONSchema7[] = [];
    for (const schema of schemas) {
        const key = stableStringify(normalizeSchemaForMatch(schema, ctx));
        if (seen.has(key)) continue;
        seen.add(key);
        result.push(schema);
    }
    return result;
}

function goDefinitionRefForEquivalentSchema(schema: JSONSchema7, ctx: GoCodegenCtx): string | undefined {
    const schemaKey = stableStringify(normalizeSchemaForMatch(schema, ctx));
    const definitions = {
        ...(ctx.definitions?.definitions ?? {}),
        ...(ctx.definitions?.$defs ?? {}),
    };
    for (const [definitionName, definition] of Object.entries(definitions)) {
        if (!definition || typeof definition !== "object") continue;
        const definitionKey = stableStringify(normalizeSchemaForMatch(definition as JSONSchema7, ctx));
        if (definitionKey === schemaKey) {
            return `#/definitions/${definitionName}`;
        }
    }
    return undefined;
}

function goDefinitionName(definitionName: string): string {
    return toGoSchemaTypeName(definitionName);
}

function goNonNullUnionMembers(schema: JSONSchema7): JSONSchema7[] {
    return ((schema.anyOf ?? schema.oneOf) as JSONSchema7[] | undefined)
        ?.filter((member) => {
            if (!member || typeof member !== "object") return false;
            if (member.type === "null") return false;
            if (member.not && typeof member.not === "object" && Object.keys(member.not).length === 0) return false;
            return true;
        }) ?? [];
}

function collectGoDiscriminatedUnionVariantDefinitionTypeNames(
    definitions: Record<string, JSONSchema7>,
    ctx: GoCodegenCtx
): Set<string> {
    const definitionTypeNames = new Set(Object.keys(definitions).map((definitionName) => goDefinitionName(definitionName)));
    const skipped = new Set<string>();

    for (const [definitionName, schema] of Object.entries(definitions)) {
        const typeName = goDefinitionName(definitionName);
        const effectiveSchema = resolveObjectSchema(schema, ctx.definitions) ?? resolveSchema(schema, ctx.definitions) ?? schema;
        const unionMembers = goNonNullUnionMembers(effectiveSchema);
        if (unionMembers.length === 0) continue;

        const discriminator = findGoDiscriminator(unionMembers, ctx, typeName);
        const requiredFieldDiscriminator = discriminator ? undefined : findGoRequiredFieldDiscriminator(unionMembers, ctx, typeName);
        const variants = discriminator?.variants ?? requiredFieldDiscriminator?.variants;
        if (!variants) continue;

        for (const variant of variants) {
            if (definitionTypeNames.has(variant.typeName)) {
                skipped.add(variant.typeName);
            }
        }
    }

    return skipped;
}

function resolveGoUnionMember(member: JSONSchema7, definitions: DefinitionCollections | undefined): JSONSchema7 {
    if (member.$ref) {
        return resolveRef(member.$ref, definitions) ?? member;
    }
    return member;
}

function goObjectUnionMemberSchema(member: JSONSchema7, ctx: GoCodegenCtx): JSONSchema7 | undefined {
    const resolved = resolveGoUnionMember(member, ctx.definitions);
    const objectSchema = resolveObjectSchema(resolved, ctx.definitions) ?? resolveSchema(resolved, ctx.definitions) ?? resolved;
    if (objectSchema?.properties && (objectSchema.type === "object" || objectSchema.type === undefined)) {
        return objectSchema;
    }
    return undefined;
}

function canFlattenGoObjectUnion(members: JSONSchema7[], ctx: GoCodegenCtx): boolean {
    return members.length > 0 && members.every((member) => goObjectUnionMemberSchema(member, ctx) !== undefined);
}

function goStringEnumValues(schema: JSONSchema7, ctx: GoCodegenCtx): string[] | undefined {
    const resolved = resolveSchema(schema, ctx.definitions) ?? schema;
    if (typeof resolved.const === "string") return [resolved.const];
    if (isStringEnumDefinition(resolved)) return resolved.enum;

    const unionMembers = goNonNullUnionMembers(resolved);
    if (unionMembers.length > 0) {
        const values: string[] = [];
        for (const member of unionMembers) {
            const memberValues = goStringEnumValues(member, ctx);
            if (!memberValues) return undefined;
            values.push(...memberValues);
        }
        return [...new Set(values)];
    }

    return undefined;
}

function goBooleanValues(schema: JSONSchema7, ctx: GoCodegenCtx): boolean[] | undefined {
    const resolved = resolveSchema(schema, ctx.definitions) ?? schema;
    if (typeof resolved.const === "boolean") return [resolved.const];
    if (Array.isArray(resolved.enum) && resolved.enum.every((value) => typeof value === "boolean")) {
        return resolved.enum as boolean[];
    }
    if (resolved.type === "boolean") return [true, false];

    const unionMembers = goNonNullUnionMembers(resolved);
    if (unionMembers.length > 0) {
        const values: boolean[] = [];
        for (const member of unionMembers) {
            const memberValues = goBooleanValues(member, ctx);
            if (!memberValues) return undefined;
            values.push(...memberValues);
        }
        return [...new Set(values)];
    }

    return undefined;
}

function goBooleanDiscriminatorValues(schema: JSONSchema7, ctx: GoCodegenCtx): boolean[] | undefined {
    const resolved = resolveSchema(schema, ctx.definitions) ?? schema;
    if (typeof resolved.const === "boolean") return [resolved.const];
    if (Array.isArray(resolved.enum) && resolved.enum.every((value) => typeof value === "boolean")) {
        return resolved.enum as boolean[];
    }

    const unionMembers = goNonNullUnionMembers(resolved);
    if (unionMembers.length > 0) {
        const values: boolean[] = [];
        for (const member of unionMembers) {
            const memberValues = goBooleanDiscriminatorValues(member, ctx);
            if (!memberValues) return undefined;
            values.push(...memberValues);
        }
        return [...new Set(values)];
    }

    return undefined;
}

function mergeGoFlattenedPropertySchema(
    typeName: string,
    propName: string,
    schemas: JSONSchema7[],
    ctx: GoCodegenCtx
): JSONSchema7 {
    if (schemas.length === 1) return schemas[0];

    const enumValues = schemas.map((schema) => goStringEnumValues(schema, ctx));
    if (enumValues.every((values): values is string[] => values !== undefined)) {
        return {
            type: "string",
            enum: [...new Set(enumValues.flat())],
            title: typeName + toGoFieldName(propName),
        };
    }

    const booleanValues = schemas.map((schema) => goBooleanValues(schema, ctx));
    if (booleanValues.every((values): values is boolean[] => values !== undefined)) {
        return { type: "boolean" };
    }

    const firstSchemaKey = stableStringify(resolveSchema(schemas[0], ctx.definitions) ?? schemas[0]);
    if (schemas.every((schema) => stableStringify(resolveSchema(schema, ctx.definitions) ?? schema) === firstSchemaKey)) {
        return schemas[0];
    }

    const unionSchema = { anyOf: dedupeGoSchemasForMatch(schemas, ctx) };
    const definitionRef = goDefinitionRefForEquivalentSchema(unionSchema, ctx);
    if (definitionRef) return { $ref: definitionRef };

    return unionSchema;
}

function emitGoFlattenedObjectUnion(
    typeName: string,
    variants: JSONSchema7[],
    ctx: GoCodegenCtx,
    description?: string,
    experimental = false
): void {
    if (ctx.generatedNames.has(typeName)) return;
    ctx.generatedNames.add(typeName);

    const objectVariants = variants
        .map((variant) => goObjectUnionMemberSchema(variant, ctx))
        .filter((variant): variant is JSONSchema7 => variant !== undefined);
    const allProps = new Map<string, { schemas: JSONSchema7[]; requiredInAll: boolean; presentCount: number }>();

    for (const variant of objectVariants) {
        const required = new Set(variant.required || []);
        for (const [propName, propSchema] of Object.entries(variant.properties || {})) {
            if (typeof propSchema !== "object") continue;
            const existing = allProps.get(propName);
            if (existing) {
                existing.schemas.push(propSchema as JSONSchema7);
                existing.presentCount++;
                if (!required.has(propName)) {
                    existing.requiredInAll = false;
                }
            } else {
                allProps.set(propName, {
                    schemas: [propSchema as JSONSchema7],
                    requiredInAll: required.has(propName),
                    presentCount: 1,
                });
            }
        }
    }

    const lines: string[] = [];
    if (description) {
        pushGoCommentForContext(lines, description, ctx);
    }
    if (experimental) {
        pushGoExperimentalTypeComment(lines, typeName, ctx);
    }
    lines.push(`type ${typeName} struct {`);

    const fields: GoStructField[] = [];

    for (const [propName, info] of sortByGoFieldName([...allProps.entries()])) {
        const goName = toGoFieldName(propName);
        const mergedSchema = mergeGoFlattenedPropertySchema(typeName, propName, info.schemas, ctx);
        const requiredInAll = info.requiredInAll && info.presentCount === objectVariants.length;
        const goType = resolveGoPropertyType(mergedSchema, typeName, propName, requiredInAll, ctx);
        const omit = requiredInAll ? "" : ",omitempty";
        const description = info.schemas.find((schema) => schema.description)?.description;
        if (description) {
            pushGoCommentForContext(lines, description, ctx, "\t");
        }
        if (info.schemas.some((schema) => isSchemaDeprecated(schema))) {
            pushGoCommentForContext(lines, `Deprecated: ${goName} is deprecated.`, ctx, "\t");
        }
        const jsonTag = `json:"${propName}${omit}"`;
        lines.push(`\t${goName} ${goType} \`${jsonTag}\``);
        fields.push({ propName, goName, goType, jsonTag });
    }

    lines.push(`}`);
    pushGoStructUnmarshalJSON(lines, typeName, fields, ctx);
    ctx.structs.push(lines.join("\n"));
}

function goUnionFieldName(member: JSONSchema7, ctx: GoCodegenCtx): string {
    if (member.$ref) {
        const resolved = resolveRef(member.$ref, ctx.definitions);
        if (resolved?.enum) return "Enum";
        return goDefinitionName(refTypeName(member.$ref, ctx.definitions));
    }

    if (member.enum) return "Enum";

    if (member.type === "object" && member.additionalProperties && typeof member.additionalProperties === "object") {
        const valueSchema = member.additionalProperties as JSONSchema7;
        if (valueSchema.$ref) {
            const resolved = resolveRef(valueSchema.$ref, ctx.definitions);
            if (resolved?.enum) return "EnumMap";
            return `${goDefinitionName(refTypeName(valueSchema.$ref, ctx.definitions))}Map`;
        }
        return `${goPrimitiveUnionFieldName(valueSchema)}Map`;
    }

    if (member.type === "array") {
        const items = member.items && typeof member.items === "object" && !Array.isArray(member.items)
            ? member.items as JSONSchema7
            : undefined;
        return `${items ? goUnionFieldName(items, ctx) : "Any"}Array`;
    }

    return goPrimitiveUnionFieldName(member);
}

function goPrimitiveUnionFieldName(schema: JSONSchema7): string {
    switch (schema.type) {
        case "boolean": return "Bool";
        case "integer": return "Integer";
        case "number": return "Double";
        case "string": return "String";
        case "object": return "Object";
        default: return "Any";
    }
}

function goUnionFieldType(member: JSONSchema7, fieldName: string, parentTypeName: string, ctx: GoCodegenCtx): string {
    const memberType = resolveGoPropertyType(member, parentTypeName, fieldName, true, ctx);
    return goTypeWithOptionalPointer(memberType, ctx);
}

function goUnionFieldMarshalIsSet(fieldName: string, fieldType: string, ctx: GoCodegenCtx): string {
    if (goTypeIsNilable(fieldType, ctx)) {
        return `r.${fieldName} != nil`;
    }
    return "true";
}

function goUnionFieldUnmarshalType(fieldType: string): string {
    if (goTypeIsPointer(fieldType)) {
        return fieldType.slice(1);
    }
    return fieldType;
}

function goUnionFieldUnmarshalAssignment(typeName: string, fieldName: string, fieldType: string): string {
    if (goTypeIsPointer(fieldType)) {
        return `*r = ${typeName}{${fieldName}: &value}`;
    }
    return `*r = ${typeName}{${fieldName}: value}`;
}

function goPrimitiveSchemaTypeName(schema: JSONSchema7, ctx: GoCodegenCtx): string | undefined {
    const resolved = resolveSchema(schema, ctx.definitions) ?? schema;
    switch (resolved.type) {
        case "boolean": return "Boolean";
        case "integer": return "Integer";
        case "number": return "Number";
        case "string": return "String";
        default: return undefined;
    }
}

function goPrimitiveSchemaGoType(schema: JSONSchema7, ctx: GoCodegenCtx): string | undefined {
    const resolved = resolveSchema(schema, ctx.definitions) ?? schema;
    switch (resolved.type) {
        case "boolean": return "bool";
        case "integer": return "int64";
        case "number": return "float64";
        case "string": return "string";
        default: return undefined;
    }
}

function goPrimitiveUnionValueName(member: JSONSchema7, ctx: GoCodegenCtx): string | undefined {
    const resolved = resolveGoUnionMember(member, ctx.definitions);
    if (resolved.enum || resolved.const !== undefined) return undefined;

    if (resolved.type === "array") {
        const items = resolved.items && typeof resolved.items === "object" && !Array.isArray(resolved.items)
            ? resolved.items as JSONSchema7
            : undefined;
        if (!items) return undefined;
        const itemName = goPrimitiveSchemaTypeName(items, ctx);
        return itemName ? `${itemName}Array` : undefined;
    }

    return goPrimitiveSchemaTypeName(resolved, ctx);
}

function goPrimitiveUnionGoType(member: JSONSchema7, ctx: GoCodegenCtx): string | undefined {
    const resolved = resolveGoUnionMember(member, ctx.definitions);
    if (resolved.enum || resolved.const !== undefined) return undefined;

    if (resolved.type === "array") {
        const items = resolved.items && typeof resolved.items === "object" && !Array.isArray(resolved.items)
            ? resolved.items as JSONSchema7
            : undefined;
        if (!items) return undefined;
        const itemType = goPrimitiveSchemaGoType(items, ctx);
        return itemType ? `[]${itemType}` : undefined;
    }

    return goPrimitiveSchemaGoType(resolved, ctx);
}

function goPrimitiveUnionVariantTypeName(typeName: string, valueName: string): string {
    if (typeName.endsWith("FieldValue")) {
        return `${typeName.slice(0, -"FieldValue".length)}${valueName}Value`;
    }
    if (typeName.endsWith("Value")) {
        return `${typeName.slice(0, -"Value".length)}${valueName}Value`;
    }
    if (typeName.endsWith("Result")) {
        return `${typeName.slice(0, -"Result".length)}${valueName}Result`;
    }
    if (typeName.endsWith("Content")) {
        return `${typeName.slice(0, -"Content".length)}${valueName}Content`;
    }
    return `${typeName}${valueName}`;
}

function goPrimitiveUnionVariants(typeName: string, schema: JSONSchema7, ctx: GoCodegenCtx): GoPrimitiveUnionVariant[] | undefined {
    const members = goNonNullUnionMembers(schema);
    if (members.length === 0) return undefined;

    const variants: GoPrimitiveUnionVariant[] = [];
    const seenTypeNames = new Set<string>();
    for (const member of members) {
        const valueName = goPrimitiveUnionValueName(member, ctx);
        const goType = goPrimitiveUnionGoType(member, ctx);
        if (!valueName || !goType) return undefined;

        const variantTypeName = goPrimitiveUnionVariantTypeName(typeName, valueName);
        if (seenTypeNames.has(variantTypeName)) return undefined;
        seenTypeNames.add(variantTypeName);
        variants.push({
            typeName: variantTypeName,
            goType,
        });
    }

    return variants;
}

function emitGoPrimitiveUnionInterface(typeName: string, schema: JSONSchema7, ctx: GoCodegenCtx, variants?: GoPrimitiveUnionVariant[]): boolean {
    if (ctx.generatedNames.has(typeName)) return true;
    variants ??= goPrimitiveUnionVariants(typeName, schema, ctx);
    if (!variants) return false;

    ctx.generatedNames.add(typeName);
    const unmarshalFuncName = goUnexportedFunctionName("unmarshal", typeName);
    const markerName = `${typeName.charAt(0).toLowerCase()}${typeName.slice(1)}`;
    ctx.discriminatedUnions.set(typeName, { typeName, unmarshalFuncName });

    const lines: string[] = [];
    if (schema.description) {
        pushGoCommentForContext(lines, schema.description, ctx);
    }
    if (isSchemaExperimental(schema)) {
        pushGoExperimentalTypeComment(lines, typeName, ctx);
    }
    if (isSchemaDeprecated(schema)) {
        pushGoCommentForContext(lines, `Deprecated: ${typeName} is deprecated and will be removed in a future version.`, ctx);
    }
    lines.push(`type ${typeName} interface {`);
    lines.push(`\t${markerName}()`);
    lines.push(`}`);

    for (const variant of [...variants].sort((left, right) => compareGoTypeNames(left.typeName, right.typeName))) {
        lines.push(``);
        lines.push(`type ${variant.typeName} ${variant.goType}`);
        lines.push(``);
        lines.push(`func (${variant.typeName}) ${markerName}() {}`);
    }

    const unmarshalLines: string[] = [];
    unmarshalLines.push(`func ${unmarshalFuncName}(data []byte) (${typeName}, error) {`);
    unmarshalLines.push(`\tif string(data) == "null" {`);
    unmarshalLines.push(`\t\treturn nil, nil`);
    unmarshalLines.push(`\t}`);
    for (const variant of variants) {
        unmarshalLines.push(`\t{`);
        unmarshalLines.push(`\t\tvar value ${variant.goType}`);
        unmarshalLines.push(`\t\tif err := json.Unmarshal(data, &value); err == nil {`);
        unmarshalLines.push(`\t\t\treturn ${variant.typeName}(value), nil`);
        unmarshalLines.push(`\t\t}`);
        unmarshalLines.push(`\t}`);
    }
    unmarshalLines.push(`\treturn nil, errors.New("data did not match any union variant for ${typeName}")`);
    unmarshalLines.push(`}`);
    pushGoEncodingBlock(unmarshalLines, ctx);

    ctx.structs.push(lines.join("\n"));
    return true;
}

function goSchemaJSONKind(schema: JSONSchema7, ctx: GoCodegenCtx): string | undefined {
    const resolved = resolveGoUnionMember(schema, ctx.definitions);
    if (resolved.const !== undefined) {
        return goSchemaJSONKind(schemaForConstValue(resolved.const), ctx);
    }

    if (Array.isArray(resolved.type)) {
        const nonNullTypes = resolved.type.filter((type) => type !== "null");
        if (nonNullTypes.length === 1) {
            return goSchemaJSONKind({ ...resolved, type: nonNullTypes[0] } as JSONSchema7, ctx);
        }
        return undefined;
    }

    if (goObjectUnionMemberSchema(schema, ctx)) return "object";

    switch (resolved.type) {
        case "array": return "array";
        case "boolean": return "boolean";
        case "integer":
        case "number": return "number";
        case "object": return "object";
        case "string": return "string";
        default: return undefined;
    }
}

function goUntaggedUnionVariant(typeName: string, member: JSONSchema7, ctx: GoCodegenCtx): GoUntaggedUnionVariant | undefined {
    const jsonKind = goSchemaJSONKind(member, ctx);
    if (!jsonKind) return undefined;

    const resolved = resolveGoUnionMember(member, ctx.definitions);
    if (member.$ref && typeof member.$ref === "string") {
        const definitionName = refTypeName(member.$ref, ctx.definitions);
        const variantTypeName = goDefinitionName(definitionName);
        emitGoRpcDefinition(definitionName, resolved, ctx);
        return {
            typeName: variantTypeName,
            goType: variantTypeName,
            jsonKind,
            returnExpr: goObjectUnionMemberSchema(member, ctx) ? "&value" : "value",
        };
    }

    if (resolved.enum && Array.isArray(resolved.enum)) {
        const enumType = getOrCreateGoEnum((resolved.title as string) || `${typeName}Enum`, resolved.enum as string[], ctx, resolved.description, isSchemaDeprecated(resolved));
        return { typeName: enumType, goType: enumType, jsonKind, returnExpr: "value" };
    }

    const primitiveValueName = goPrimitiveUnionValueName(member, ctx);
    const primitiveGoType = goPrimitiveUnionGoType(member, ctx);
    if (primitiveValueName && primitiveGoType) {
        const variantTypeName = goPrimitiveUnionVariantTypeName(typeName, primitiveValueName);
        return {
            typeName: variantTypeName,
            goType: primitiveGoType,
            jsonKind,
            typeDefinition: `type ${variantTypeName} ${primitiveGoType}`,
            returnExpr: `${variantTypeName}(value)`,
        };
    }

    if (jsonKind === "object" && resolved.type === "object" && resolved.additionalProperties && !resolved.properties) {
        const fieldName = goUnionFieldName(resolved, ctx);
        const variantTypeName = `${typeName}${fieldName}`;
        const goType = resolveGoPropertyType(resolved, typeName, fieldName, true, ctx);
        if (!goTypeIsMap(goType)) return undefined;
        return {
            typeName: variantTypeName,
            goType: variantTypeName,
            jsonKind,
            typeDefinition: `type ${variantTypeName} ${goType}`,
            returnExpr: "value",
        };
    }

    if (jsonKind === "object" && (resolved.properties || resolved.additionalProperties === false)) {
        const variantTypeName = (resolved.title as string) || `${typeName}Object`;
        emitGoStruct(variantTypeName, resolved, ctx);
        return { typeName: variantTypeName, goType: variantTypeName, jsonKind, returnExpr: "&value" };
    }

    return undefined;
}

function goUntaggedUnionVariants(typeName: string, schema: JSONSchema7, ctx: GoCodegenCtx): GoUntaggedUnionVariant[] | undefined {
    const members = goNonNullUnionMembers(schema);
    if (members.length === 0) return undefined;

    const variants: GoUntaggedUnionVariant[] = [];
    const seenKinds = new Set<string>();
    const seenTypeNames = new Set<string>();
    for (const member of members) {
        const variant = goUntaggedUnionVariant(typeName, member, ctx);
        if (!variant) return undefined;
        if (seenKinds.has(variant.jsonKind) || seenTypeNames.has(variant.typeName)) return undefined;
        seenKinds.add(variant.jsonKind);
        seenTypeNames.add(variant.typeName);
        variants.push(variant);
    }

    return variants;
}

function emitGoUntaggedUnionInterface(typeName: string, schema: JSONSchema7, ctx: GoCodegenCtx, variants?: GoUntaggedUnionVariant[]): boolean {
    if (ctx.generatedNames.has(typeName)) return true;
    variants ??= goUntaggedUnionVariants(typeName, schema, ctx);
    if (!variants) return false;

    ctx.generatedNames.add(typeName);
    const unmarshalFuncName = goUnexportedFunctionName("unmarshal", typeName);
    const markerName = `${typeName.charAt(0).toLowerCase()}${typeName.slice(1)}`;
    ctx.discriminatedUnions.set(typeName, { typeName, unmarshalFuncName });

    const lines: string[] = [];
    if (schema.description) {
        pushGoCommentForContext(lines, schema.description, ctx);
    }
    if (isSchemaExperimental(schema)) {
        pushGoExperimentalTypeComment(lines, typeName, ctx);
    }
    if (isSchemaDeprecated(schema)) {
        pushGoCommentForContext(lines, `Deprecated: ${typeName} is deprecated and will be removed in a future version.`, ctx);
    }
    lines.push(`type ${typeName} interface {`);
    lines.push(`\t${markerName}()`);
    lines.push(`}`);

    for (const variant of [...variants].sort((left, right) => compareGoTypeNames(left.typeName, right.typeName))) {
        lines.push(``);
        if (variant.typeDefinition) {
            lines.push(variant.typeDefinition);
            lines.push(``);
        }
        lines.push(`func (${variant.typeName}) ${markerName}() {}`);
    }

    const unmarshalLines: string[] = [];
    unmarshalLines.push(`func ${unmarshalFuncName}(data []byte) (${typeName}, error) {`);
    unmarshalLines.push(`\tif string(data) == "null" {`);
    unmarshalLines.push(`\t\treturn nil, nil`);
    unmarshalLines.push(`\t}`);
    for (const variant of variants) {
        unmarshalLines.push(`\t{`);
        unmarshalLines.push(`\t\tvar value ${variant.goType}`);
        unmarshalLines.push(`\t\tif err := json.Unmarshal(data, &value); err == nil {`);
        unmarshalLines.push(`\t\t\treturn ${variant.returnExpr}, nil`);
        unmarshalLines.push(`\t\t}`);
        unmarshalLines.push(`\t}`);
    }
    unmarshalLines.push(`\treturn nil, errors.New("data did not match any union variant for ${typeName}")`);
    unmarshalLines.push(`}`);
    pushGoEncodingBlock(unmarshalLines, ctx);

    ctx.structs.push(lines.join("\n"));
    return true;
}

function planGoUnion(typeName: string, schema: JSONSchema7, ctx: GoCodegenCtx, includeWrapper: boolean = false): GoUnionPlan | undefined {
    const members = goNonNullUnionMembers(schema);
    if (members.length === 0) return undefined;

    const description = (schema as JSONSchema7).description;
    const discriminator = findGoDiscriminator(members, ctx, typeName);
    if (discriminator) {
        return { kind: "discriminated", typeName, schema, description, discriminator };
    }

    const primitiveVariants = goPrimitiveUnionVariants(typeName, schema, ctx);
    if (primitiveVariants) {
        return { kind: "primitive", typeName, schema, description, variants: primitiveVariants };
    }

    const requiredFieldDiscriminator = findGoRequiredFieldDiscriminator(members, ctx, typeName);
    if (requiredFieldDiscriminator) {
        return { kind: "requiredFieldDiscriminated", typeName, schema, description, discriminator: requiredFieldDiscriminator };
    }

    const resolvedVariants = members.map((member) => resolveGoUnionMember(member, ctx.definitions));
    if (canFlattenGoObjectUnion(resolvedVariants, ctx)) {
        return { kind: "flattenedObject", typeName, schema, description, variants: resolvedVariants };
    }

    const untaggedVariants = goUntaggedUnionVariants(typeName, schema, ctx);
    if (untaggedVariants) {
        return { kind: "untagged", typeName, schema, description, variants: untaggedVariants };
    }

    return includeWrapper ? { kind: "wrapper", typeName, schema, description } : undefined;
}

function emitGoUnionPlan(plan: GoUnionPlan, ctx: GoCodegenCtx): void {
    switch (plan.kind) {
        case "discriminated":
            emitGoFlatDiscriminatedUnion(plan.typeName, plan.discriminator, ctx, plan.description, isSchemaExperimental(plan.schema));
            return;
        case "requiredFieldDiscriminated":
            emitGoRequiredFieldDiscriminatedUnion(plan.typeName, plan.discriminator, ctx, plan.description, isSchemaExperimental(plan.schema));
            return;
        case "primitive":
            emitGoPrimitiveUnionInterface(plan.typeName, plan.schema, ctx, plan.variants);
            return;
        case "flattenedObject":
            emitGoFlattenedObjectUnion(plan.typeName, plan.variants, ctx, plan.description, isSchemaExperimental(plan.schema));
            return;
        case "untagged":
            emitGoUntaggedUnionInterface(plan.typeName, plan.schema, ctx, plan.variants);
            return;
        case "wrapper":
            emitGoUnionWrapperStruct(plan.typeName, plan.schema, ctx);
            return;
    }
}

function goUnionPlanPropertyType(plan: GoUnionPlan, isRequired: boolean, hasNull: boolean): string {
    if (plan.kind === "flattenedObject" || plan.kind === "wrapper") {
        return isRequired && !hasNull ? plan.typeName : `*${plan.typeName}`;
    }
    return plan.typeName;
}

function emitGoUnionStruct(typeName: string, schema: JSONSchema7, ctx: GoCodegenCtx): void {
    if (ctx.generatedNames.has(typeName)) return;
    const plan = planGoUnion(typeName, schema, ctx, true);
    if (plan) emitGoUnionPlan(plan, ctx);
}

function emitGoUnionWrapperStruct(typeName: string, schema: JSONSchema7, ctx: GoCodegenCtx): void {
    if (ctx.generatedNames.has(typeName)) return;
    ctx.generatedNames.add(typeName);

    const members = goNonNullUnionMembers(schema);
    const lines: string[] = [];
    if (schema.description) {
        pushGoCommentForContext(lines, schema.description, ctx);
    }
    if (isSchemaExperimental(schema)) {
        pushGoExperimentalTypeComment(lines, typeName, ctx);
    }
    if (isSchemaDeprecated(schema)) {
        pushGoCommentForContext(lines, `Deprecated: ${typeName} is deprecated and will be removed in a future version.`, ctx);
    }
    lines.push(`type ${typeName} struct {`);

    const emittedFields = new Set<string>();
    const fields: { name: string; type: string }[] = [];
    for (const member of members) {
        const fieldNameBase = goUnionFieldName(member, ctx);
        let fieldName = fieldNameBase;
        let suffix = 2;
        while (emittedFields.has(fieldName)) {
            fieldName = `${fieldNameBase}${suffix++}`;
        }
        emittedFields.add(fieldName);
        const fieldType = goUnionFieldType(member, fieldName, typeName, ctx);
        fields.push({ name: fieldName, type: fieldType });
    }

    fields.sort((left, right) => compareGoFieldNames(left.name, right.name));
    for (const field of fields) {
        lines.push(`\t${field.name} ${field.type}`);
    }

    lines.push(`}`);
    const encodingLines: string[] = [];
    encodingLines.push(`func (r ${typeName}) MarshalJSON() ([]byte, error) {`);
    for (const field of fields) {
        encodingLines.push(`\tif ${goUnionFieldMarshalIsSet(field.name, field.type, ctx)} {`);
        encodingLines.push(`\t\treturn json.Marshal(r.${field.name})`);
        encodingLines.push(`\t}`);
    }
    encodingLines.push(`\treturn []byte("null"), nil`);
    encodingLines.push(`}`);
    encodingLines.push(``);
    encodingLines.push(`func (r *${typeName}) UnmarshalJSON(data []byte) error {`);
    encodingLines.push(`\tif string(data) == "null" {`);
    encodingLines.push(`\t\t*r = ${typeName}{}`);
    encodingLines.push(`\t\treturn nil`);
    encodingLines.push(`\t}`);
    for (const field of fields) {
        encodingLines.push(`\t{`);
        encodingLines.push(`\t\tvar value ${goUnionFieldUnmarshalType(field.type)}`);
        encodingLines.push(`\t\tif err := json.Unmarshal(data, &value); err == nil {`);
        encodingLines.push(`\t\t\t${goUnionFieldUnmarshalAssignment(typeName, field.name, field.type)}`);
        encodingLines.push(`\t\t\treturn nil`);
        encodingLines.push(`\t\t}`);
        encodingLines.push(`\t}`);
    }
    encodingLines.push(`\treturn errors.New("data did not match any union variant for ${typeName}")`);
    encodingLines.push(`}`);
    pushGoEncodingBlock(encodingLines, ctx);
    ctx.structs.push(lines.join("\n"));
}

function emitGoAlias(typeName: string, schema: JSONSchema7, ctx: GoCodegenCtx): void {
    if (ctx.generatedNames.has(typeName)) return;
    ctx.generatedNames.add(typeName);

    const lines: string[] = [];
    if (schema.description) {
        pushGoCommentForContext(lines, schema.description, ctx);
    }
    if (isSchemaExperimental(schema)) {
        pushGoExperimentalTypeComment(lines, typeName, ctx);
    }
    if (isSchemaDeprecated(schema)) {
        pushGoCommentForContext(lines, `Deprecated: ${typeName} is deprecated and will be removed in a future version.`, ctx);
    }
    lines.push(`type ${typeName} ${resolveGoPropertyType(schema, typeName, "Value", true, ctx)}`);
    ctx.structs.push(lines.join("\n"));
}

function emitGoRpcDefinition(definitionName: string, schema: JSONSchema7, ctx: GoCodegenCtx): string {
    const typeName = goDefinitionName(definitionName);
    const effectiveSchema = resolveObjectSchema(schema, ctx.definitions) ?? resolveSchema(schema, ctx.definitions) ?? schema;

    if (isStringEnumDefinition(effectiveSchema)) {
        getOrCreateGoEnum(typeName, effectiveSchema.enum, ctx, effectiveSchema.description, isSchemaDeprecated(effectiveSchema), isSchemaExperimental(effectiveSchema));
        return typeName;
    }

    if (isNamedGoObjectSchema(effectiveSchema)) {
        emitGoStruct(typeName, effectiveSchema, ctx);
        return typeName;
    }

    const unionMembers = goNonNullUnionMembers(effectiveSchema);
    if (unionMembers.length > 0) {
        const plan = planGoUnion(typeName, effectiveSchema, ctx, true);
        if (plan) emitGoUnionPlan(plan, ctx);
        return typeName;
    }

    emitGoAlias(typeName, effectiveSchema, ctx);
    return typeName;
}

interface GoGeneratedTypeCode {
    typeCode: string;
    encodingCode: string;
    discriminatedUnions: Map<string, GoDiscriminatedUnionInfo>;
}

function stripTrailingGoWhitespace(code: string): string {
    return code.replace(/[ \t]+$/gm, "");
}

function pushGoCodeBlocks(lines: string[], blocks: Iterable<string>): void {
    for (const block of blocks) {
        lines.push(block);
        lines.push(``);
    }
}

function sortedGoDeclaredTypeBlocks(blocks: string[]): string[] {
    return [...blocks].sort((left, right) => goDeclaredTypeName(left).localeCompare(goDeclaredTypeName(right)));
}

function joinGoCode(lines: string[]): string {
    return lines.join("\n").replace(/\n+$/, "");
}

function goEncodingBlocksCode(blocks: string[] | undefined): string {
    const lines: string[] = [];
    pushGoCodeBlocks(lines, blocks ?? []);
    return joinGoCode(lines);
}

function goDoNotEditHeader(schemaFileName: string): string[] {
    return [
        `// Code generated by scripts/codegen/go.ts; DO NOT EDIT.`,
        `// Source: ${schemaFileName}`,
    ];
}

function goGeneratedEncodingFileCode(schemaFileName: string, packageName: string, generatedEncodingCode: string, wrapComments = false): string {
    const lines: string[] = [];
    lines.push(...goDoNotEditHeader(schemaFileName));
    lines.push(``);
    lines.push(`package ${packageName}`);
    lines.push(``);

    const imports = [`"encoding/json"`];
    if (generatedEncodingCode.includes("errors.")) {
        imports.push(`"errors"`);
    }
    if (generatedEncodingCode.includes("time.Time")) {
        imports.push(`"time"`);
    }
    lines.push(`import (`);
    for (const imp of imports) {
        lines.push(`\t${imp}`);
    }
    lines.push(`)`);
    lines.push(``);
    lines.push(generatedEncodingCode);

    const code = lines.join("\n");
    return wrapComments ? wrapGeneratedGoComments(code) : code;
}

function generateGoRpcTypeCode(definitions: Record<string, JSONSchema7>, definitionCollections: DefinitionCollections): GoGeneratedTypeCode {
    const ctx: GoCodegenCtx = {
        structs: [],
        encoding: [],
        enums: [],
        enumsByName: new Map(),
        discriminatedUnions: new Map(),
        generatedNames: new Set(),
        definitions: definitionCollections,
    };
    ctx.skipDefinitionTypeNames = collectGoDiscriminatedUnionVariantDefinitionTypeNames(definitions, ctx);
    const schemaKeysByTypeName = new Map<string, string>();
    const entries = Object.entries(definitions)
        .sort(([left], [right]) => goDefinitionName(left).localeCompare(goDefinitionName(right)));

    for (const [definitionName, definition] of entries) {
        const typeName = goDefinitionName(definitionName);
        if (ctx.skipDefinitionTypeNames.has(typeName)) continue;
        const schemaKey = stableStringify(resolveSchema(definition, definitionCollections) ?? definition);
        const existingSchemaKey = schemaKeysByTypeName.get(typeName);
        if (existingSchemaKey && existingSchemaKey !== schemaKey) {
            throw new Error(`Conflicting Go RPC type name "${typeName}" for different schemas. Add a schema title/withTypeName to disambiguate.`);
        }
        schemaKeysByTypeName.set(typeName, schemaKey);
        emitGoRpcDefinition(definitionName, definition, ctx);
    }

    const lines: string[] = [];
    pushGoCodeBlocks(lines, sortedGoDeclaredTypeBlocks(ctx.structs));
    pushGoCodeBlocks(lines, sortedGoDeclaredTypeBlocks(ctx.enums));

    return {
        typeCode: joinGoCode(lines),
        encodingCode: goEncodingBlocksCode(ctx.encoding),
        discriminatedUnions: new Map(ctx.discriminatedUnions),
    };
}

function goDeclaredTypeName(code: string): string {
    return /^type\s+(\w+)\b/m.exec(code)?.[1] ?? code;
}

/**
 * Generate the complete Go session-events file content.
 */
function generateGoSessionEventsCode(schema: JSONSchema7): GoGeneratedTypeCode {
    const variants = extractGoEventVariants(schema);
    const ctx: GoCodegenCtx = {
        structs: [],
        encoding: [],
        enums: [],
        enumsByName: new Map(),
        discriminatedUnions: new Map(),
        generatedNames: new Set(),
        definitions: collectDefinitionCollections(schema as Record<string, unknown>),
        wrapComments: false,
        discriminatedUnionRawVariantSuffix: "",
    };
    const envelopeProperties = getGoSharedEventEnvelopeProperties(schema, ctx);
    const sessionEventStructFields = [
        ...envelopeProperties.map((property) => ({
            fieldName: property.fieldName,
            lines: emitGoEnvelopeStructField(property, true, ctx.wrapComments !== false),
        })),
        {
            fieldName: "Data",
            lines: [
                ...goCommentLines("Typed event payload. Use a type switch to access per-event fields.", "\t", ctx.wrapComments !== false),
                `\tData SessionEventData \`json:"-"\``,
            ],
        },
    ].sort((left, right) => compareGoFieldNames(left.fieldName, right.fieldName));
    const rawEventUnmarshalFields = [
        ...envelopeProperties.map((property) => ({
            fieldName: property.fieldName,
            lines: emitGoEnvelopeStructField(property, false, ctx.wrapComments !== false),
        })),
        { fieldName: "Data", lines: [`\tData json.RawMessage \`json:"data"\``] },
        { fieldName: "Type", lines: [`\tType SessionEventType \`json:"type"\``] },
    ].sort((left, right) => compareGoFieldNames(left.fieldName, right.fieldName));
    const rawEventMarshalFields = [
        ...envelopeProperties.map((property) => ({
            fieldName: property.fieldName,
            lines: emitGoEnvelopeStructField(property, false, ctx.wrapComments !== false),
        })),
        { fieldName: "Data", lines: [`\tData any \`json:"data"\``] },
        { fieldName: "Type", lines: [`\tType SessionEventType \`json:"type"\``] },
    ].sort((left, right) => compareGoFieldNames(left.fieldName, right.fieldName));

    // Generate per-event data structs
    const dataStructs: string[] = [];
    for (const variant of variants) {
        const required = new Set(variant.dataSchema.required || []);
        const lines: string[] = [];

        if (variant.dataDescription) {
            pushGoCommentForContext(lines, variant.dataDescription, ctx);
        } else {
            pushGoCommentForContext(lines, `${variant.dataClassName} holds the payload for ${variant.typeName} events.`, ctx);
        }
        if (variant.dataExperimental || isSchemaExperimental(variant.dataSchema)) {
            pushGoExperimentalTypeComment(lines, variant.dataClassName, ctx);
        }
        lines.push(`type ${variant.dataClassName} struct {`);

        const fields: GoStructField[] = [];

        for (const [propName, propSchema] of sortByGoFieldName(Object.entries(variant.dataSchema.properties || {}))) {
            if (typeof propSchema !== "object") continue;
            const prop = propSchema as JSONSchema7;
            const isReq = required.has(propName);
            const goName = toGoFieldName(propName);
            const goType = resolveGoPropertyType(prop, variant.dataClassName, propName, isReq, ctx);
            const omit = isReq ? "" : ",omitempty";

            if (prop.description) {
                pushGoCommentForContext(lines, prop.description, ctx, "\t");
            }
            if (isSchemaDeprecated(prop)) {
                pushGoCommentForContext(lines, `Deprecated: ${goName} is deprecated.`, ctx, "\t");
            }
            const jsonTag = `json:"${propName}${omit}"`;
            lines.push(`\t${goName} ${goType} \`${jsonTag}\``);
            fields.push({ propName, goName, goType, jsonTag });
        }

        lines.push(`}`);
        pushGoStructUnmarshalJSON(lines, variant.dataClassName, fields, ctx);
        lines.push(``);
        const constName = "SessionEventType" + variant.typeName
            .split(/[._]/)
            .map((w) =>
                goInitialisms.has(w.toLowerCase())
                    ? w.toUpperCase()
                    : w.charAt(0).toUpperCase() + w.slice(1)
            )
            .join("");
        lines.push(`func (*${variant.dataClassName}) sessionEventData() {}`);
        lines.push(`func (*${variant.dataClassName}) Type() SessionEventType { return ${constName} }`);

        dataStructs.push(lines.join("\n"));
    }

    // Generate SessionEventType enum
    const eventTypeEnum: string[] = [];
    eventTypeEnum.push(`// SessionEventType identifies the kind of session event.`);
    eventTypeEnum.push(`type SessionEventType string`);
    eventTypeEnum.push(``);
    eventTypeEnum.push(`const (`);
    const eventTypeConsts = variants
        .map((variant) => ({
            constName: "SessionEventType" + variant.typeName
                .split(/[._]/)
                .map((w) =>
                    goInitialisms.has(w.toLowerCase())
                        ? w.toUpperCase()
                        : w.charAt(0).toUpperCase() + w.slice(1)
                )
                .join(""),
            typeName: variant.typeName,
        }))
        .sort((left, right) => left.constName.localeCompare(right.constName));
    for (const { constName, typeName } of eventTypeConsts) {
        const variant = variants.find((candidate) => candidate.typeName === typeName);
        if (variant?.eventExperimental) {
            pushGoExperimentalEventComment(eventTypeEnum, constName, "\t");
        }
        eventTypeEnum.push(`\t${constName} SessionEventType = "${typeName}"`);
    }
    eventTypeEnum.push(`)`);

    const sessionEncoding: string[] = [];

    // Assemble file
    const out: string[] = [];
    out.push(...goDoNotEditHeader("session-events.schema.json"));
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
    out.push(`\tType() SessionEventType`);
    out.push(`}`);
    out.push(``);

    // SessionEvent struct
    out.push(`// SessionEvent represents a single session event with a typed data payload.`);
    out.push(`type SessionEvent struct {`);
    for (const field of sessionEventStructFields) {
        out.push(...field.lines);
    }
    out.push(`}`);
    out.push(``);

    // Marshal
    sessionEncoding.push(`// Marshal serializes the SessionEvent to JSON.`);
    sessionEncoding.push(`func (r *SessionEvent) Marshal() ([]byte, error) {`);
    sessionEncoding.push(`\treturn json.Marshal(r)`);
    sessionEncoding.push(`}`);
    sessionEncoding.push(``);

    const eventCases = variants
        .map((variant) => ({
            constName: "SessionEventType" + variant.typeName
                .split(/[._]/)
                .map((w) =>
                    goInitialisms.has(w.toLowerCase())
                        ? w.toUpperCase()
                        : w.charAt(0).toUpperCase() + w.slice(1)
                )
                .join(""),
            dataClassName: variant.dataClassName,
        }))
        .sort((left, right) => left.constName.localeCompare(right.constName));

    // Type method
    out.push(`// Type returns the event type discriminator derived from Data.`);
    out.push(`func (e SessionEvent) Type() SessionEventType {`);
    out.push(`\tif e.Data == nil {`);
    out.push(`\t\treturn ""`);
    out.push(`\t}`);
    out.push(`\treturn e.Data.Type()`);
    out.push(`}`);
    out.push(``);

    // Custom UnmarshalJSON
    sessionEncoding.push(`func (e *SessionEvent) UnmarshalJSON(data []byte) error {`);
    sessionEncoding.push(`\ttype rawEvent struct {`);
    for (const field of rawEventUnmarshalFields) {
        for (const line of field.lines) {
            sessionEncoding.push(`\t${line}`);
        }
    }
    sessionEncoding.push(`\t}`);
    sessionEncoding.push(`\tvar raw rawEvent`);
    sessionEncoding.push(`\tif err := json.Unmarshal(data, &raw); err != nil {`);
    sessionEncoding.push(`\t\treturn err`);
    sessionEncoding.push(`\t}`);
    for (const property of sortedGoEventEnvelopeProperties(envelopeProperties)) {
        sessionEncoding.push(`\te.${property.fieldName} = raw.${property.fieldName}`);
    }
    sessionEncoding.push(``);
    sessionEncoding.push(`\tswitch raw.Type {`);
    for (const { constName, dataClassName } of eventCases) {
        sessionEncoding.push(`\tcase ${constName}:`);
        sessionEncoding.push(`\t\tvar d ${dataClassName}`);
        sessionEncoding.push(`\t\tif err := json.Unmarshal(raw.Data, &d); err != nil {`);
        sessionEncoding.push(`\t\t\treturn err`);
        sessionEncoding.push(`\t\t}`);
        sessionEncoding.push(`\t\te.Data = &d`);
    }
    sessionEncoding.push(`\tdefault:`);
    sessionEncoding.push(`\t\te.Data = &RawSessionEventData{EventType: raw.Type, Raw: raw.Data}`);
    sessionEncoding.push(`\t}`);
    sessionEncoding.push(`\treturn nil`);
    sessionEncoding.push(`}`);
    sessionEncoding.push(``);

    // Custom MarshalJSON
    sessionEncoding.push(`func (e SessionEvent) MarshalJSON() ([]byte, error) {`);
    sessionEncoding.push(`\ttype rawEvent struct {`);
    for (const field of rawEventMarshalFields) {
        for (const line of field.lines) {
            sessionEncoding.push(`\t${line}`);
        }
    }
    sessionEncoding.push(`\t}`);
    sessionEncoding.push(`\treturn json.Marshal(rawEvent{`);
    const rawEventValues = [
        ...envelopeProperties.map((property) => property.fieldName),
        "Data",
    ].sort(compareGoFieldNames);
    for (const fieldName of rawEventValues) {
        sessionEncoding.push(`\t\t${fieldName}: e.${fieldName},`);
    }
    sessionEncoding.push(`\t\tType:      e.Type(),`);
    sessionEncoding.push(`\t})`);
    sessionEncoding.push(`}`);
    sessionEncoding.push(``);

    // RawSessionEventData for unknown event types
    out.push(`// RawSessionEventData holds unparsed JSON data for unrecognized event types.`);
    out.push(`type RawSessionEventData struct {`);
    out.push(`\tEventType SessionEventType`);
    out.push(`\tRaw       json.RawMessage`);
    out.push(`}`);
    out.push(``);
    out.push(`func (RawSessionEventData) sessionEventData() {}`);
    out.push(`func (r RawSessionEventData) Type() SessionEventType {`);
    out.push(`\treturn r.EventType`);
    out.push(`}`);

    sessionEncoding.push(`// MarshalJSON returns the original raw JSON so round-tripping preserves the payload.`);
    sessionEncoding.push(`func (r RawSessionEventData) MarshalJSON() ([]byte, error) {`);
    sessionEncoding.push(`\tif r.Raw == nil {`);
    sessionEncoding.push(`\t\treturn []byte("null"), nil`);
    sessionEncoding.push(`\t}`);
    sessionEncoding.push(`\treturn r.Raw, nil`);
    sessionEncoding.push(`}`);
    sessionEncoding.push(``);

    // Event type enum
    out.push(eventTypeEnum.join("\n"));
    out.push(``);

    // Per-event data structs
    for (const ds of dataStructs.sort()) {
        out.push(ds);
        out.push(``);
    }

    // Nested structs
    pushGoCodeBlocks(out, sortedGoDeclaredTypeBlocks(ctx.structs));

    // Enums
    pushGoCodeBlocks(out, sortedGoDeclaredTypeBlocks(ctx.enums));

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
    for (const [alias, target] of Object.entries(TYPE_ALIASES).sort(([left], [right]) => left.localeCompare(right))) {
        out.push(`\t${alias} = ${target}`);
    }
    out.push(`)`);
    out.push(``);
    out.push(`// Constant aliases for convenience.`);
    out.push(`const (`);
    for (const [alias, target] of Object.entries(CONST_ALIASES).sort(([left], [right]) => left.localeCompare(right))) {
        out.push(`\t${alias} = ${target}`);
    }
    out.push(`)`);
    out.push(``);

    const encodingOut: string[] = [...sessionEncoding];
    if (encodingOut.length > 0) encodingOut.push("");
    pushGoCodeBlocks(encodingOut, ctx.encoding ?? []);

    return {
        typeCode: joinGoCode(out),
        encodingCode: joinGoCode(encodingOut),
        discriminatedUnions: new Map(ctx.discriminatedUnions),
    };
}

async function generateSessionEvents(schemaPath?: string): Promise<void> {
    console.log("Go: generating session-events...");

    const resolvedPath = schemaPath ?? (await getSessionEventsSchemaPath());
    const schema = cloneSchemaForCodegen(JSON.parse(await fs.readFile(resolvedPath, "utf-8")) as JSONSchema7);
    const processed = postProcessSchema(schema);

    const generatedSessionCode = generateGoSessionEventsCode(processed);
    const generatedTypeCode = stripTrailingGoWhitespace(generatedSessionCode.typeCode);
    const generatedEncodingCode = stripTrailingGoWhitespace(generatedSessionCode.encodingCode);

    const outPath = await writeGeneratedFile("go/zsession_events.go", generatedTypeCode);
    console.log(`  ✓ ${outPath}`);

    await formatGoFile(outPath);

    const encodingOutPath = await writeGeneratedFile("go/zsession_encoding.go", goGeneratedEncodingFileCode("session-events.schema.json", "copilot", generatedEncodingCode));
    console.log(`  ✓ ${encodingOutPath}`);

    await formatGoFile(encodingOutPath);
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
    ].sort((left, right) => left.rpcMethod.localeCompare(right.rpcMethod));

    // Build a combined definition map, including shared API definitions plus
    // method-specific request/result wrapper types.
    rpcDefinitions = collectDefinitionCollections(schema as Record<string, unknown>);
    const allDefinitions: Record<string, JSONSchema7> = {
        ...Object.fromEntries(
            Object.entries(rpcDefinitions.$defs ?? {}).filter(([, value]) => typeof value === "object" && value !== null)
        ) as Record<string, JSONSchema7>,
        ...Object.fromEntries(
            Object.entries(rpcDefinitions.definitions ?? {}).filter(([, value]) => typeof value === "object" && value !== null)
        ) as Record<string, JSONSchema7>,
    };

    for (const method of allMethods) {
        const resultSchema = getMethodResultSchema(method);
        const nullableInner = resultSchema ? getNullableInner(resultSchema) : undefined;
        if (nullableInner) {
            // Nullable results (e.g., *SessionFSError) don't need a wrapper type;
            // the inner type is already in definitions via shared hoisting.
        } else if (isVoidSchema(resultSchema)) {
            // Emit an empty struct for void results (forward-compatible with adding fields later)
            allDefinitions[goResultTypeName(method)] = {
                title: goResultTypeName(method),
                type: "object",
                properties: {},
                additionalProperties: false,
            };
        } else if (method.result) {
            allDefinitions[goResultTypeName(method)] = withRootTitle(
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
                    allDefinitions[goParamsTypeName(method)] = withRootTitle(
                        filtered,
                        goParamsTypeName(method)
                    );
                }
            } else {
                allDefinitions[goParamsTypeName(method)] = withRootTitle(
                    schemaSourceForNamedDefinition(method.params, resolvedParams),
                    goParamsTypeName(method)
                );
            }
        }
    }

    const allDefinitionCollections: DefinitionCollections = {
        definitions: { ...(rpcDefinitions.$defs ?? {}), ...allDefinitions },
        $defs: { ...allDefinitions, ...(rpcDefinitions.$defs ?? {}) },
    };
    rpcDefinitions = allDefinitionCollections;

    // Strip trailing whitespace from generated output (gofmt requirement)
    const generatedRpcCode = generateGoRpcTypeCode(allDefinitions, allDefinitionCollections);
    let generatedTypeCode = stripTrailingGoWhitespace(generatedRpcCode.typeCode);
    const generatedEncodingCode = stripTrailingGoWhitespace(generatedRpcCode.encodingCode);

    // Extract generated type names. Some may differ from toPascalCase due explicit schema titles.
    const actualTypeNames = new Map<string, string>();
    const typeRe = /^type\s+(\w+)\b/gm;
    let sm;
    while ((sm = typeRe.exec(generatedTypeCode)) !== null) {
        actualTypeNames.set(sm[1].toLowerCase(), sm[1]);
    }
    const resolveType = (name: string): string => actualTypeNames.get(name.toLowerCase()) ?? name;

    // Extract field metadata so wrappers use emitted Go names and nil semantics.
    const fields = extractFields(generatedTypeCode);

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
        generatedTypeCode = generatedTypeCode.replace(
            new RegExp(`^(type ${typeName} struct)`, "m"),
            `// ${goExperimentalTypeComment(typeName)}\n$1`
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
        generatedTypeCode = generatedTypeCode.replace(
            new RegExp(`^(type ${typeName} struct)`, "m"),
            `// Deprecated: ${typeName} is deprecated and will be removed in a future version.\n$1`
        );
    }

    // Annotate internal data types (driven by the JSON Schema definition's
    // `visibility: "internal"` flag, set via `.asInternal()` on the Zod source).
    const internalTypeNames = new Set<string>();
    for (const [name, def] of Object.entries(allDefinitions)) {
        if (def && typeof def === "object" && (def as Record<string, unknown>).visibility === "internal") {
            internalTypeNames.add(name);
        }
    }
    for (const typeName of internalTypeNames) {
        generatedTypeCode = generatedTypeCode.replace(
            new RegExp(`^(type ${typeName} struct)`, "m"),
            `// Internal: ${typeName} is an internal SDK API and is not part of the public surface.\n$1`
        );
    }
    // Remove trailing blank lines before appending.
    generatedTypeCode = generatedTypeCode.replace(/\n+$/, "");

    // Build method wrappers
    const lines: string[] = [];
    lines.push(...goDoNotEditHeader("api.schema.json"));
    lines.push(``);
    lines.push(`package rpc`);
    lines.push(``);
    const imports = [`"context"`, `"encoding/json"`];
    if (generatedTypeCode.includes("time.Time")) {
        imports.push(`"time"`);
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

    lines.push(generatedTypeCode);
    lines.push(``);

    // Emit ServerRpc
    if (schema.server) {
        const publicNode = filterNodeByVisibility(schema.server, "public");
        if (publicNode) emitRpcWrapper(lines, publicNode, false, resolveType, fields, generatedRpcCode.discriminatedUnions, "");
        const internalNode = filterNodeByVisibility(schema.server, "internal");
        if (internalNode) emitRpcWrapper(lines, internalNode, false, resolveType, fields, generatedRpcCode.discriminatedUnions, "Internal");
    }

    // Emit SessionRpc
    if (schema.session) {
        const publicNode = filterNodeByVisibility(schema.session, "public");
        if (publicNode) emitRpcWrapper(lines, publicNode, true, resolveType, fields, generatedRpcCode.discriminatedUnions, "");
        const internalNode = filterNodeByVisibility(schema.session, "internal");
        if (internalNode) emitRpcWrapper(lines, internalNode, true, resolveType, fields, generatedRpcCode.discriminatedUnions, "Internal");
    }

    if (schema.clientSession) {
        emitClientSessionApiRegistration(lines, schema.clientSession, resolveType, generatedRpcCode.discriminatedUnions);
    }

    const outPath = await writeGeneratedFile("go/rpc/zrpc.go", wrapGeneratedGoComments(lines.join("\n")));
    console.log(`  ✓ ${outPath}`);

    await formatGoFile(outPath);

    const encodingOutPath = await writeGeneratedFile("go/rpc/zrpc_encoding.go", goGeneratedEncodingFileCode("api.schema.json", "rpc", generatedEncodingCode, true));
    console.log(`  ✓ ${encodingOutPath}`);

    await formatGoFile(encodingOutPath);
}

function emitApiGroup(
    lines: string[],
    apiName: string,
    node: Record<string, unknown>,
    isSession: boolean,
    serviceName: string,
    resolveType: (name: string) => string,
    fields: Map<string, Map<string, GoExtractedField>>,
    unionInfos: Map<string, GoDiscriminatedUnionInfo>,
    groupExperimental: boolean,
    groupDeprecated: boolean = false
): void {
    const subGroups = sortByPascalName(Object.entries(node).filter(([, v]) => typeof v === "object" && v !== null && !isRpcMethod(v)));
    const methods = sortByPascalName(Object.entries(node).filter(([, v]) => isRpcMethod(v)));

    if (groupDeprecated) {
        pushGoComment(lines, `Deprecated: ${apiName} contains deprecated APIs that will be removed in a future version.`);
    }
    if (groupExperimental) {
        pushGoExperimentalApiComment(lines, apiName);
    }
    lines.push(`type ${apiName} ${serviceName}`);
    lines.push(``);

    for (const [key, value] of methods) {
        if (!isRpcMethod(value)) continue;
        emitMethod(lines, apiName, key, value, isSession, resolveType, fields, unionInfos, groupExperimental, false, groupDeprecated);
    }

    for (const [subGroupName, subGroupNode] of subGroups) {
        const subApiName = apiName.replace(/Api$/, "") + toPascalCase(subGroupName) + "Api";
        const subGroupExperimental = isNodeFullyExperimental(subGroupNode as Record<string, unknown>);
        const subGroupDeprecated = isNodeFullyDeprecated(subGroupNode as Record<string, unknown>);
        emitApiGroup(lines, subApiName, subGroupNode as Record<string, unknown>, isSession, serviceName, resolveType, fields, unionInfos, subGroupExperimental, subGroupDeprecated);

        if (subGroupExperimental) {
            pushGoExperimentalSubApiComment(lines, toPascalCase(subGroupName));
        }
        lines.push(`func (s *${apiName}) ${toPascalCase(subGroupName)}() *${subApiName} {`);
        lines.push(`\treturn (*${subApiName})(s)`);
        lines.push(`}`);
        lines.push(``);
    }
}

function emitRpcWrapper(lines: string[], node: Record<string, unknown>, isSession: boolean, resolveType: (name: string) => string, fields: Map<string, Map<string, GoExtractedField>>, unionInfos: Map<string, GoDiscriminatedUnionInfo>, classPrefix: string = ""): void {
    const groups = sortByPascalName(Object.entries(node).filter(([, v]) => typeof v === "object" && v !== null && !isRpcMethod(v)));
    const topLevelMethods = sortByPascalName(Object.entries(node).filter(([, v]) => isRpcMethod(v)));

    const wrapperName = classPrefix + (isSession ? "SessionRpc" : "ServerRpc");
    const apiSuffix = "Api";
    // Lowercase the prefix so the unexported service struct stays unexported in Go.
    const prefixLower = classPrefix ? classPrefix.charAt(0).toLowerCase() + classPrefix.slice(1) : "";
    const serviceName = prefixLower
        ? prefixLower + (isSession ? "SessionApi" : "ServerApi")
        : (isSession ? "sessionApi" : "serverApi");

    // Emit the common service struct (unexported, shared by all API groups via type cast)
    lines.push(`type ${serviceName} struct {`);
    lines.push(`\tclient *jsonrpc2.Client`);
    if (isSession) lines.push(`\tsessionID string`);
    lines.push(`}`);
    lines.push(``);

    // Emit API types for groups
    for (const [groupName, groupNode] of groups) {
        const prefix = classPrefix + (isSession ? "" : "Server");
        const apiName = prefix + toPascalCase(groupName) + apiSuffix;
        const groupExperimental = isNodeFullyExperimental(groupNode as Record<string, unknown>);
        const groupDeprecated = isNodeFullyDeprecated(groupNode as Record<string, unknown>);
        emitApiGroup(lines, apiName, groupNode as Record<string, unknown>, isSession, serviceName, resolveType, fields, unionInfos, groupExperimental, groupDeprecated);
    }

    // Compute field name lengths for gofmt-compatible column alignment
    const groupPascalNames = groups.map(([g]) => toPascalCase(g));
    const allFieldNames = ["common", ...groupPascalNames];
    const maxFieldLen = Math.max(...allFieldNames.map((n) => n.length));
    const pad = (name: string) => name.padEnd(maxFieldLen);

    // Emit wrapper struct
    pushGoComment(
        lines,
        classPrefix === "Internal"
            ? `${wrapperName} provides internal SDK ${isSession ? "session" : "server"}-scoped RPC methods (handshake helpers etc.). Not part of the public API.`
            : `${wrapperName} provides typed ${isSession ? "session" : "server"}-scoped RPC methods.`
    );
    lines.push(`type ${wrapperName} struct {`);
    pushGoComment(lines, `Reuse a single struct instead of allocating one for each service on the heap.`, "\t");
    lines.push(`\t${pad("common")} ${serviceName}`);
    lines.push(``);
    for (const [groupName] of groups) {
        const prefix = classPrefix + (isSession ? "" : "Server");
        lines.push(`\t${pad(toPascalCase(groupName))} *${prefix}${toPascalCase(groupName)}${apiSuffix}`);
    }
    lines.push(`}`);
    lines.push(``);

    // Top-level methods on the wrapper use the common service fields
    for (const [key, value] of topLevelMethods) {
        if (!isRpcMethod(value)) continue;
        emitMethod(lines, wrapperName, key, value, isSession, resolveType, fields, unionInfos, false, true);
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
        const prefix = classPrefix + (isSession ? "" : "Server");
        lines.push(`\tr.${toPascalCase(groupName)} = (*${prefix}${toPascalCase(groupName)}${apiSuffix})(&r.common)`);
    }
    lines.push(`\treturn r`);
    lines.push(`}`);
    lines.push(``);
}

function emitMethod(lines: string[], receiver: string, name: string, method: RpcMethod, isSession: boolean, resolveType: (name: string) => string, fields: Map<string, Map<string, GoExtractedField>>, unionInfos: Map<string, GoDiscriminatedUnionInfo>, groupExperimental = false, isWrapper = false, groupDeprecated = false): void {
    const methodName = toPascalCase(name);
    const resultSchema = getMethodResultSchema(method);
    const nullableInner = resultSchema ? getNullableInner(resultSchema) : undefined;
    const resultType = nullableInner
        ? resolveType(goNullableResultTypeName(method, nullableInner))
        : resolveType(goResultTypeName(method));
    const resultUnion = unionInfos.get(resultType);
    const returnType = resultUnion ? resultType : `*${resultType}`;

    const effectiveParams = getMethodParamsSchema(method);
    const paramProps = effectiveParams?.properties || {};
    const requiredParams = new Set(effectiveParams?.required || []);
    const nonSessionParams = Object.keys(paramProps)
        .filter((k) => k !== "sessionId")
        .sort((left, right) => compareGoFieldNames(toGoFieldName(left), toGoFieldName(right)));
    const hasParams = isSession ? nonSessionParams.length > 0 : hasSchemaPayload(effectiveParams);
    const paramsType = hasParams ? resolveType(goParamsTypeName(method)) : "";
    const hasRequiredNonSessionParams = nonSessionParams.some((name) => requiredParams.has(name));
    const paramsAreOptional = hasParams && !!method.params && !!getNullableInner(method.params) && !hasRequiredNonSessionParams;

    // For wrapper-level methods, access fields through a.common; for service type aliases, use a directly
    const clientRef = isWrapper ? "a.common.client" : "a.client";
    const sessionIDRef = isWrapper ? "a.common.sessionID" : "a.sessionID";

    if (method.deprecated && !groupDeprecated) {
        pushGoComment(lines, `Deprecated: ${methodName} is deprecated and will be removed in a future version.`);
    }
    if (method.stability === "experimental" && !groupExperimental) {
        pushGoExperimentalMethodComment(lines, methodName);
    }
    if (method.visibility === "internal") {
        pushGoComment(lines, `Internal: ${methodName} is part of the SDK's internal handshake/plumbing; external callers should not use it.`);
    }
    const sig = hasParams
        ? `func (a *${receiver}) ${methodName}(ctx context.Context, params ${paramsAreOptional ? "..." : ""}*${paramsType}) (${returnType}, error)`
        : `func (a *${receiver}) ${methodName}(ctx context.Context) (${returnType}, error)`;

    lines.push(sig + ` {`);
    const paramsRef = paramsAreOptional ? "requestParams" : "params";
    if (paramsAreOptional) {
        lines.push(`\tvar requestParams *${paramsType}`);
        lines.push(`\tif len(params) > 0 {`);
        lines.push(`\t\trequestParams = params[0]`);
        lines.push(`\t}`);
    }

    if (isSession) {
        lines.push(`\treq := map[string]any{"sessionId": ${sessionIDRef}}`);
        if (hasParams) {
            lines.push(`\tif ${paramsRef} != nil {`);
            for (const pName of nonSessionParams) {
                const field = fields.get(paramsType)?.get(pName);
                const goField = field?.name ?? toGoFieldName(pName);
                const goType = field?.type;
                const isOptional = !requiredParams.has(pName);
                if (isOptional) {
                    // Optional fields are usually pointers; generated union interfaces, slices,
                    // and maps are nilable values and should be passed through directly.
                    lines.push(`\t\tif ${paramsRef}.${goField} != nil {`);
                    const valueExpr = goOptionalFieldNeedsDereference(goType) ? `*${paramsRef}.${goField}` : `${paramsRef}.${goField}`;
                    lines.push(`\t\t\treq["${pName}"] = ${valueExpr}`);
                    lines.push(`\t\t}`);
                } else {
                    lines.push(`\t\treq["${pName}"] = ${paramsRef}.${goField}`);
                }
            }
            lines.push(`\t}`);
        }
        lines.push(`\traw, err := ${clientRef}.Request("${method.rpcMethod}", req)`);
    } else {
        const arg = hasParams ? paramsRef : "nil";
        lines.push(`\traw, err := ${clientRef}.Request("${method.rpcMethod}", ${arg})`);
    }

    lines.push(`\tif err != nil {`);
    lines.push(`\t\treturn nil, err`);
    lines.push(`\t}`);
    if (resultUnion) {
        lines.push(`\tresult, err := ${resultUnion.unmarshalFuncName}(raw)`);
        lines.push(`\tif err != nil {`);
        lines.push(`\t\treturn nil, err`);
        lines.push(`\t}`);
        lines.push(`\treturn result, nil`);
    } else {
        lines.push(`\tvar result ${resultType}`);
        lines.push(`\tif err := json.Unmarshal(raw, &result); err != nil {`);
        lines.push(`\t\treturn nil, err`);
        lines.push(`\t}`);
        lines.push(`\treturn &result, nil`);
    }
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
    for (const [groupName, groupNode] of sortByPascalName(Object.entries(node))) {
        if (typeof groupNode === "object" && groupNode !== null) {
            groups.push({
                groupName,
                groupNode: groupNode as Record<string, unknown>,
                methods: collectRpcMethods(groupNode as Record<string, unknown>).sort(compareRpcMethodsByGoName),
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

function emitClientSessionApiRegistration(lines: string[], clientSchema: Record<string, unknown>, resolveType: (name: string) => string, unionInfos: Map<string, GoDiscriminatedUnionInfo>): void {
    const groups = collectClientGroups(clientSchema);

    for (const { groupName, groupNode, methods } of groups) {
        const interfaceName = clientHandlerInterfaceName(groupName);
        const groupExperimental = isNodeFullyExperimental(groupNode);
        const groupDeprecated = isNodeFullyDeprecated(groupNode);
        if (groupDeprecated) {
            pushGoComment(lines, `Deprecated: ${interfaceName} contains deprecated APIs that will be removed in a future version.`);
        }
        if (groupExperimental) {
            pushGoExperimentalApiComment(lines, interfaceName);
        }
        lines.push(`type ${interfaceName} interface {`);
        for (const method of methods) {
            if (method.deprecated && !groupDeprecated) {
                pushGoComment(lines, `Deprecated: ${clientHandlerMethodName(method.rpcMethod)} is deprecated and will be removed in a future version.`, "\t");
            }
            if (method.stability === "experimental" && !groupExperimental) {
                pushGoExperimentalMethodComment(lines, clientHandlerMethodName(method.rpcMethod), "\t");
            }
            const paramsType = resolveType(goParamsTypeName(method));
            const resultSchema = getMethodResultSchema(method);
            const nullableInner = resultSchema ? getNullableInner(resultSchema) : undefined;
            const resultType = nullableInner
                ? resolveType(goNullableResultTypeName(method, nullableInner))
                : resolveType(goResultTypeName(method));
            const returnType = unionInfos.has(resultType) ? resultType : `*${resultType}`;
            lines.push(`\t${clientHandlerMethodName(method.rpcMethod)}(request *${paramsType}) (${returnType}, error)`);
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
