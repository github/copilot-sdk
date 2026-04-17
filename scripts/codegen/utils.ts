/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Shared utilities for code generation - schema loading, file I/O, schema processing.
 */

import { execFile } from "child_process";
import fs from "fs/promises";
import path from "path";
import { fileURLToPath } from "url";
import { promisify } from "util";
import type { JSONSchema7, JSONSchema7Definition } from "json-schema";

export const execFileAsync = promisify(execFile);

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

/** Root of the copilot-sdk repo */
export const REPO_ROOT = path.resolve(__dirname, "../..");

/** Event types to exclude from generation (internal/legacy types) */
export const EXCLUDED_EVENT_TYPES = new Set(["session.import_legacy"]);

export interface DefinitionCollections {
    definitions?: Record<string, JSONSchema7Definition>;
    $defs?: Record<string, JSONSchema7Definition>;
}

export interface JSONSchema7WithDefs extends JSONSchema7, DefinitionCollections {}

export type SchemaWithSharedDefinitions<T extends JSONSchema7 = JSONSchema7> = T & {
    definitions: Record<string, JSONSchema7Definition>;
    $defs: Record<string, JSONSchema7Definition>;
};
// ── Schema paths ────────────────────────────────────────────────────────────

export async function getSessionEventsSchemaPath(): Promise<string> {
    const schemaPath = path.join(
        REPO_ROOT,
        "nodejs/node_modules/@github/copilot/schemas/session-events.schema.json"
    );
    await fs.access(schemaPath);
    return schemaPath;
}

export async function getApiSchemaPath(cliArg?: string): Promise<string> {
    if (cliArg) return cliArg;
    const schemaPath = path.join(
        REPO_ROOT,
        "nodejs/node_modules/@github/copilot/schemas/api.schema.json"
    );
    await fs.access(schemaPath);
    return schemaPath;
}

// ── Schema processing ───────────────────────────────────────────────────────

/**
 * Post-process JSON Schema for quicktype compatibility.
 * Converts boolean const values to enum.
 */
export function postProcessSchema(schema: JSONSchema7): JSONSchema7 {
    if (typeof schema !== "object" || schema === null) return schema;

    const processed = { ...schema } as JSONSchema7WithDefs;

    if ("const" in processed && typeof processed.const === "boolean") {
        processed.enum = [processed.const];
        delete processed.const;
    }

    if (processed.properties) {
        const newProps: Record<string, JSONSchema7Definition> = {};
        for (const [key, value] of Object.entries(processed.properties)) {
            newProps[key] = typeof value === "object" ? postProcessSchema(value as JSONSchema7) : value;
        }
        processed.properties = newProps;
    }

    if (processed.items) {
        if (typeof processed.items === "object" && !Array.isArray(processed.items)) {
            processed.items = postProcessSchema(processed.items as JSONSchema7);
        } else if (Array.isArray(processed.items)) {
            processed.items = processed.items.map((item) =>
                typeof item === "object" ? postProcessSchema(item as JSONSchema7) : item
            ) as JSONSchema7Definition[];
        }
    }

    for (const combiner of ["anyOf", "allOf", "oneOf"] as const) {
        if (processed[combiner]) {
            processed[combiner] = processed[combiner]!.map((item) =>
                typeof item === "object" ? postProcessSchema(item as JSONSchema7) : item
            ) as JSONSchema7Definition[];
        }
    }

    const { definitions, $defs } = collectDefinitionCollections(processed as Record<string, unknown>);
    let newDefs: Record<string, JSONSchema7Definition> | undefined;
    if (Object.keys(definitions).length > 0) {
        newDefs = {};
        for (const [key, value] of Object.entries(definitions)) {
            newDefs[key] = typeof value === "object" ? postProcessSchema(value as JSONSchema7) : value;
        }
        processed.definitions = newDefs;
    }
    let newDraftDefs: Record<string, JSONSchema7Definition> | undefined;
    if (Object.keys($defs).length > 0) {
        newDraftDefs = {};
        for (const [key, value] of Object.entries($defs)) {
            newDraftDefs[key] = typeof value === "object" ? postProcessSchema(value as JSONSchema7) : value;
        }
        processed.$defs = newDraftDefs;
    }
    if (processed.definitions && !processed.$defs) {
        processed.$defs = { ...(newDefs ?? processed.definitions) };
    } else if (processed.$defs && !processed.definitions) {
        processed.definitions = { ...processed.$defs };
    }

    if (typeof processed.additionalProperties === "object") {
        processed.additionalProperties = postProcessSchema(processed.additionalProperties as JSONSchema7);
    }

    return processed;
}

// ── File output ─────────────────────────────────────────────────────────────

export async function writeGeneratedFile(relativePath: string, content: string): Promise<string> {
    const fullPath = path.join(REPO_ROOT, relativePath);
    await fs.mkdir(path.dirname(fullPath), { recursive: true });
    await fs.writeFile(fullPath, content, "utf-8");
    return fullPath;
}

// ── RPC schema types ────────────────────────────────────────────────────────

export interface RpcMethod {
    rpcMethod: string;
    params: JSONSchema7 | null;
    result: JSONSchema7 | null;
    stability?: string;
    deprecated?: boolean;
}

export function getRpcSchemaTypeName(schema: JSONSchema7 | null | undefined, fallback: string): string {
    if (typeof schema?.title === "string") return schema.title;
    return fallback;
}

/**
 * Returns true if the schema represents an object with properties (i.e., a type that should
 * be generated as a class/struct/dataclass). Returns false for enums, primitives, arrays,
 * and other non-object schemas.
 */
export function isObjectSchema(schema: JSONSchema7 | null | undefined): boolean {
    if (!schema) return false;
    if (schema.type === "object" && schema.properties) return true;
    return false;
}

/**
 * Returns true if the schema represents a void/null result (type: "null").
 * These carry a title for languages that need a named empty type (e.g., Go)
 * but should be treated as void in other languages.
 */
export function isVoidSchema(schema: JSONSchema7 | null | undefined): boolean {
    if (!schema) return true;
    return schema.type === "null";
}

export function cloneSchemaForCodegen<T>(value: T): T {
    if (Array.isArray(value)) {
        return value.map((item) => cloneSchemaForCodegen(item)) as T;
    }

    if (value && typeof value === "object") {
        const result: Record<string, unknown> = {};
        for (const [key, child] of Object.entries(value as Record<string, unknown>)) {
            if (key === "titleSource") {
                continue;
            }
            result[key] = cloneSchemaForCodegen(child);
        }

        return result as T;
    }

    return value;
}

export function stripNonAnnotationTitles<T>(value: T): T {
    if (Array.isArray(value)) {
        return value.map((item) => stripNonAnnotationTitles(item)) as T;
    }

    if (value && typeof value === "object") {
        const result: Record<string, unknown> = {};
        const source = value as Record<string, unknown>;
        const keepTitle = typeof source.title === "string" && source.titleSource === "annotation";
        for (const [key, child] of Object.entries(source)) {
            if (key === "titleSource") {
                continue;
            }
            if (key === "title" && !keepTitle) {
                continue;
            }
            result[key] = stripNonAnnotationTitles(child);
        }

        return result as T;
    }

    return value;
}

export function hoistTitledSchemas(
    rootDefinitions: Record<string, JSONSchema7>
): { rootDefinitions: Record<string, JSONSchema7>; sharedDefinitions: Record<string, JSONSchema7> } {
    const sharedDefinitions: Record<string, JSONSchema7> = {};
    const processedRoots: Record<string, JSONSchema7> = {};

    for (const [rootName, definition] of Object.entries(rootDefinitions)) {
        processedRoots[rootName] = visitSchema(definition, rootName, sharedDefinitions);
    }

    return { rootDefinitions: processedRoots, sharedDefinitions };
}

function visitSchema(
    schema: JSONSchema7,
    rootName: string,
    sharedDefinitions: Record<string, JSONSchema7>
): JSONSchema7 {
    const result: JSONSchema7 = { ...schema };

    if (result.properties) {
        result.properties = Object.fromEntries(
            Object.entries(result.properties).map(([key, value]) => [
                key,
                typeof value === "object" && value !== null && !Array.isArray(value)
                    ? visitSchema(value as JSONSchema7, rootName, sharedDefinitions)
                    : value,
            ])
        );
    }

    if (result.items) {
        if (Array.isArray(result.items)) {
            result.items = result.items.map((item) =>
                typeof item === "object" && item !== null && !Array.isArray(item)
                    ? visitSchema(item as JSONSchema7, rootName, sharedDefinitions)
                    : item
            ) as JSONSchema7Definition[];
        } else if (typeof result.items === "object" && result.items !== null) {
            result.items = visitSchema(result.items as JSONSchema7, rootName, sharedDefinitions);
        }
    }

    if (typeof result.additionalProperties === "object" && result.additionalProperties !== null) {
        result.additionalProperties = visitSchema(result.additionalProperties as JSONSchema7, rootName, sharedDefinitions);
    }

    for (const combiner of ["anyOf", "allOf", "oneOf"] as const) {
        if (result[combiner]) {
            result[combiner] = result[combiner]!.map((item) =>
                typeof item === "object" && item !== null && !Array.isArray(item)
                    ? visitSchema(item as JSONSchema7, rootName, sharedDefinitions)
                    : item
            ) as JSONSchema7Definition[];
        }
    }

    if (typeof result.title === "string" && result.title !== rootName) {
        const existing = sharedDefinitions[result.title];
        if (existing) {
            if (stableStringify(existing) !== stableStringify(result)) {
                throw new Error(`Conflicting titled schemas for "${result.title}" while preparing quicktype inputs.`);
            }
        } else {
            sharedDefinitions[result.title] = result;
        }
        return { $ref: `#/definitions/${result.title}`, description: result.description } as JSONSchema7;
    }

    return result;
}

function stableStringify(value: unknown): string {
    return JSON.stringify(sortJsonValue(value));
}

function sortJsonValue(value: unknown): unknown {
    if (Array.isArray(value)) {
        return value.map(sortJsonValue);
    }

    if (value && typeof value === "object") {
        return Object.fromEntries(
            Object.entries(value as Record<string, unknown>)
                .filter(([key]) => key !== "description" && key !== "titleSource")
                .sort(([left], [right]) => left.localeCompare(right))
                .map(([key, child]) => [key, sortJsonValue(child)])
        );
    }

    return value;
}

export interface ApiSchema {
    definitions?: Record<string, JSONSchema7Definition>;
    $defs?: Record<string, JSONSchema7Definition>;
    server?: Record<string, unknown>;
    session?: Record<string, unknown>;
    clientSession?: Record<string, unknown>;
}

export function isRpcMethod(node: unknown): node is RpcMethod {
    return typeof node === "object" && node !== null && "rpcMethod" in node;
}

function normalizeSchemaDefinitionTitles(definition: JSONSchema7Definition): JSONSchema7Definition {
    return typeof definition === "object" && definition !== null
        ? normalizeSchemaTitles(definition as JSONSchema7)
        : definition;
}

export function normalizeSchemaTitles(schema: JSONSchema7): JSONSchema7 {
    if (typeof schema !== "object" || schema === null) return schema;

    const normalized = { ...schema } as JSONSchema7WithDefs & Record<string, unknown>;
    delete normalized.title;
    delete normalized.titleSource;

    if (normalized.properties) {
        const newProps: Record<string, JSONSchema7Definition> = {};
        for (const [key, value] of Object.entries(normalized.properties)) {
            newProps[key] = normalizeSchemaDefinitionTitles(value);
        }
        normalized.properties = newProps;
    }

    if (normalized.items) {
        if (typeof normalized.items === "object" && !Array.isArray(normalized.items)) {
            normalized.items = normalizeSchemaTitles(normalized.items as JSONSchema7);
        } else if (Array.isArray(normalized.items)) {
            normalized.items = normalized.items.map((item) => normalizeSchemaDefinitionTitles(item)) as JSONSchema7Definition[];
        }
    }

    for (const combiner of ["anyOf", "allOf", "oneOf"] as const) {
        if (normalized[combiner]) {
            normalized[combiner] = normalized[combiner]!.map((item) => normalizeSchemaDefinitionTitles(item)) as JSONSchema7Definition[];
        }
    }

    if (normalized.additionalProperties && typeof normalized.additionalProperties === "object") {
        normalized.additionalProperties = normalizeSchemaTitles(normalized.additionalProperties as JSONSchema7);
    }

    if (normalized.propertyNames && typeof normalized.propertyNames === "object" && !Array.isArray(normalized.propertyNames)) {
        normalized.propertyNames = normalizeSchemaTitles(normalized.propertyNames as JSONSchema7);
    }

    if (normalized.contains && typeof normalized.contains === "object" && !Array.isArray(normalized.contains)) {
        normalized.contains = normalizeSchemaTitles(normalized.contains as JSONSchema7);
    }

    if (normalized.not && typeof normalized.not === "object" && !Array.isArray(normalized.not)) {
        normalized.not = normalizeSchemaTitles(normalized.not as JSONSchema7);
    }

    if (normalized.if && typeof normalized.if === "object" && !Array.isArray(normalized.if)) {
        normalized.if = normalizeSchemaTitles(normalized.if as JSONSchema7);
    }
    if (normalized.then && typeof normalized.then === "object" && !Array.isArray(normalized.then)) {
        normalized.then = normalizeSchemaTitles(normalized.then as JSONSchema7);
    }
    if (normalized.else && typeof normalized.else === "object" && !Array.isArray(normalized.else)) {
        normalized.else = normalizeSchemaTitles(normalized.else as JSONSchema7);
    }

    if (normalized.patternProperties) {
        const newPatternProps: Record<string, JSONSchema7Definition> = {};
        for (const [key, value] of Object.entries(normalized.patternProperties)) {
            newPatternProps[key] = normalizeSchemaDefinitionTitles(value);
        }
        normalized.patternProperties = newPatternProps;
    }

    const { definitions, $defs } = collectDefinitionCollections(normalized as Record<string, unknown>);
    if (Object.keys(definitions).length > 0) {
        const newDefs: Record<string, JSONSchema7Definition> = {};
        for (const [key, value] of Object.entries(definitions)) {
            newDefs[key] = normalizeSchemaDefinitionTitles(value);
        }
        normalized.definitions = newDefs;
    }
    if (Object.keys($defs).length > 0) {
        const newDraftDefs: Record<string, JSONSchema7Definition> = {};
        for (const [key, value] of Object.entries($defs)) {
            newDraftDefs[key] = normalizeSchemaDefinitionTitles(value);
        }
        normalized.$defs = newDraftDefs;
    }

    return normalized;
}

function normalizeApiNode(node: Record<string, unknown> | undefined): Record<string, unknown> | undefined {
    if (!node) return undefined;

    const normalizedNode: Record<string, unknown> = {};
    for (const [key, value] of Object.entries(node)) {
        if (isRpcMethod(value)) {
            const method = value as RpcMethod;
            normalizedNode[key] = {
                ...method,
                params: method.params ? normalizeSchemaTitles(method.params) : method.params,
                result: method.result ? normalizeSchemaTitles(method.result) : method.result,
            };
        } else if (typeof value === "object" && value !== null) {
            normalizedNode[key] = normalizeApiNode(value as Record<string, unknown>);
        } else {
            normalizedNode[key] = value;
        }
    }

    return normalizedNode;
}

export function normalizeApiSchema(schema: ApiSchema): ApiSchema {
    return {
        ...schema,
        definitions: schema.definitions
            ? Object.fromEntries(
                  Object.entries(schema.definitions).map(([key, value]) => [key, normalizeSchemaDefinitionTitles(value)])
              )
            : schema.definitions,
        $defs: schema.$defs
            ? Object.fromEntries(
                  Object.entries(schema.$defs).map(([key, value]) => [key, normalizeSchemaDefinitionTitles(value)])
              )
            : schema.$defs,
        server: normalizeApiNode(schema.server),
        session: normalizeApiNode(schema.session),
        clientSession: normalizeApiNode(schema.clientSession),
    };
}

/** Returns true when every leaf RPC method inside `node` is marked experimental. */
export function isNodeFullyExperimental(node: Record<string, unknown>): boolean {
    const methods: RpcMethod[] = [];
    (function collect(n: Record<string, unknown>) {
        for (const value of Object.values(n)) {
            if (isRpcMethod(value)) {
                methods.push(value);
            } else if (typeof value === "object" && value !== null) {
                collect(value as Record<string, unknown>);
            }
        }
    })(node);
    return methods.length > 0 && methods.every(m => m.stability === "experimental");
}

/** Returns true when every leaf RPC method inside `node` is marked deprecated. */
export function isNodeFullyDeprecated(node: Record<string, unknown>): boolean {
    const methods: RpcMethod[] = [];
    (function collect(n: Record<string, unknown>) {
        for (const value of Object.values(n)) {
            if (isRpcMethod(value)) {
                methods.push(value);
            } else if (typeof value === "object" && value !== null) {
                collect(value as Record<string, unknown>);
            }
        }
    })(node);
    return methods.length > 0 && methods.every(m => m.deprecated === true);
}

/** Returns true when a JSON Schema node is marked as deprecated. */
export function isSchemaDeprecated(schema: JSONSchema7 | null | undefined): boolean {
    return typeof schema === "object" && schema !== null && (schema as Record<string, unknown>).deprecated === true;
}

// ── $ref resolution ─────────────────────────────────────────────────────────

/** Extract the generated type name from a `$ref` path (e.g. "#/definitions/Model" → "Model"). */
export function refTypeName(ref: string, definitions?: DefinitionCollections): string {
    const baseName = ref.split("/").pop()!;
    const match = ref.match(/^#\/(definitions|\$defs)\/(.+)$/);
    if (!match || match[1] !== "$defs" || !definitions) return baseName;

    const key = match[2];
    const legacyDefinition = definitions.definitions?.[key];
    const draftDefinition = definitions.$defs?.[key];
    if (
        legacyDefinition !== undefined &&
        draftDefinition !== undefined &&
        stableStringify(legacyDefinition) !== stableStringify(draftDefinition)
    ) {
        return `Draft${baseName}`;
    }

    return baseName;
}

/** Resolve a `$ref` path against a definitions map, returning the referenced schema. */
export function resolveRef(
    ref: string,
    definitions: DefinitionCollections | undefined
): JSONSchema7 | undefined {
    const match = ref.match(/^#\/(definitions|\$defs)\/(.+)$/);
    if (!match || !definitions) return undefined;
    const [, namespace, key] = match;
    const primary = namespace === "$defs" ? definitions.$defs : definitions.definitions;
    const fallback = namespace === "$defs" ? definitions.definitions : definitions.$defs;
    const def = primary?.[key] ?? fallback?.[key];
    return typeof def === "object" ? (def as JSONSchema7) : undefined;
}

export function resolveSchema(
    schema: JSONSchema7 | null | undefined,
    definitions: DefinitionCollections | undefined
): JSONSchema7 | undefined {
    let current = schema ?? undefined;
    const seenRefs = new Set<string>();
    while (current?.$ref) {
        if (seenRefs.has(current.$ref)) break;
        seenRefs.add(current.$ref);
        const resolved = resolveRef(current.$ref, definitions);
        if (!resolved) break;
        current = resolved;
    }
    return current;
}

export function resolveObjectSchema(
    schema: JSONSchema7 | null | undefined,
    definitions: DefinitionCollections | undefined
): JSONSchema7 | undefined {
    const resolved = resolveSchema(schema, definitions) ?? schema ?? undefined;
    if (!resolved) return undefined;
    if (resolved.properties || resolved.additionalProperties || resolved.type === "object") return resolved;

    if (resolved.allOf) {
        const mergedProperties: Record<string, JSONSchema7Definition> = {};
        const mergedRequired = new Set<string>();
        const merged: JSONSchema7 = {
            type: "object",
            description: resolved.description,
        };
        let hasObjectShape = false;

        for (const item of resolved.allOf) {
            if (typeof item !== "object") continue;
            const objectSchema = resolveObjectSchema(item as JSONSchema7, definitions);
            if (!objectSchema) continue;

            if (objectSchema.properties) {
                Object.assign(mergedProperties, objectSchema.properties);
                hasObjectShape = true;
            }
            if (objectSchema.required) {
                for (const name of objectSchema.required) {
                    mergedRequired.add(name);
                }
            }
            if (objectSchema.additionalProperties !== undefined) {
                merged.additionalProperties = objectSchema.additionalProperties;
                hasObjectShape = true;
            }
            if (!merged.description && objectSchema.description) {
                merged.description = objectSchema.description;
            }
        }

        if (!hasObjectShape) return resolved;
        if (Object.keys(mergedProperties).length > 0) {
            merged.properties = mergedProperties;
        }
        if (mergedRequired.size > 0) {
            merged.required = [...mergedRequired];
        }
        return merged;
    }

    const singleBranch = (resolved.anyOf ?? resolved.oneOf)
        ?.filter((item): item is JSONSchema7 => typeof item === "object" && (item as JSONSchema7).type !== "null");
    if (singleBranch && singleBranch.length === 1) {
        return resolveObjectSchema(singleBranch[0], definitions);
    }

    return resolved;
}

export function hasSchemaPayload(schema: JSONSchema7 | null | undefined): boolean {
    if (!schema) return false;
    if (schema.properties) return Object.keys(schema.properties).length > 0;
    if (schema.additionalProperties) return true;
    if (schema.items) return true;
    if (schema.anyOf || schema.oneOf || schema.allOf) return true;
    if (schema.enum && schema.enum.length > 0) return true;
    if (schema.const !== undefined) return true;
    if (schema.$ref) return true;
    if (Array.isArray(schema.type)) return schema.type.length > 0 && !(schema.type.length === 1 && schema.type[0] === "object");
    return schema.type !== undefined && schema.type !== "object";
}

export function collectDefinitionCollections(
    schema: Record<string, unknown>
): Required<DefinitionCollections> {
    return {
        definitions: { ...((schema.definitions ?? {}) as Record<string, JSONSchema7Definition>) },
        $defs: { ...((schema.$defs ?? {}) as Record<string, JSONSchema7Definition>) },
    };
}

/** Collect the shared definitions from a schema (handles both `definitions` and `$defs`). */
export function collectDefinitions(
    schema: Record<string, unknown>
): Record<string, JSONSchema7Definition> {
    const { definitions, $defs } = collectDefinitionCollections(schema);
    return { ...$defs, ...definitions };
}

export function withSharedDefinitions<T extends JSONSchema7>(
    schema: T,
    definitions: DefinitionCollections
): SchemaWithSharedDefinitions<T> {
    const legacyDefinitions = { ...(definitions.definitions ?? {}) };
    const draft2019Definitions = { ...(definitions.$defs ?? {}) };

    const sharedLegacyDefinitions =
        Object.keys(legacyDefinitions).length > 0 ? legacyDefinitions : { ...draft2019Definitions };
    const sharedDraftDefinitions =
        Object.keys(draft2019Definitions).length > 0 ? draft2019Definitions : { ...legacyDefinitions };

    return {
        ...schema,
        definitions: sharedLegacyDefinitions,
        $defs: sharedDraftDefinitions,
    };
}
