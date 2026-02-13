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
 * Converts boolean const values to enum, filters excluded event types.
 */
export function postProcessSchema(schema: JSONSchema7): JSONSchema7 {
    if (typeof schema !== "object" || schema === null) return schema;

    const processed: JSONSchema7 = { ...schema };

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
            processed[combiner] = processed[combiner]!
                .filter((item) => {
                    if (typeof item !== "object") return true;
                    const typeConst = (item as JSONSchema7).properties?.type;
                    if (typeof typeConst === "object" && "const" in typeConst) {
                        return !EXCLUDED_EVENT_TYPES.has(typeConst.const as string);
                    }
                    return true;
                })
                .map((item) =>
                    typeof item === "object" ? postProcessSchema(item as JSONSchema7) : item
                ) as JSONSchema7Definition[];
        }
    }

    if (processed.definitions) {
        const newDefs: Record<string, JSONSchema7Definition> = {};
        for (const [key, value] of Object.entries(processed.definitions)) {
            newDefs[key] = typeof value === "object" ? postProcessSchema(value as JSONSchema7) : value;
        }
        processed.definitions = newDefs;
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
    result: JSONSchema7;
}

export interface ApiSchema {
    server?: Record<string, unknown>;
    session?: Record<string, unknown>;
}

export function isRpcMethod(node: unknown): node is RpcMethod {
    return typeof node === "object" && node !== null && "rpcMethod" in node;
}
