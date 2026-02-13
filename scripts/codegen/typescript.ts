/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * TypeScript code generator for session-events and RPC types.
 */

import fs from "fs/promises";
import type { JSONSchema7 } from "json-schema";
import { compile } from "json-schema-to-typescript";
import {
    getSessionEventsSchemaPath,
    getApiSchemaPath,
    postProcessSchema,
    writeGeneratedFile,
    isRpcMethod,
    type ApiSchema,
    type RpcMethod,
} from "./utils.js";

// ── Utilities ───────────────────────────────────────────────────────────────

function toPascalCase(s: string): string {
    return s.charAt(0).toUpperCase() + s.slice(1);
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

// ── Session Events ──────────────────────────────────────────────────────────

async function generateSessionEvents(schemaPath?: string): Promise<void> {
    console.log("TypeScript: generating session-events...");

    const resolvedPath = schemaPath ?? (await getSessionEventsSchemaPath());
    const schema = JSON.parse(await fs.readFile(resolvedPath, "utf-8")) as JSONSchema7;
    const processed = postProcessSchema(schema);

    const ts = await compile(processed, "SessionEvent", {
        bannerComment: `/**
 * AUTO-GENERATED FILE - DO NOT EDIT
 * Generated from: session-events.schema.json
 */`,
        style: { semi: true, singleQuote: false, trailingComma: "all" },
        additionalProperties: false,
    });

    const outPath = await writeGeneratedFile("nodejs/src/generated/session-events.ts", ts);
    console.log(`  ✓ ${outPath}`);
}

// ── RPC Types ───────────────────────────────────────────────────────────────

function resultTypeName(rpcMethod: string): string {
    return rpcMethod.split(".").map(toPascalCase).join("") + "Result";
}

function paramsTypeName(rpcMethod: string): string {
    return rpcMethod.split(".").map(toPascalCase).join("") + "Params";
}

async function generateRpc(schemaPath?: string): Promise<void> {
    console.log("TypeScript: generating RPC types...");

    const resolvedPath = schemaPath ?? (await getApiSchemaPath());
    const schema = JSON.parse(await fs.readFile(resolvedPath, "utf-8")) as ApiSchema;

    const lines: string[] = [];
    lines.push(`/**
 * AUTO-GENERATED FILE - DO NOT EDIT
 * Generated from: api.schema.json
 */

import type { MessageConnection } from "vscode-jsonrpc/node.js";
`);

    const allMethods = [...collectRpcMethods(schema.server || {}), ...collectRpcMethods(schema.session || {})];

    for (const method of allMethods) {
        const compiled = await compile(method.result, resultTypeName(method.rpcMethod), {
            bannerComment: "",
            additionalProperties: false,
        });
        lines.push(compiled.trim());
        lines.push("");

        if (method.params?.properties && Object.keys(method.params.properties).length > 0) {
            const paramsCompiled = await compile(method.params, paramsTypeName(method.rpcMethod), {
                bannerComment: "",
                additionalProperties: false,
            });
            lines.push(paramsCompiled.trim());
            lines.push("");
        }
    }

    // Generate factory functions
    if (schema.server) {
        lines.push(`/** Create typed server-scoped RPC methods (no session required). */`);
        lines.push(`export function createServerRpc(connection: MessageConnection) {`);
        lines.push(`    return {`);
        lines.push(...emitGroup(schema.server, "        ", false));
        lines.push(`    };`);
        lines.push(`}`);
        lines.push("");
    }

    if (schema.session) {
        lines.push(`/** Create typed session-scoped RPC methods. */`);
        lines.push(`export function createSessionRpc(connection: MessageConnection, sessionId: string) {`);
        lines.push(`    return {`);
        lines.push(...emitGroup(schema.session, "        ", true));
        lines.push(`    };`);
        lines.push(`}`);
        lines.push("");
    }

    const outPath = await writeGeneratedFile("nodejs/src/generated/rpc.ts", lines.join("\n"));
    console.log(`  ✓ ${outPath}`);
}

function emitGroup(node: Record<string, unknown>, indent: string, isSession: boolean): string[] {
    const lines: string[] = [];
    for (const [key, value] of Object.entries(node)) {
        if (isRpcMethod(value)) {
            const { rpcMethod, params } = value;
            const resultType = resultTypeName(rpcMethod);
            const paramsType = paramsTypeName(rpcMethod);

            const paramEntries = params?.properties ? Object.entries(params.properties).filter(([k]) => k !== "sessionId") : [];
            const hasParams = params?.properties && Object.keys(params.properties).length > 0;
            const hasNonSessionParams = paramEntries.length > 0;

            const sigParams: string[] = [];
            let bodyArg: string;

            if (isSession) {
                if (hasNonSessionParams) {
                    sigParams.push(`params: Omit<${paramsType}, "sessionId">`);
                    bodyArg = "{ sessionId, ...params }";
                } else {
                    bodyArg = "{ sessionId }";
                }
            } else {
                if (hasParams) {
                    sigParams.push(`params: ${paramsType}`);
                    bodyArg = "params";
                } else {
                    bodyArg = "{}";
                }
            }

            lines.push(`${indent}${key}: async (${sigParams.join(", ")}): Promise<${resultType}> =>`);
            lines.push(`${indent}    connection.sendRequest("${rpcMethod}", ${bodyArg}),`);
        } else if (typeof value === "object" && value !== null) {
            lines.push(`${indent}${key}: {`);
            lines.push(...emitGroup(value as Record<string, unknown>, indent + "    ", isSession));
            lines.push(`${indent}},`);
        }
    }
    return lines;
}

// ── Main ────────────────────────────────────────────────────────────────────

async function generate(sessionSchemaPath?: string, apiSchemaPath?: string): Promise<void> {
    await generateSessionEvents(sessionSchemaPath);
    try {
        await generateRpc(apiSchemaPath);
    } catch (err) {
        if ((err as NodeJS.ErrnoException).code === "ENOENT" && !apiSchemaPath) {
            console.log("TypeScript: skipping RPC (api.schema.json not found)");
        } else {
            throw err;
        }
    }
}

const sessionArg = process.argv[2] || undefined;
const apiArg = process.argv[3] || undefined;
generate(sessionArg, apiArg).catch((err) => {
    console.error("TypeScript generation failed:", err);
    process.exit(1);
});
