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

export interface JSONSchema7WithDefs extends JSONSchema7 {
    $defs?: Record<string, JSONSchema7Definition>;
}

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

    const definitions = collectDefinitions(processed as Record<string, unknown>);
    if (Object.keys(definitions).length > 0) {
        const newDefs: Record<string, JSONSchema7Definition> = {};
        for (const [key, value] of Object.entries(definitions)) {
            newDefs[key] = typeof value === "object" ? postProcessSchema(value as JSONSchema7) : value;
        }
        processed.definitions = newDefs;
        processed.$defs = newDefs;
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

// ── $ref resolution ─────────────────────────────────────────────────────────

/** Extract the type name from a `$ref` path (e.g. "#/definitions/Model" → "Model"). */
export function refTypeName(ref: string): string {
    return ref.split("/").pop()!;
}

/** Resolve a `$ref` path against a definitions map, returning the referenced schema. */
export function resolveRef(
    ref: string,
    definitions: Record<string, JSONSchema7Definition> | undefined
): JSONSchema7 | undefined {
    const match = ref.match(/^#\/(definitions|\$defs)\/(.+)$/);
    if (!match || !definitions) return undefined;
    const def = definitions[match[2]];
    return typeof def === "object" ? (def as JSONSchema7) : undefined;
}

/** Collect the shared definitions from a schema (handles both `definitions` and `$defs`). */
export function collectDefinitions(
    schema: Record<string, unknown>
): Record<string, JSONSchema7Definition> {
    const legacyDefinitions = (schema.definitions ?? {}) as Record<string, JSONSchema7Definition>;
    const draft2019Definitions = (schema.$defs ?? {}) as Record<string, JSONSchema7Definition>;
    return { ...draft2019Definitions, ...legacyDefinitions };
}

export function withSharedDefinitions<T extends JSONSchema7>(
    schema: T,
    definitions: Record<string, JSONSchema7Definition>
): SchemaWithSharedDefinitions<T> {
    const sharedDefinitions = { ...definitions };
    return {
        ...schema,
        definitions: sharedDefinitions,
        $defs: sharedDefinitions,
    };
}
