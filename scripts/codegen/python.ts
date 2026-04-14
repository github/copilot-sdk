/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Python code generator for session-events and RPC types.
 */

import fs from "fs/promises";
import path from "path";
import type { JSONSchema7 } from "json-schema";
import { fileURLToPath } from "url";
import {
    cloneSchemaForCodegen,
    getApiSchemaPath,
    getRpcSchemaTypeName,
    getSessionEventsSchemaPath,
    hoistTitledSchemas,
    isObjectSchema,
    isVoidSchema,
    isRpcMethod,
    isNodeFullyExperimental,
    postProcessSchema,
    writeGeneratedFile,
    collectDefinitionCollections,
    hasSchemaPayload,
    refTypeName,
    resolveObjectSchema,
    resolveSchema,
    withSharedDefinitions,
    type ApiSchema,
    type DefinitionCollections,
    type RpcMethod,
} from "./utils.js";

// ── Utilities ───────────────────────────────────────────────────────────────

/**
 * Modernize quicktype's Python 3.7 output to Python 3.11+ syntax:
 * - Optional[T] → T | None
 * - List[T] → list[T]
 * - Dict[K, V] → dict[K, V]
 * - Type[T] → type[T]
 * - Callable from collections.abc instead of typing
 * - Clean up unused typing imports
 */
function replaceBalancedBrackets(code: string, prefix: string, replacer: (inner: string) => string): string {
    let result = "";
    let i = 0;
    while (i < code.length) {
        const idx = code.indexOf(prefix + "[", i);
        if (idx === -1) {
            result += code.slice(i);
            break;
        }
        result += code.slice(i, idx);
        const start = idx + prefix.length + 1; // after '['
        let depth = 1;
        let j = start;
        while (j < code.length && depth > 0) {
            if (code[j] === "[") depth++;
            else if (code[j] === "]") depth--;
            j++;
        }
        const inner = code.slice(start, j - 1);
        result += replacer(inner);
        i = j;
    }
    return result;
}

/** Split a string by commas, but only at the top bracket depth (ignores commas inside [...]) */
function splitTopLevelCommas(s: string): string[] {
    const parts: string[] = [];
    let depth = 0;
    let start = 0;
    for (let i = 0; i < s.length; i++) {
        if (s[i] === "[") depth++;
        else if (s[i] === "]") depth--;
        else if (s[i] === "," && depth === 0) {
            parts.push(s.slice(start, i));
            start = i + 1;
        }
    }
    parts.push(s.slice(start));
    return parts;
}

function pyDocstringLiteral(text: string): string {
    const normalized = text
        .split(/\r?\n/)
        .map((line) => line.replace(/\s+$/g, ""))
        .join("\n");
    return JSON.stringify(normalized);
}

function modernizePython(code: string): string {
    // Replace Optional[X] with X | None (handles arbitrarily nested brackets)
    code = replaceBalancedBrackets(code, "Optional", (inner) => `${inner} | None`);

    // Replace Union[X, Y] with X | Y (split only at top-level commas, not inside brackets)
    // Run iteratively to handle nested Union inside Dict/List
    let prev = "";
    while (prev !== code) {
        prev = code;
        code = replaceBalancedBrackets(code, "Union", (inner) => {
            return splitTopLevelCommas(inner).map((s: string) => s.trim()).join(" | ");
        });
    }

    // Replace List[X] with list[X]
    code = code.replace(/\bList\[/g, "list[");

    // Replace Dict[K, V] with dict[K, V]
    code = code.replace(/\bDict\[/g, "dict[");

    // Replace Type[T] with type[T]
    code = code.replace(/\bType\[/g, "type[");

    // Move Callable from typing to collections.abc
    code = code.replace(
        /from typing import (.*), Callable$/m,
        "from typing import $1\nfrom collections.abc import Callable"
    );
    code = code.replace(
        /from typing import Callable, (.*)$/m,
        "from typing import $1\nfrom collections.abc import Callable"
    );

    // Remove now-unused imports from typing (Optional, List, Dict, Type)
    code = code.replace(/from typing import (.+)$/m, (_match, imports: string) => {
        const items = imports.split(",").map((s: string) => s.trim());
        const remove = new Set(["Optional", "List", "Dict", "Type", "Union"]);
        const kept = items.filter((i: string) => !remove.has(i));
        return `from typing import ${kept.join(", ")}`;
    });

    return code;
}

function collapsePlaceholderPythonDataclasses(code: string): string {
    const classBlockRe = /(@dataclass\r?\nclass\s+(\w+):[\s\S]*?)(?=^@dataclass|^class\s+\w+|^def\s+\w+|\Z)/gm;
    const matches = [...code.matchAll(classBlockRe)].map((match) => ({
        fullBlock: match[1],
        name: match[2],
        normalizedBody: normalizePythonDataclassBlock(match[1], match[2]),
    }));
    const groups = new Map<string, typeof matches>();

    for (const match of matches) {
        const group = groups.get(match.normalizedBody) ?? [];
        group.push(match);
        groups.set(match.normalizedBody, group);
    }

    for (const group of groups.values()) {
        if (group.length < 2) continue;

        const canonical = chooseCanonicalPlaceholderDuplicate(group.map(({ name }) => name));
        if (!canonical) continue;

        for (const duplicate of group) {
            if (duplicate.name === canonical) continue;
            if (!isPlaceholderTypeName(duplicate.name)) continue;

            code = code.replace(duplicate.fullBlock, "");
            code = code.replace(new RegExp(`\\b${duplicate.name}\\b`, "g"), canonical);
        }
    }

    return code.replace(/\n{3,}/g, "\n\n");
}

function normalizePythonDataclassBlock(block: string, name: string): string {
    return block
        .replace(/^@dataclass\r?\nclass\s+\w+:/, "@dataclass\nclass:")
        .replace(new RegExp(`\\b${name}\\b`, "g"), "SelfType")
        .split(/\r?\n/)
        .map((line) => line.trim())
        .filter((line) => line.length > 0)
        .join("\n");
}

function chooseCanonicalPlaceholderDuplicate(names: string[]): string | undefined {
    const specificNames = names.filter((name) => !isPlaceholderTypeName(name));
    if (specificNames.length === 0) return undefined;
    return specificNames.sort((left, right) => right.length - left.length || left.localeCompare(right))[0];
}

function isPlaceholderTypeName(name: string): boolean {
    return name.endsWith("Class");
}

function toSnakeCase(s: string): string {
    return s
        .replace(/([a-z])([A-Z])/g, "$1_$2")
        .replace(/[._]/g, "_")
        .toLowerCase();
}

function toPascalCase(s: string): string {
    return s
        .split(/[._]/)
        .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
        .join("");
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

function pythonRequestFallbackName(method: RpcMethod): string {
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

function isNamedPyObjectSchema(schema: JSONSchema7 | undefined): schema is JSONSchema7 {
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

function pythonResultTypeName(method: RpcMethod): string {
    return getRpcSchemaTypeName(getMethodResultSchema(method), toPascalCase(method.rpcMethod) + "Result");
}

function pythonParamsTypeName(method: RpcMethod): string {
    const fallback = pythonRequestFallbackName(method);
    if (method.rpcMethod.startsWith("session.") && method.params?.$ref) {
        return fallback;
    }
    return getRpcSchemaTypeName(getMethodParamsSchema(method), fallback);
}

// ── Session Events ──────────────────────────────────────────────────────────
// ── Session Events (custom codegen — dedicated per-event payload types) ─────

interface PyEventVariant {
    typeName: string;
    dataClassName: string;
    dataSchema: JSONSchema7;
    dataDescription?: string;
}

interface PyResolvedType {
    annotation: string;
    fromExpr: (expr: string) => string;
    toExpr: (expr: string) => string;
}

interface PyCodegenCtx {
    classes: string[];
    enums: string[];
    enumsByName: Map<string, string>;
    generatedNames: Set<string>;
    usesTimedelta: boolean;
    usesIntegerTimedelta: boolean;
    definitions: DefinitionCollections;
}

function toEnumMemberName(value: string): string {
    const cleaned = value
        .replace(/([a-z])([A-Z])/g, "$1_$2")
        .replace(/[^A-Za-z0-9]+/g, "_")
        .replace(/^_+|_+$/g, "")
        .toUpperCase();
    if (!cleaned) {
        return "VALUE";
    }
    return /^[0-9]/.test(cleaned) ? `VALUE_${cleaned}` : cleaned;
}

function wrapParser(resolved: PyResolvedType, arg = "x"): string {
    return `lambda ${arg}: ${resolved.fromExpr(arg)}`;
}

function wrapSerializer(resolved: PyResolvedType, arg = "x"): string {
    return `lambda ${arg}: ${resolved.toExpr(arg)}`;
}

const PY_SESSION_EVENT_TYPE_RENAMES: Record<string, string> = {
    AssistantMessageDataToolRequestsItem: "AssistantMessageToolRequest",
    AssistantMessageDataToolRequestsItemType: "AssistantMessageToolRequestType",
    AssistantUsageDataCopilotUsage: "AssistantUsageCopilotUsage",
    AssistantUsageDataCopilotUsageTokenDetailsItem: "AssistantUsageCopilotUsageTokenDetail",
    AssistantUsageDataQuotaSnapshotsValue: "AssistantUsageQuotaSnapshot",
    CapabilitiesChangedDataUi: "CapabilitiesChangedUI",
    CommandsChangedDataCommandsItem: "CommandsChangedCommand",
    ElicitationCompletedDataAction: "ElicitationCompletedAction",
    ElicitationRequestedDataMode: "ElicitationRequestedMode",
    ElicitationRequestedDataRequestedSchema: "ElicitationRequestedSchema",
    McpOauthRequiredDataStaticClientConfig: "MCPOauthRequiredStaticClientConfig",
    PermissionCompletedDataResultKind: "PermissionCompletedKind",
    PermissionRequestedDataPermissionRequest: "PermissionRequest",
    PermissionRequestedDataPermissionRequestAction: "PermissionRequestMemoryAction",
    PermissionRequestedDataPermissionRequestCommandsItem: "PermissionRequestShellCommand",
    PermissionRequestedDataPermissionRequestDirection: "PermissionRequestMemoryDirection",
    PermissionRequestedDataPermissionRequestPossibleUrlsItem: "PermissionRequestShellPossibleURL",
    SessionCompactionCompleteDataCompactionTokensUsed: "CompactionCompleteCompactionTokensUsed",
    SessionCustomAgentsUpdatedDataAgentsItem: "CustomAgentsUpdatedAgent",
    SessionExtensionsLoadedDataExtensionsItem: "ExtensionsLoadedExtension",
    SessionExtensionsLoadedDataExtensionsItemSource: "ExtensionsLoadedExtensionSource",
    SessionExtensionsLoadedDataExtensionsItemStatus: "ExtensionsLoadedExtensionStatus",
    SessionHandoffDataRepository: "HandoffRepository",
    SessionHandoffDataSourceType: "HandoffSourceType",
    SessionMcpServersLoadedDataServersItem: "MCPServersLoadedServer",
    SessionMcpServersLoadedDataServersItemStatus: "MCPServerStatus",
    SessionShutdownDataCodeChanges: "ShutdownCodeChanges",
    SessionShutdownDataModelMetricsValue: "ShutdownModelMetric",
    SessionShutdownDataModelMetricsValueRequests: "ShutdownModelMetricRequests",
    SessionShutdownDataModelMetricsValueUsage: "ShutdownModelMetricUsage",
    SessionShutdownDataShutdownType: "ShutdownType",
    SessionSkillsLoadedDataSkillsItem: "SkillsLoadedSkill",
    UserMessageDataAgentMode: "UserMessageAgentMode",
    UserMessageDataAttachmentsItem: "UserMessageAttachment",
    UserMessageDataAttachmentsItemLineRange: "UserMessageAttachmentFileLineRange",
    UserMessageDataAttachmentsItemReferenceType: "UserMessageAttachmentGithubReferenceType",
    UserMessageDataAttachmentsItemSelection: "UserMessageAttachmentSelectionDetails",
    UserMessageDataAttachmentsItemSelectionEnd: "UserMessageAttachmentSelectionDetailsEnd",
    UserMessageDataAttachmentsItemSelectionStart: "UserMessageAttachmentSelectionDetailsStart",
    UserMessageDataAttachmentsItemType: "UserMessageAttachmentType",
};

function postProcessPythonSessionEventCode(code: string): string {
    for (const [from, to] of Object.entries(PY_SESSION_EVENT_TYPE_RENAMES).sort(
        ([left], [right]) => right.length - left.length
    )) {
        code = code.replace(new RegExp(`\\b${from}\\b`, "g"), to);
    }
    return code;
}

function pyPrimitiveResolvedType(annotation: string, fromFn: string, toFn = fromFn): PyResolvedType {
    return {
        annotation,
        fromExpr: (expr) => `${fromFn}(${expr})`,
        toExpr: (expr) => `${toFn}(${expr})`,
    };
}

function pyOptionalResolvedType(inner: PyResolvedType): PyResolvedType {
    return {
        annotation: `${inner.annotation} | None`,
        fromExpr: (expr) => `from_union([from_none, ${wrapParser(inner)}], ${expr})`,
        toExpr: (expr) => `from_union([from_none, ${wrapSerializer(inner)}], ${expr})`,
    };
}

function pyAnyResolvedType(): PyResolvedType {
    return {
        annotation: "Any",
        fromExpr: (expr) => expr,
        toExpr: (expr) => expr,
    };
}

function pyDurationResolvedType(ctx: PyCodegenCtx, isInteger: boolean): PyResolvedType {
    ctx.usesTimedelta = true;
    if (isInteger) {
        ctx.usesIntegerTimedelta = true;
    }
    return {
        annotation: "timedelta",
        fromExpr: (expr) => `from_timedelta(${expr})`,
        toExpr: (expr) => (isInteger ? `to_timedelta_int(${expr})` : `to_timedelta(${expr})`),
    };
}

function isPyBase64StringSchema(schema: JSONSchema7): boolean {
    return schema.format === "byte" || (schema as Record<string, unknown>).contentEncoding === "base64";
}

function toPythonLiteral(value: unknown): string | undefined {
    if (typeof value === "string") {
        return JSON.stringify(value);
    }
    if (typeof value === "number") {
        return Number.isFinite(value) ? String(value) : undefined;
    }
    if (typeof value === "boolean") {
        return value ? "True" : "False";
    }
    if (value === null) {
        return "None";
    }
    return undefined;
}

function extractPyEventVariants(schema: JSONSchema7): PyEventVariant[] {
    const definitionCollections = collectDefinitionCollections(schema as Record<string, unknown>);
    const sessionEvent =
        resolveSchema({ $ref: "#/definitions/SessionEvent" }, definitionCollections) ??
        resolveSchema({ $ref: "#/$defs/SessionEvent" }, definitionCollections);
    if (!sessionEvent?.anyOf) {
        throw new Error("Schema must have SessionEvent definition with anyOf");
    }

    return (sessionEvent.anyOf as JSONSchema7[])
        .map((variant) => {
            const resolvedVariant =
                resolveObjectSchema(variant as JSONSchema7, definitionCollections) ??
                resolveSchema(variant as JSONSchema7, definitionCollections) ??
                (variant as JSONSchema7);
            if (typeof resolvedVariant !== "object" || !resolvedVariant.properties) {
                throw new Error("Invalid event variant");
            }

            const typeSchema = resolvedVariant.properties.type as JSONSchema7;
            const typeName = typeSchema?.const as string;
            if (!typeName) {
                throw new Error("Event variant must define type.const");
            }

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

function findPyDiscriminator(
    variants: JSONSchema7[]
): { property: string; mapping: Map<string, JSONSchema7> } | null {
    if (variants.length === 0) {
        return null;
    }

    const firstVariant = variants[0];
    if (!firstVariant.properties) {
        return null;
    }

    for (const [propName, propSchema] of Object.entries(firstVariant.properties)) {
        if (typeof propSchema !== "object") {
            continue;
        }
        if ((propSchema as JSONSchema7).const === undefined) {
            continue;
        }

        const mapping = new Map<string, JSONSchema7>();
        let valid = true;
        for (const variant of variants) {
            if (!variant.properties) {
                valid = false;
                break;
            }

            const variantProp = variant.properties[propName];
            if (typeof variantProp !== "object" || (variantProp as JSONSchema7).const === undefined) {
                valid = false;
                break;
            }

            mapping.set(String((variantProp as JSONSchema7).const), variant);
        }

        if (valid && mapping.size === variants.length) {
            return { property: propName, mapping };
        }
    }

    return null;
}

function getOrCreatePyEnum(
    enumName: string,
    values: string[],
    ctx: PyCodegenCtx,
    description?: string
): string {
    const existing = ctx.enumsByName.get(enumName);
    if (existing) {
        return existing;
    }

    const lines: string[] = [];
    if (description) {
        lines.push(`class ${enumName}(Enum):`);
        lines.push(`    ${pyDocstringLiteral(description)}`);
    } else {
        lines.push(`class ${enumName}(Enum):`);
    }
    for (const value of values) {
        lines.push(`    ${toEnumMemberName(value)} = ${JSON.stringify(value)}`);
    }
    ctx.enumsByName.set(enumName, enumName);
    ctx.enums.push(lines.join("\n"));
    return enumName;
}

function resolvePyPropertyType(
    propSchema: JSONSchema7,
    parentTypeName: string,
    jsonPropName: string,
    isRequired: boolean,
    ctx: PyCodegenCtx
): PyResolvedType {
    const nestedName = parentTypeName + toPascalCase(jsonPropName);

    if (propSchema.$ref && typeof propSchema.$ref === "string") {
        const typeName = toPascalCase(refTypeName(propSchema.$ref, ctx.definitions));
        const resolved = resolveSchema(propSchema, ctx.definitions);
        if (resolved && resolved !== propSchema) {
            if (resolved.enum && Array.isArray(resolved.enum) && resolved.enum.every((value) => typeof value === "string")) {
                const enumType = getOrCreatePyEnum(typeName, resolved.enum as string[], ctx, resolved.description);
                const enumResolved: PyResolvedType = {
                    annotation: enumType,
                    fromExpr: (expr) => `parse_enum(${enumType}, ${expr})`,
                    toExpr: (expr) => `to_enum(${enumType}, ${expr})`,
                };
                return isRequired ? enumResolved : pyOptionalResolvedType(enumResolved);
            }

            const resolvedObject = resolveObjectSchema(propSchema, ctx.definitions);
            if (isNamedPyObjectSchema(resolvedObject)) {
                emitPyClass(typeName, resolvedObject, ctx, resolvedObject.description);
                const objectResolved: PyResolvedType = {
                    annotation: typeName,
                    fromExpr: (expr) => `${typeName}.from_dict(${expr})`,
                    toExpr: (expr) => `to_class(${typeName}, ${expr})`,
                };
                return isRequired ? objectResolved : pyOptionalResolvedType(objectResolved);
            }

            return resolvePyPropertyType(resolved, parentTypeName, jsonPropName, isRequired, ctx);
        }
    }

    if (propSchema.allOf && propSchema.allOf.length === 1 && typeof propSchema.allOf[0] === "object") {
        return resolvePyPropertyType(
            propSchema.allOf[0] as JSONSchema7,
            parentTypeName,
            jsonPropName,
            isRequired,
            ctx
        );
    }

    if (propSchema.anyOf) {
        const variants = (propSchema.anyOf as JSONSchema7[])
            .filter((item) => typeof item === "object")
            .map(
                (item) =>
                    resolveObjectSchema(item as JSONSchema7, ctx.definitions) ??
                    resolveSchema(item as JSONSchema7, ctx.definitions) ??
                    (item as JSONSchema7)
            );
        const nonNull = variants.filter((item) => item.type !== "null");
        const hasNull = variants.length !== nonNull.length;

        if (nonNull.length === 1) {
            const inner = resolvePyPropertyType(nonNull[0], parentTypeName, jsonPropName, true, ctx);
            return hasNull || !isRequired ? pyOptionalResolvedType(inner) : inner;
        }

        if (nonNull.length > 1) {
            const discriminator = findPyDiscriminator(nonNull);
            if (discriminator) {
                emitPyFlatDiscriminatedUnion(
                    nestedName,
                    discriminator.property,
                    discriminator.mapping,
                    ctx,
                    propSchema.description
                );
                const resolved: PyResolvedType = {
                    annotation: nestedName,
                    fromExpr: (expr) => `${nestedName}.from_dict(${expr})`,
                    toExpr: (expr) => `to_class(${nestedName}, ${expr})`,
                };
                return hasNull || !isRequired ? pyOptionalResolvedType(resolved) : resolved;
            }

            return pyAnyResolvedType();
        }
    }

    if (propSchema.enum && Array.isArray(propSchema.enum) && propSchema.enum.every((value) => typeof value === "string")) {
        const enumType = getOrCreatePyEnum(
            nestedName,
            propSchema.enum as string[],
            ctx,
            propSchema.description
        );
        const resolved: PyResolvedType = {
            annotation: enumType,
            fromExpr: (expr) => `parse_enum(${enumType}, ${expr})`,
            toExpr: (expr) => `to_enum(${enumType}, ${expr})`,
        };
        return isRequired ? resolved : pyOptionalResolvedType(resolved);
    }

    if (propSchema.const !== undefined) {
        if (typeof propSchema.const === "string") {
            const resolved = pyPrimitiveResolvedType("str", "from_str");
            return isRequired ? resolved : pyOptionalResolvedType(resolved);
        }
        if (typeof propSchema.const === "boolean") {
            const resolved = pyPrimitiveResolvedType("bool", "from_bool");
            return isRequired ? resolved : pyOptionalResolvedType(resolved);
        }
        if (typeof propSchema.const === "number") {
            const resolved = Number.isInteger(propSchema.const)
                ? pyPrimitiveResolvedType("int", "from_int", "to_int")
                : pyPrimitiveResolvedType("float", "from_float", "to_float");
            return isRequired ? resolved : pyOptionalResolvedType(resolved);
        }
    }

    const type = propSchema.type;
    const format = propSchema.format;

    if (Array.isArray(type)) {
        const nonNullTypes = type.filter((value) => value !== "null");
        if (nonNullTypes.length === 1) {
            const inner = resolvePyPropertyType(
                { ...propSchema, type: nonNullTypes[0] as JSONSchema7["type"] },
                parentTypeName,
                jsonPropName,
                true,
                ctx
            );
            return pyOptionalResolvedType(inner);
        }
    }

    if (type === "string") {
        if (format === "date-time") {
            const resolved = pyPrimitiveResolvedType("datetime", "from_datetime", "to_datetime");
            return isRequired ? resolved : pyOptionalResolvedType(resolved);
        }
        if (format === "uuid") {
            const resolved = pyPrimitiveResolvedType("UUID", "from_uuid", "to_uuid");
            return isRequired ? resolved : pyOptionalResolvedType(resolved);
        }
        if (format === "uri" || format === "regex" || isPyBase64StringSchema(propSchema)) {
            const resolved = pyPrimitiveResolvedType("str", "from_str");
            return isRequired ? resolved : pyOptionalResolvedType(resolved);
        }
        const resolved = pyPrimitiveResolvedType("str", "from_str");
        return isRequired ? resolved : pyOptionalResolvedType(resolved);
    }

    if (type === "integer") {
        if (format === "duration") {
            const resolved = pyDurationResolvedType(ctx, true);
            return isRequired ? resolved : pyOptionalResolvedType(resolved);
        }
        const resolved = pyPrimitiveResolvedType("int", "from_int", "to_int");
        return isRequired ? resolved : pyOptionalResolvedType(resolved);
    }

    if (type === "number") {
        if (format === "duration") {
            const resolved = pyDurationResolvedType(ctx, false);
            return isRequired ? resolved : pyOptionalResolvedType(resolved);
        }
        const resolved = pyPrimitiveResolvedType("float", "from_float", "to_float");
        return isRequired ? resolved : pyOptionalResolvedType(resolved);
    }

    if (type === "boolean") {
        const resolved = pyPrimitiveResolvedType("bool", "from_bool");
        return isRequired ? resolved : pyOptionalResolvedType(resolved);
    }

    if (type === "array") {
        const items = propSchema.items as JSONSchema7 | undefined;
        if (!items) {
            const resolved: PyResolvedType = {
                annotation: "list[Any]",
                fromExpr: (expr) => `from_list(lambda x: x, ${expr})`,
                toExpr: (expr) => `from_list(lambda x: x, ${expr})`,
            };
            return isRequired ? resolved : pyOptionalResolvedType(resolved);
        }

        if (items.allOf && items.allOf.length === 1 && typeof items.allOf[0] === "object") {
            return resolvePyPropertyType(
                { ...propSchema, items: items.allOf[0] as JSONSchema7 },
                parentTypeName,
                jsonPropName,
                isRequired,
                ctx
            );
        }

        if (items.anyOf) {
            const itemVariants = (items.anyOf as JSONSchema7[])
                .filter((variant) => typeof variant === "object")
                .map(
                    (variant) =>
                        resolveObjectSchema(variant as JSONSchema7, ctx.definitions) ??
                        resolveSchema(variant as JSONSchema7, ctx.definitions) ??
                        (variant as JSONSchema7)
                )
                .filter((variant) => variant.type !== "null");
            const discriminator = findPyDiscriminator(itemVariants);
            if (discriminator) {
                const itemTypeName = nestedName + "Item";
                emitPyFlatDiscriminatedUnion(
                    itemTypeName,
                    discriminator.property,
                    discriminator.mapping,
                    ctx,
                    items.description
                );
                const resolved: PyResolvedType = {
                    annotation: `list[${itemTypeName}]`,
                    fromExpr: (expr) => `from_list(${itemTypeName}.from_dict, ${expr})`,
                    toExpr: (expr) => `from_list(lambda x: to_class(${itemTypeName}, x), ${expr})`,
                };
                return isRequired ? resolved : pyOptionalResolvedType(resolved);
            }
        }

        const itemType = resolvePyPropertyType(items, parentTypeName, jsonPropName + "Item", true, ctx);
        const resolved: PyResolvedType = {
            annotation: `list[${itemType.annotation}]`,
            fromExpr: (expr) => `from_list(${wrapParser(itemType)}, ${expr})`,
            toExpr: (expr) => `from_list(${wrapSerializer(itemType)}, ${expr})`,
        };
        return isRequired ? resolved : pyOptionalResolvedType(resolved);
    }

    if (type === "object" || (propSchema.properties && !type)) {
        if (propSchema.properties) {
            emitPyClass(nestedName, propSchema, ctx, propSchema.description);
            const resolved: PyResolvedType = {
                annotation: nestedName,
                fromExpr: (expr) => `${nestedName}.from_dict(${expr})`,
                toExpr: (expr) => `to_class(${nestedName}, ${expr})`,
            };
            return isRequired ? resolved : pyOptionalResolvedType(resolved);
        }

        if (propSchema.additionalProperties) {
            if (
                typeof propSchema.additionalProperties === "object" &&
                Object.keys(propSchema.additionalProperties as Record<string, unknown>).length > 0
            ) {
                const valueType = resolvePyPropertyType(
                    propSchema.additionalProperties as JSONSchema7,
                    parentTypeName,
                    jsonPropName + "Value",
                    true,
                    ctx
                );
                const resolved: PyResolvedType = {
                    annotation: `dict[str, ${valueType.annotation}]`,
                    fromExpr: (expr) => `from_dict(${wrapParser(valueType)}, ${expr})`,
                    toExpr: (expr) => `from_dict(${wrapSerializer(valueType)}, ${expr})`,
                };
                return isRequired ? resolved : pyOptionalResolvedType(resolved);
            }

            const resolved: PyResolvedType = {
                annotation: "dict[str, Any]",
                fromExpr: (expr) => `from_dict(lambda x: x, ${expr})`,
                toExpr: (expr) => `from_dict(lambda x: x, ${expr})`,
            };
            return isRequired ? resolved : pyOptionalResolvedType(resolved);
        }

        return pyAnyResolvedType();
    }

    return pyAnyResolvedType();
}

function emitPyClass(
    typeName: string,
    schema: JSONSchema7,
    ctx: PyCodegenCtx,
    description?: string
): void {
    if (ctx.generatedNames.has(typeName)) {
        return;
    }
    ctx.generatedNames.add(typeName);

    const required = new Set(schema.required || []);
    const fieldEntries = Object.entries(schema.properties || {}).filter(
        ([, value]) => typeof value === "object"
    ) as Array<[string, JSONSchema7]>;
    const orderedFieldEntries = [
        ...fieldEntries.filter(([name]) => required.has(name)),
        ...fieldEntries.filter(([name]) => !required.has(name)),
    ];

    const fieldInfos = orderedFieldEntries.map(([propName, propSchema]) => {
        const isRequired = required.has(propName);
        const resolved = resolvePyPropertyType(propSchema, typeName, propName, isRequired, ctx);
        return {
            jsonName: propName,
            fieldName: toSnakeCase(propName),
            isRequired,
            resolved,
            defaultLiteral: isRequired ? undefined : toPythonLiteral(propSchema.default),
        };
    });

    const lines: string[] = [];
    lines.push(`@dataclass`);
    lines.push(`class ${typeName}:`);
    if (description || schema.description) {
        lines.push(`    ${pyDocstringLiteral(description || schema.description || "")}`);
    }

    if (fieldInfos.length === 0) {
        lines.push(`    @staticmethod`);
        lines.push(`    def from_dict(obj: Any) -> "${typeName}":`);
        lines.push(`        assert isinstance(obj, dict)`);
        lines.push(`        return ${typeName}()`);
        lines.push(``);
        lines.push(`    def to_dict(self) -> dict:`);
        lines.push(`        return {}`);
        ctx.classes.push(lines.join("\n"));
        return;
    }

    for (const field of fieldInfos) {
        const suffix = field.isRequired ? "" : " = None";
        lines.push(`    ${field.fieldName}: ${field.resolved.annotation}${suffix}`);
    }

    lines.push(``);
    lines.push(`    @staticmethod`);
    lines.push(`    def from_dict(obj: Any) -> "${typeName}":`);
    lines.push(`        assert isinstance(obj, dict)`);
    for (const field of fieldInfos) {
        const sourceExpr = field.defaultLiteral
            ? `obj.get(${JSON.stringify(field.jsonName)}, ${field.defaultLiteral})`
            : `obj.get(${JSON.stringify(field.jsonName)})`;
        lines.push(
            `        ${field.fieldName} = ${field.resolved.fromExpr(sourceExpr)}`
        );
    }
    lines.push(`        return ${typeName}(`);
    for (const field of fieldInfos) {
        lines.push(`            ${field.fieldName}=${field.fieldName},`);
    }
    lines.push(`        )`);
    lines.push(``);
    lines.push(`    def to_dict(self) -> dict:`);
    lines.push(`        result: dict = {}`);
    for (const field of fieldInfos) {
        const valueExpr = field.resolved.toExpr(`self.${field.fieldName}`);
        if (field.isRequired) {
            lines.push(`        result[${JSON.stringify(field.jsonName)}] = ${valueExpr}`);
        } else {
            lines.push(`        if self.${field.fieldName} is not None:`);
            lines.push(`            result[${JSON.stringify(field.jsonName)}] = ${valueExpr}`);
        }
    }
    lines.push(`        return result`);

    ctx.classes.push(lines.join("\n"));
}

function emitPyFlatDiscriminatedUnion(
    typeName: string,
    discriminatorProp: string,
    mapping: Map<string, JSONSchema7>,
    ctx: PyCodegenCtx,
    description?: string
): void {
    if (ctx.generatedNames.has(typeName)) {
        return;
    }
    ctx.generatedNames.add(typeName);

    const allProps = new Map<string, { schema: JSONSchema7; requiredInAll: boolean }>();
    for (const [, variant] of mapping) {
        const required = new Set(variant.required || []);
        for (const [propName, propSchema] of Object.entries(variant.properties || {})) {
            if (typeof propSchema !== "object") {
                continue;
            }
            if (!allProps.has(propName)) {
                allProps.set(propName, {
                    schema: propSchema as JSONSchema7,
                    requiredInAll: required.has(propName),
                });
            } else if (!required.has(propName)) {
                allProps.get(propName)!.requiredInAll = false;
            }
        }
    }

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

    const discriminatorEnumName = getOrCreatePyEnum(
        typeName + toPascalCase(discriminatorProp),
        [...mapping.keys()],
        ctx,
        description ? `${description} discriminator` : `${typeName} discriminator`
    );

    const fieldEntries: Array<[string, JSONSchema7, boolean]> = [
        [
            discriminatorProp,
            {
                type: "string",
                enum: [...mapping.keys()],
            },
            true,
        ],
        ...[...allProps.entries()]
            .filter(([propName]) => propName !== discriminatorProp)
            .map(([propName, info]) => [propName, info.schema, info.requiredInAll] as [string, JSONSchema7, boolean]),
    ];

    const orderedFieldEntries = [
        ...fieldEntries.filter(([, , requiredInAll]) => requiredInAll),
        ...fieldEntries.filter(([, , requiredInAll]) => !requiredInAll),
    ];

    const fieldInfos = orderedFieldEntries.map(([propName, propSchema, requiredInAll]) => {
        let resolved: PyResolvedType;
        if (propName === discriminatorProp) {
            resolved = {
                annotation: discriminatorEnumName,
                fromExpr: (expr) => `parse_enum(${discriminatorEnumName}, ${expr})`,
                toExpr: (expr) => `to_enum(${discriminatorEnumName}, ${expr})`,
            };
        } else {
            resolved = resolvePyPropertyType(propSchema, typeName, propName, requiredInAll, ctx);
        }

        return {
            jsonName: propName,
            fieldName: toSnakeCase(propName),
            isRequired: requiredInAll,
            resolved,
            defaultLiteral: requiredInAll ? undefined : toPythonLiteral(propSchema.default),
        };
    });

    const lines: string[] = [];
    lines.push(`@dataclass`);
    lines.push(`class ${typeName}:`);
    if (description) {
        lines.push(`    ${pyDocstringLiteral(description)}`);
    }
    for (const field of fieldInfos) {
        const suffix = field.isRequired ? "" : " = None";
        lines.push(`    ${field.fieldName}: ${field.resolved.annotation}${suffix}`);
    }
    lines.push(``);
    lines.push(`    @staticmethod`);
    lines.push(`    def from_dict(obj: Any) -> "${typeName}":`);
    lines.push(`        assert isinstance(obj, dict)`);
    for (const field of fieldInfos) {
        const sourceExpr = field.defaultLiteral
            ? `obj.get(${JSON.stringify(field.jsonName)}, ${field.defaultLiteral})`
            : `obj.get(${JSON.stringify(field.jsonName)})`;
        lines.push(
            `        ${field.fieldName} = ${field.resolved.fromExpr(sourceExpr)}`
        );
    }
    lines.push(`        return ${typeName}(`);
    for (const field of fieldInfos) {
        lines.push(`            ${field.fieldName}=${field.fieldName},`);
    }
    lines.push(`        )`);
    lines.push(``);
    lines.push(`    def to_dict(self) -> dict:`);
    lines.push(`        result: dict = {}`);
    for (const field of fieldInfos) {
        const valueExpr = field.resolved.toExpr(`self.${field.fieldName}`);
        if (field.isRequired) {
            lines.push(`        result[${JSON.stringify(field.jsonName)}] = ${valueExpr}`);
        } else {
            lines.push(`        if self.${field.fieldName} is not None:`);
            lines.push(`            result[${JSON.stringify(field.jsonName)}] = ${valueExpr}`);
        }
    }
    lines.push(`        return result`);

    ctx.classes.push(lines.join("\n"));
}

export function generatePythonSessionEventsCode(schema: JSONSchema7): string {
    const variants = extractPyEventVariants(schema);
    const ctx: PyCodegenCtx = {
        classes: [],
        enums: [],
        enumsByName: new Map(),
        generatedNames: new Set(),
        usesTimedelta: false,
        usesIntegerTimedelta: false,
        definitions: collectDefinitionCollections(schema as Record<string, unknown>),
    };

    for (const variant of variants) {
        emitPyClass(variant.dataClassName, variant.dataSchema, ctx, variant.dataDescription);
    }

    const eventTypeLines: string[] = [];
    eventTypeLines.push(`class SessionEventType(Enum):`);
    for (const variant of variants) {
        eventTypeLines.push(`    ${toEnumMemberName(variant.typeName)} = ${JSON.stringify(variant.typeName)}`);
    }
    eventTypeLines.push(`    UNKNOWN = "unknown"`);
    eventTypeLines.push(``);
    eventTypeLines.push(`    @classmethod`);
    eventTypeLines.push(`    def _missing_(cls, value: object) -> "SessionEventType":`);
    eventTypeLines.push(`        return cls.UNKNOWN`);

    const out: string[] = [];
    out.push(`"""`);
    out.push(`AUTO-GENERATED FILE - DO NOT EDIT`);
    out.push(`Generated from: session-events.schema.json`);
    out.push(`"""`);
    out.push(``);
    out.push(`from __future__ import annotations`);
    out.push(``);
    out.push(`from collections.abc import Callable`);
    out.push(`from dataclasses import dataclass`);
    out.push(ctx.usesTimedelta ? `from datetime import datetime, timedelta` : `from datetime import datetime`);
    out.push(`from enum import Enum`);
    out.push(`from typing import Any, TypeVar, cast`);
    out.push(`from uuid import UUID`);
    out.push(``);
    out.push(`import dateutil.parser`);
    out.push(``);
    out.push(`T = TypeVar("T")`);
    out.push(`EnumT = TypeVar("EnumT", bound=Enum)`);
    out.push(``);
    out.push(``);
    out.push(`def from_str(x: Any) -> str:`);
    out.push(`    assert isinstance(x, str)`);
    out.push(`    return x`);
    out.push(``);
    out.push(``);
    out.push(`def from_int(x: Any) -> int:`);
    out.push(`    assert isinstance(x, int) and not isinstance(x, bool)`);
    out.push(`    return x`);
    out.push(``);
    out.push(``);
    out.push(`def to_int(x: Any) -> int:`);
    out.push(`    assert isinstance(x, int) and not isinstance(x, bool)`);
    out.push(`    return x`);
    out.push(``);
    out.push(``);
    out.push(`def from_float(x: Any) -> float:`);
    out.push(`    assert isinstance(x, (float, int)) and not isinstance(x, bool)`);
    out.push(`    return float(x)`);
    out.push(``);
    out.push(``);
    out.push(`def to_float(x: Any) -> float:`);
    out.push(`    assert isinstance(x, (float, int)) and not isinstance(x, bool)`);
    out.push(`    return float(x)`);
    out.push(``);
    out.push(``);
    if (ctx.usesTimedelta) {
        out.push(`def from_timedelta(x: Any) -> timedelta:`);
        out.push(`    assert isinstance(x, (float, int)) and not isinstance(x, bool)`);
        out.push(`    return timedelta(milliseconds=float(x))`);
        out.push(``);
        out.push(``);
        if (ctx.usesIntegerTimedelta) {
            out.push(`def to_timedelta_int(x: timedelta) -> int:`);
            out.push(`    assert isinstance(x, timedelta)`);
            out.push(`    milliseconds = x.total_seconds() * 1000.0`);
            out.push(`    assert milliseconds.is_integer()`);
            out.push(`    return int(milliseconds)`);
            out.push(``);
            out.push(``);
        }
        out.push(`def to_timedelta(x: timedelta) -> float:`);
        out.push(`    assert isinstance(x, timedelta)`);
        out.push(`    return x.total_seconds() * 1000.0`);
        out.push(``);
        out.push(``);
    }
    out.push(`def from_bool(x: Any) -> bool:`);
    out.push(`    assert isinstance(x, bool)`);
    out.push(`    return x`);
    out.push(``);
    out.push(``);
    out.push(`def from_none(x: Any) -> Any:`);
    out.push(`    assert x is None`);
    out.push(`    return x`);
    out.push(``);
    out.push(``);
    out.push(`def from_union(fs: list[Callable[[Any], T]], x: Any) -> T:`);
    out.push(`    for f in fs:`);
    out.push(`        try:`);
    out.push(`            return f(x)`);
    out.push(`        except Exception:`);
    out.push(`            pass`);
    out.push(`    assert False`);
    out.push(``);
    out.push(``);
    out.push(`def from_list(f: Callable[[Any], T], x: Any) -> list[T]:`);
    out.push(`    assert isinstance(x, list)`);
    out.push(`    return [f(item) for item in x]`);
    out.push(``);
    out.push(``);
    out.push(`def from_dict(f: Callable[[Any], T], x: Any) -> dict[str, T]:`);
    out.push(`    assert isinstance(x, dict)`);
    out.push(`    return {key: f(value) for key, value in x.items()}`);
    out.push(``);
    out.push(``);
    out.push(`def from_datetime(x: Any) -> datetime:`);
    out.push(`    return dateutil.parser.parse(from_str(x))`);
    out.push(``);
    out.push(``);
    out.push(`def to_datetime(x: datetime) -> str:`);
    out.push(`    return x.isoformat()`);
    out.push(``);
    out.push(``);
    out.push(`def from_uuid(x: Any) -> UUID:`);
    out.push(`    return UUID(from_str(x))`);
    out.push(``);
    out.push(``);
    out.push(`def to_uuid(x: UUID) -> str:`);
    out.push(`    return str(x)`);
    out.push(``);
    out.push(``);
    out.push(`def parse_enum(c: type[EnumT], x: Any) -> EnumT:`);
    out.push(`    assert isinstance(x, str)`);
    out.push(`    return c(x)`);
    out.push(``);
    out.push(``);
    out.push(`def to_class(c: type[T], x: Any) -> dict:`);
    out.push(`    assert isinstance(x, c)`);
    out.push(`    return cast(Any, x).to_dict()`);
    out.push(``);
    out.push(``);
    out.push(`def to_enum(c: type[EnumT], x: Any) -> str:`);
    out.push(`    assert isinstance(x, c)`);
    out.push(`    return cast(str, x.value)`);
    out.push(``);
    out.push(``);
    out.push(eventTypeLines.join("\n"));
    out.push(``);
    out.push(``);
    out.push(`@dataclass`);
    out.push(`class RawSessionEventData:`);
    out.push(`    raw: Any`);
    out.push(``);
    out.push(`    @staticmethod`);
    out.push(`    def from_dict(obj: Any) -> "RawSessionEventData":`);
    out.push(`        return RawSessionEventData(obj)`);
    out.push(``);
    out.push(`    def to_dict(self) -> Any:`);
    out.push(`        return self.raw`);
    out.push(``);
    out.push(``);
    out.push(`def _compat_to_python_key(name: str) -> str:`);
    out.push(`    normalized = name.replace(".", "_")`);
    out.push(`    result: list[str] = []`);
    out.push(`    for index, char in enumerate(normalized):`);
    out.push(
        `        if char.isupper() and index > 0 and (not normalized[index - 1].isupper() or (index + 1 < len(normalized) and normalized[index + 1].islower())):`
    );
    out.push(`            result.append("_")`);
    out.push(`        result.append(char.lower())`);
    out.push(`    return "".join(result)`);
    out.push(``);
    out.push(``);
    out.push(`def _compat_to_json_key(name: str) -> str:`);
    out.push(`    parts = name.split("_")`);
    out.push(`    if not parts:`);
    out.push(`        return name`);
    out.push(`    return parts[0] + "".join(part[:1].upper() + part[1:] for part in parts[1:])`);
    out.push(``);
    out.push(``);
    out.push(`def _compat_to_json_value(value: Any) -> Any:`);
    out.push(`    if hasattr(value, "to_dict"):`);
    out.push(`        return cast(Any, value).to_dict()`);
    out.push(`    if isinstance(value, Enum):`);
    out.push(`        return value.value`);
    out.push(`    if isinstance(value, datetime):`);
    out.push(`        return value.isoformat()`);
    if (ctx.usesTimedelta) {
        out.push(`    if isinstance(value, timedelta):`);
        out.push(`        return value.total_seconds() * 1000.0`);
    }
    out.push(`    if isinstance(value, UUID):`);
    out.push(`        return str(value)`);
    out.push(`    if isinstance(value, list):`);
    out.push(`        return [_compat_to_json_value(item) for item in value]`);
    out.push(`    if isinstance(value, dict):`);
    out.push(`        return {key: _compat_to_json_value(item) for key, item in value.items()}`);
    out.push(`    return value`);
    out.push(``);
    out.push(``);
    out.push(`def _compat_from_json_value(value: Any) -> Any:`);
    out.push(`    return value`);
    out.push(``);
    out.push(``);
    out.push(`class Data:`);
    out.push(`    """Backward-compatible shim for manually constructed event payloads."""`);
    out.push(``);
    out.push(`    def __init__(self, **kwargs: Any):`);
    out.push(`        self._values = {key: _compat_from_json_value(value) for key, value in kwargs.items()}`);
    out.push(`        for key, value in self._values.items():`);
    out.push(`            setattr(self, key, value)`);
    out.push(``);
    out.push(`    @staticmethod`);
    out.push(`    def from_dict(obj: Any) -> "Data":`);
    out.push(`        assert isinstance(obj, dict)`);
    out.push(
        `        return Data(**{_compat_to_python_key(key): _compat_from_json_value(value) for key, value in obj.items()})`
    );
    out.push(``);
    out.push(`    def to_dict(self) -> dict:`);
    out.push(
        `        return {_compat_to_json_key(key): _compat_to_json_value(value) for key, value in self._values.items() if value is not None}`
    );
    out.push(``);
    out.push(``);
    for (const classDef of ctx.classes) {
        out.push(classDef);
        out.push(``);
        out.push(``);
    }
    for (const enumDef of ctx.enums) {
        out.push(enumDef);
        out.push(``);
        out.push(``);
    }

    const sessionEventDataTypes = [
        ...variants.map((variant) => variant.dataClassName),
        "RawSessionEventData",
        "Data",
    ];
    out.push(`SessionEventData = ${sessionEventDataTypes.join(" | ")}`);
    out.push(``);
    out.push(``);
    out.push(`@dataclass`);
    out.push(`class SessionEvent:`);
    out.push(`    data: SessionEventData`);
    out.push(`    id: UUID`);
    out.push(`    timestamp: datetime`);
    out.push(`    type: SessionEventType`);
    out.push(`    ephemeral: bool | None = None`);
    out.push(`    parent_id: UUID | None = None`);
    out.push(`    raw_type: str | None = None`);
    out.push(``);
    out.push(`    @staticmethod`);
    out.push(`    def from_dict(obj: Any) -> "SessionEvent":`);
    out.push(`        assert isinstance(obj, dict)`);
    out.push(`        raw_type = from_str(obj.get("type"))`);
    out.push(`        event_type = SessionEventType(raw_type)`);
    out.push(`        event_id = from_uuid(obj.get("id"))`);
    out.push(`        timestamp = from_datetime(obj.get("timestamp"))`);
    out.push(`        ephemeral = from_union([from_bool, from_none], obj.get("ephemeral"))`);
    out.push(`        parent_id = from_union([from_none, from_uuid], obj.get("parentId"))`);
    out.push(`        data_obj = obj.get("data")`);
    out.push(`        match event_type:`);
    for (const variant of variants) {
        out.push(
            `            case SessionEventType.${toEnumMemberName(variant.typeName)}: data = ${variant.dataClassName}.from_dict(data_obj)`
        );
    }
    out.push(`            case _: data = RawSessionEventData.from_dict(data_obj)`);
    out.push(`        return SessionEvent(`);
    out.push(`            data=data,`);
    out.push(`            id=event_id,`);
    out.push(`            timestamp=timestamp,`);
    out.push(`            type=event_type,`);
    out.push(`            ephemeral=ephemeral,`);
    out.push(`            parent_id=parent_id,`);
    out.push(`            raw_type=raw_type if event_type == SessionEventType.UNKNOWN else None,`);
    out.push(`        )`);
    out.push(``);
    out.push(`    def to_dict(self) -> dict:`);
    out.push(`        result: dict = {}`);
    out.push(`        result["data"] = self.data.to_dict()`);
    out.push(`        result["id"] = to_uuid(self.id)`);
    out.push(`        result["timestamp"] = to_datetime(self.timestamp)`);
    out.push(
        `        result["type"] = self.raw_type if self.type == SessionEventType.UNKNOWN and self.raw_type is not None else to_enum(SessionEventType, self.type)`
    );
    out.push(`        if self.ephemeral is not None:`);
    out.push(`            result["ephemeral"] = from_bool(self.ephemeral)`);
    out.push(`        result["parentId"] = from_union([from_none, to_uuid], self.parent_id)`);
    out.push(`        return result`);
    out.push(``);
    out.push(``);
    out.push(`def session_event_from_dict(s: Any) -> SessionEvent:`);
    out.push(`    return SessionEvent.from_dict(s)`);
    out.push(``);
    out.push(``);
    out.push(`def session_event_to_dict(x: SessionEvent) -> Any:`);
    out.push(`    return x.to_dict()`);
    out.push(``);
    out.push(``);

    return postProcessPythonSessionEventCode(out.join("\n"));
}

async function generateSessionEvents(schemaPath?: string): Promise<void> {
    console.log("Python: generating session-events...");

    const resolvedPath = schemaPath ?? (await getSessionEventsSchemaPath());
    const schema = JSON.parse(await fs.readFile(resolvedPath, "utf-8")) as JSONSchema7;
    const processed = postProcessSchema(schema);
    const code = generatePythonSessionEventsCode(processed);

    const outPath = await writeGeneratedFile("python/copilot/generated/session_events.py", code);
    console.log(`  ✓ ${outPath}`);
}

// ── RPC Types ───────────────────────────────────────────────────────────────

async function generateRpc(schemaPath?: string): Promise<void> {
    console.log("Python: generating RPC types...");
    const { FetchingJSONSchemaStore, InputData, JSONSchemaInput, quicktype } = await import("quicktype-core");

    const resolvedPath = schemaPath ?? (await getApiSchemaPath());
    const schema = cloneSchemaForCodegen(JSON.parse(await fs.readFile(resolvedPath, "utf-8")) as ApiSchema);

    const allMethods = [
        ...collectRpcMethods(schema.server || {}),
        ...collectRpcMethods(schema.session || {}),
        ...collectRpcMethods(schema.clientSession || {}),
    ];

    // Build a combined schema for quicktype, including shared definitions from the API schema
    rpcDefinitions = collectDefinitionCollections(schema as Record<string, unknown>);
    const combinedSchema = withSharedDefinitions(
        {
            $schema: "http://json-schema.org/draft-07/schema#",
        },
        rpcDefinitions
    );

    for (const method of allMethods) {
        const resultSchema = getMethodResultSchema(method);
        if (!isVoidSchema(resultSchema)) {
            combinedSchema.definitions![pythonResultTypeName(method)] = withRootTitle(
                schemaSourceForNamedDefinition(method.result, resultSchema),
                pythonResultTypeName(method)
            );
        }
        const resolvedParams = getMethodParamsSchema(method);
        if (method.params && hasSchemaPayload(resolvedParams)) {
            if (method.rpcMethod.startsWith("session.") && resolvedParams?.properties) {
                const filtered: JSONSchema7 = {
                    ...resolvedParams,
                    properties: Object.fromEntries(
                        Object.entries(resolvedParams.properties).filter(([k]) => k !== "sessionId")
                    ),
                    required: resolvedParams.required?.filter((r) => r !== "sessionId"),
                };
                if (hasSchemaPayload(filtered)) {
                    combinedSchema.definitions![pythonParamsTypeName(method)] = withRootTitle(
                        filtered,
                        pythonParamsTypeName(method)
                    );
                }
            } else {
                combinedSchema.definitions![pythonParamsTypeName(method)] = withRootTitle(
                    schemaSourceForNamedDefinition(method.params, resolvedParams),
                    pythonParamsTypeName(method)
                );
            }
        }
    }

    const { rootDefinitions, sharedDefinitions } = hoistTitledSchemas(combinedSchema.definitions! as Record<string, JSONSchema7>);
    const allDefinitions = { ...rootDefinitions, ...sharedDefinitions };
    const allDefinitionCollections: DefinitionCollections = {
        definitions: { ...(combinedSchema.$defs ?? {}), ...allDefinitions },
        $defs: { ...allDefinitions, ...(combinedSchema.$defs ?? {}) },
    };

    // Generate types via quicktype
    const schemaInput = new JSONSchemaInput(new FetchingJSONSchemaStore());
    for (const [name, def] of Object.entries(rootDefinitions)) {
        const schemaWithDefs = withSharedDefinitions(
            typeof def === "object" ? (def as JSONSchema7) : {},
            allDefinitionCollections
        );
        await schemaInput.addSource({ name, schema: JSON.stringify(schemaWithDefs) });
    }

    const inputData = new InputData();
    inputData.addInput(schemaInput);

    const qtResult = await quicktype({
        inputData,
        lang: "python",
        rendererOptions: { "python-version": "3.7" },
    });

    let typesCode = qtResult.lines.join("\n");
    // Fix dataclass field ordering
    typesCode = typesCode.replace(/: Any$/gm, ": Any = None");
    // Fix bare except: to use Exception (required by ruff/pylint)
    typesCode = typesCode.replace(/except:/g, "except Exception:");
    // Remove unnecessary pass when class has methods (quicktype generates pass for empty schemas)
    typesCode = typesCode.replace(/^(\s*)pass\n\n(\s*@staticmethod)/gm, "$2");
    // Modernize to Python 3.11+ syntax
    typesCode = modernizePython(typesCode);
    typesCode = collapsePlaceholderPythonDataclasses(typesCode);

    // Strip quicktype's import block and preamble — we provide our own unified header.
    // The preamble ends just before the first helper function (e.g. "def from_str")
    // or class definition.
    typesCode = typesCode.replace(/^[\s\S]*?(?=^(?:def |@dataclass|class )\w)/m, "");

    // Strip trailing whitespace from blank lines (e.g. inside multi-line docstrings)
    typesCode = typesCode.replace(/^\s+$/gm, "");

    // Annotate experimental data types
    const experimentalTypeNames = new Set<string>();
    for (const method of allMethods) {
        if (method.stability !== "experimental") continue;
        experimentalTypeNames.add(pythonResultTypeName(method));
        const paramsTypeName = pythonParamsTypeName(method);
        if (rootDefinitions[paramsTypeName]) {
            experimentalTypeNames.add(paramsTypeName);
        }
    }
    for (const typeName of experimentalTypeNames) {
        typesCode = typesCode.replace(
            new RegExp(`^(@dataclass\\n)?class ${typeName}[:(]`, "m"),
            (match) => `# Experimental: this type is part of an experimental API and may change or be removed.\n${match}`
        );
    }

    // Extract actual class names generated by quicktype (may differ from toPascalCase,
    // e.g. quicktype produces "SessionMCPList" not "SessionMcpList")
    const actualTypeNames = new Map<string, string>();
    const classRe = /^class\s+(\w+)\b/gm;
    let cm;
    while ((cm = classRe.exec(typesCode)) !== null) {
        actualTypeNames.set(cm[1].toLowerCase(), cm[1]);
    }
    const resolveType = (name: string): string => actualTypeNames.get(name.toLowerCase()) ?? name;

    const lines: string[] = [];
    lines.push(`"""
AUTO-GENERATED FILE - DO NOT EDIT
Generated from: api.schema.json
"""

from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from .._jsonrpc import JsonRpcClient

from collections.abc import Callable
from dataclasses import dataclass
from datetime import datetime
from enum import Enum
from typing import Any, Protocol, TypeVar, cast
from uuid import UUID

import dateutil.parser

T = TypeVar("T")
EnumT = TypeVar("EnumT", bound=Enum)

`);
    lines.push(typesCode);
    lines.push(`
def _timeout_kwargs(timeout: float | None) -> dict:
    """Build keyword arguments for optional timeout forwarding."""
    if timeout is not None:
        return {"timeout": timeout}
    return {}

def _patch_model_capabilities(data: dict) -> dict:
    """Ensure model capabilities have required fields.

    TODO: Remove once the runtime schema correctly marks these fields as optional.
    Some models (e.g. embedding models) may omit 'limits' or 'supports' in their
    capabilities, or omit 'max_context_window_tokens' within limits. The generated
    deserializer requires these fields, so we supply defaults here.
    """
    for model in data.get("models", []):
        caps = model.get("capabilities")
        if caps is None:
            model["capabilities"] = {"supports": {}, "limits": {"max_context_window_tokens": 0}}
            continue
        if "supports" not in caps:
            caps["supports"] = {}
        if "limits" not in caps:
            caps["limits"] = {"max_context_window_tokens": 0}
        elif "max_context_window_tokens" not in caps["limits"]:
            caps["limits"]["max_context_window_tokens"] = 0
    return data

`);

    // Emit RPC wrapper classes
    if (schema.server) {
        emitRpcWrapper(lines, schema.server, false, resolveType);
    }
    if (schema.session) {
        emitRpcWrapper(lines, schema.session, true, resolveType);
    }
    if (schema.clientSession) {
        emitClientSessionApiRegistration(lines, schema.clientSession, resolveType);
    }

    // Patch models.list to normalize capabilities before deserialization
    let finalCode = lines.join("\n");
    finalCode = finalCode.replace(
        `ModelList.from_dict(await self._client.request("models.list"`,
        `ModelList.from_dict(_patch_model_capabilities(await self._client.request("models.list"`,
    );
    // Close the extra paren opened by _patch_model_capabilities(
    finalCode = finalCode.replace(
        /(_patch_model_capabilities\(await self\._client\.request\("models\.list",\s*\{[^)]*\)[^)]*\))/,
        "$1)",
    );

    const outPath = await writeGeneratedFile("python/copilot/generated/rpc.py", finalCode);
    console.log(`  ✓ ${outPath}`);
}

function emitRpcWrapper(lines: string[], node: Record<string, unknown>, isSession: boolean, resolveType: (name: string) => string): void {
    const groups = Object.entries(node).filter(([, v]) => typeof v === "object" && v !== null && !isRpcMethod(v));
    const topLevelMethods = Object.entries(node).filter(([, v]) => isRpcMethod(v));

    const wrapperName = isSession ? "SessionRpc" : "ServerRpc";

    // Emit API classes for groups
    for (const [groupName, groupNode] of groups) {
        const prefix = isSession ? "" : "Server";
        const apiName = prefix + toPascalCase(groupName) + "Api";
        const groupExperimental = isNodeFullyExperimental(groupNode as Record<string, unknown>);
        if (isSession) {
            if (groupExperimental) {
                lines.push(`# Experimental: this API group is experimental and may change or be removed.`);
            }
            lines.push(`class ${apiName}:`);
            lines.push(`    def __init__(self, client: "JsonRpcClient", session_id: str):`);
            lines.push(`        self._client = client`);
            lines.push(`        self._session_id = session_id`);
        } else {
            if (groupExperimental) {
                lines.push(`# Experimental: this API group is experimental and may change or be removed.`);
            }
            lines.push(`class ${apiName}:`);
            lines.push(`    def __init__(self, client: "JsonRpcClient"):`);
            lines.push(`        self._client = client`);
        }
        lines.push(``);
        for (const [key, value] of Object.entries(groupNode as Record<string, unknown>)) {
            if (!isRpcMethod(value)) continue;
            emitMethod(lines, key, value, isSession, resolveType, groupExperimental);
        }
        lines.push(``);
    }

    // Emit wrapper class
    if (isSession) {
        lines.push(`class ${wrapperName}:`);
        lines.push(`    """Typed session-scoped RPC methods."""`);
        lines.push(`    def __init__(self, client: "JsonRpcClient", session_id: str):`);
        lines.push(`        self._client = client`);
        lines.push(`        self._session_id = session_id`);
        for (const [groupName] of groups) {
            lines.push(`        self.${toSnakeCase(groupName)} = ${toPascalCase(groupName)}Api(client, session_id)`);
        }
    } else {
        lines.push(`class ${wrapperName}:`);
        lines.push(`    """Typed server-scoped RPC methods."""`);
        lines.push(`    def __init__(self, client: "JsonRpcClient"):`);
        lines.push(`        self._client = client`);
        for (const [groupName] of groups) {
            lines.push(`        self.${toSnakeCase(groupName)} = Server${toPascalCase(groupName)}Api(client)`);
        }
    }
    lines.push(``);

    // Top-level methods
    for (const [key, value] of topLevelMethods) {
        if (!isRpcMethod(value)) continue;
        emitMethod(lines, key, value, isSession, resolveType, false);
    }
    lines.push(``);
}

function emitMethod(lines: string[], name: string, method: RpcMethod, isSession: boolean, resolveType: (name: string) => string, groupExperimental = false): void {
    const methodName = toSnakeCase(name);
    const resultSchema = getMethodResultSchema(method);
    const hasResult = !isVoidSchema(resultSchema);
    const resultType = hasResult ? resolveType(pythonResultTypeName(method)) : "None";
    const resultIsObject = isObjectSchema(resultSchema);

    const effectiveParams = getMethodParamsSchema(method);
    const paramProps = effectiveParams?.properties || {};
    const nonSessionParams = Object.keys(paramProps).filter((k) => k !== "sessionId");
    const hasParams = isSession ? nonSessionParams.length > 0 : hasSchemaPayload(effectiveParams);
    const paramsType = resolveType(pythonParamsTypeName(method));

    // Build signature with typed params + optional timeout
    const sig = hasParams
        ? `    async def ${methodName}(self, params: ${paramsType}, *, timeout: float | None = None) -> ${resultType}:`
        : `    async def ${methodName}(self, *, timeout: float | None = None) -> ${resultType}:`;

    lines.push(sig);

    if (method.stability === "experimental" && !groupExperimental) {
        lines.push(`        """.. warning:: This API is experimental and may change or be removed in future versions."""`);
    }

    // For object results use .from_dict(); for enums/primitives use direct construction
    const deserialize = (expr: string) => resultIsObject ? `${resultType}.from_dict(${expr})` : `${resultType}(${expr})`;

    // Build request body with proper serialization/deserialization
    if (isSession) {
        if (hasParams) {
            lines.push(`        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}`);
            lines.push(`        params_dict["sessionId"] = self._session_id`);
            if (hasResult) {
                lines.push(`        return ${deserialize(`await self._client.request("${method.rpcMethod}", params_dict, **_timeout_kwargs(timeout))`)}`);
            } else {
                lines.push(`        await self._client.request("${method.rpcMethod}", params_dict, **_timeout_kwargs(timeout))`);
            }
        } else {
            if (hasResult) {
                lines.push(`        return ${deserialize(`await self._client.request("${method.rpcMethod}", {"sessionId": self._session_id}, **_timeout_kwargs(timeout))`)}`);
            } else {
                lines.push(`        await self._client.request("${method.rpcMethod}", {"sessionId": self._session_id}, **_timeout_kwargs(timeout))`);
            }
        }
    } else {
        if (hasParams) {
            lines.push(`        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}`);
            if (hasResult) {
                lines.push(`        return ${deserialize(`await self._client.request("${method.rpcMethod}", params_dict, **_timeout_kwargs(timeout))`)}`);
            } else {
                lines.push(`        await self._client.request("${method.rpcMethod}", params_dict, **_timeout_kwargs(timeout))`);
            }
        } else {
            if (hasResult) {
                lines.push(`        return ${deserialize(`await self._client.request("${method.rpcMethod}", {}, **_timeout_kwargs(timeout))`)}`);
            } else {
                lines.push(`        await self._client.request("${method.rpcMethod}", {}, **_timeout_kwargs(timeout))`);
            }
        }
    }
    lines.push(``);
}

function emitClientSessionApiRegistration(
    lines: string[],
    node: Record<string, unknown>,
    resolveType: (name: string) => string
): void {
    const groups = Object.entries(node).filter(([, value]) => typeof value === "object" && value !== null && !isRpcMethod(value));

    for (const [groupName, groupNode] of groups) {
        const handlerName = `${toPascalCase(groupName)}Handler`;
        const groupExperimental = isNodeFullyExperimental(groupNode as Record<string, unknown>);
        if (groupExperimental) {
            lines.push(`# Experimental: this API group is experimental and may change or be removed.`);
        }
        lines.push(`class ${handlerName}(Protocol):`);
        for (const [methodName, value] of Object.entries(groupNode as Record<string, unknown>)) {
            if (!isRpcMethod(value)) continue;
            emitClientSessionHandlerMethod(lines, methodName, value, resolveType, groupExperimental);
        }
        lines.push(``);
    }

    lines.push(`@dataclass`);
    lines.push(`class ClientSessionApiHandlers:`);
    if (groups.length === 0) {
        lines.push(`    pass`);
    } else {
        for (const [groupName] of groups) {
            lines.push(`    ${toSnakeCase(groupName)}: ${toPascalCase(groupName)}Handler | None = None`);
        }
    }
    lines.push(``);

    lines.push(`def register_client_session_api_handlers(`);
    lines.push(`    client: "JsonRpcClient",`);
    lines.push(`    get_handlers: Callable[[str], ClientSessionApiHandlers],`);
    lines.push(`) -> None:`);
    lines.push(`    """Register client-session request handlers on a JSON-RPC connection."""`);
    if (groups.length === 0) {
        lines.push(`    return`);
    } else {
        for (const [groupName, groupNode] of groups) {
            for (const [methodName, value] of Object.entries(groupNode as Record<string, unknown>)) {
                if (!isRpcMethod(value)) continue;
                emitClientSessionRegistrationMethod(
                    lines,
                    groupName,
                    methodName,
                    value,
                    resolveType
                );
            }
        }
    }
    lines.push(``);
}

function emitClientSessionHandlerMethod(
    lines: string[],
    name: string,
    method: RpcMethod,
    resolveType: (name: string) => string,
    groupExperimental = false
): void {
    const paramsType = resolveType(pythonParamsTypeName(method));
    const resultSchema = getMethodResultSchema(method);
    const resultType = !isVoidSchema(resultSchema) ? resolveType(pythonResultTypeName(method)) : "None";
    lines.push(`    async def ${toSnakeCase(name)}(self, params: ${paramsType}) -> ${resultType}:`);
    if (method.stability === "experimental" && !groupExperimental) {
        lines.push(`        """.. warning:: This API is experimental and may change or be removed in future versions."""`);
    }
    lines.push(`        pass`);
}

function emitClientSessionRegistrationMethod(
    lines: string[],
    groupName: string,
    methodName: string,
    method: RpcMethod,
    resolveType: (name: string) => string
): void {
    const handlerVariableName = `handle_${toSnakeCase(groupName)}_${toSnakeCase(methodName)}`;
    const paramsType = resolveType(pythonParamsTypeName(method));
    const resultSchema = getMethodResultSchema(method);
    const resultType = !isVoidSchema(resultSchema) ? resolveType(pythonResultTypeName(method)) : null;
    const handlerField = toSnakeCase(groupName);
    const handlerMethod = toSnakeCase(methodName);

    lines.push(`    async def ${handlerVariableName}(params: dict) -> dict | None:`);
    lines.push(`        request = ${paramsType}.from_dict(params)`);
    lines.push(`        handler = get_handlers(request.session_id).${handlerField}`);
    lines.push(
        `        if handler is None: raise RuntimeError(f"No ${handlerField} handler registered for session: {request.session_id}")`
    );
    if (resultType) {
        lines.push(`        result = await handler.${handlerMethod}(request)`);
        if (isObjectSchema(resultSchema)) {
            lines.push(`        return result.to_dict()`);
        } else {
            lines.push(`        return result.value if hasattr(result, 'value') else result`);
        }
    } else {
        lines.push(`        await handler.${handlerMethod}(request)`);
        lines.push(`        return None`);
    }
    lines.push(`    client.set_request_handler("${method.rpcMethod}", ${handlerVariableName})`);
}

// ── Main ────────────────────────────────────────────────────────────────────

async function generate(sessionSchemaPath?: string, apiSchemaPath?: string): Promise<void> {
    await generateSessionEvents(sessionSchemaPath);
    try {
        await generateRpc(apiSchemaPath);
    } catch (err) {
        if ((err as NodeJS.ErrnoException).code === "ENOENT" && !apiSchemaPath) {
            console.log("Python: skipping RPC (api.schema.json not found)");
        } else {
            throw err;
        }
    }
}

const __filename = fileURLToPath(import.meta.url);

if (process.argv[1] && path.resolve(process.argv[1]) === __filename) {
    const sessionArg = process.argv[2] || undefined;
    const apiArg = process.argv[3] || undefined;
    generate(sessionArg, apiArg).catch((err) => {
        console.error("Python generation failed:", err);
        process.exit(1);
    });
}
