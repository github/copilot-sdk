/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Generate TypeScript RPC client wrappers from api.schema.json.
 *
 * Uses json-schema-to-typescript for all type generation (same as session-events).
 * Only custom code is for method signatures (which no library produces).
 */

import fs from "fs/promises";
import path from "path";
import { fileURLToPath } from "url";
import type { JSONSchema7 } from "json-schema";
import { compile } from "json-schema-to-typescript";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

interface RpcMethod {
    rpcMethod: string;
    params: JSONSchema7 | null;
    result: JSONSchema7;
}

interface SchemaRoot {
    server?: Record<string, unknown>;
    session?: Record<string, unknown>;
}

function isRpcMethod(node: unknown): node is RpcMethod {
    return typeof node === "object" && node !== null && "rpcMethod" in node;
}

function pascal(s: string): string {
    return s.charAt(0).toUpperCase() + s.slice(1);
}

/** Convert "models.list" → "ModelsListResult" */
function resultTypeName(rpcMethod: string): string {
    return rpcMethod.split(".").map(pascal).join("") + "Result";
}

/** Convert "models.list" → "ModelsListParams" */
function paramsTypeName(rpcMethod: string): string {
    return rpcMethod.split(".").map(pascal).join("") + "Params";
}

/** Collect all RPC methods from schema. */
function collectMethods(node: Record<string, unknown>): RpcMethod[] {
    const results: RpcMethod[] = [];
    for (const value of Object.values(node)) {
        if (isRpcMethod(value)) {
            results.push(value);
        } else if (typeof value === "object" && value !== null) {
            results.push(...collectMethods(value as Record<string, unknown>));
        }
    }
    return results;
}

/** Emit method group recursively. */
function emitGroup(
    node: Record<string, unknown>,
    indent: string,
    isSession: boolean
): string[] {
    const lines: string[] = [];
    for (const [key, value] of Object.entries(node)) {
        if (isRpcMethod(value)) {
            const { rpcMethod, params } = value;
            const resultType = resultTypeName(rpcMethod);
            const paramsType = paramsTypeName(rpcMethod);

            // Build params for function signature
            const sigParams = ["connection: MessageConnection"];
            if (isSession) sigParams.push("sessionId: string");

            const hasParams = params?.properties && Object.keys(params.properties).length > 0;
            const hasNonSessionParams = hasParams && Object.keys(params!.properties!).some(k => k !== "sessionId");

            if (hasNonSessionParams) {
                sigParams.push(`params: Omit<${paramsType}, "sessionId">`);
            }

            // Build body args
            let bodyArg: string;
            if (isSession && hasNonSessionParams) {
                bodyArg = "{ sessionId, ...params }";
            } else if (isSession) {
                bodyArg = "{ sessionId }";
            } else if (hasParams) {
                bodyArg = "params";
            } else {
                bodyArg = "{}";
            }

            lines.push("");
            lines.push(`${indent}${key}: async (${sigParams.join(", ")}): Promise<${resultType}> => {`);
            lines.push(`${indent}    return await connection.sendRequest("${rpcMethod}", ${bodyArg});`);
            lines.push(`${indent}},`);
        } else if (typeof value === "object" && value !== null) {
            lines.push("");
            lines.push(`${indent}${key}: {`);
            lines.push(...emitGroup(value as Record<string, unknown>, indent + "    ", isSession));
            lines.push(`${indent}},`);
        }
    }
    return lines;
}

async function getApiSchemaPath(): Promise<string> {
    const schemaPath = path.join(__dirname, "../../../copilot-agent-runtime/generated/api.schema.json");
    await fs.access(schemaPath);
    console.log(`✅ Found API schema at: ${schemaPath}`);
    return schemaPath;
}

export async function generateTypeScriptRpc(schemaPath?: string): Promise<void> {
    console.log("🔄 Generating TypeScript RPC types…");

    const resolvedPath = schemaPath ?? (await getApiSchemaPath());
    const schema = JSON.parse(await fs.readFile(resolvedPath, "utf-8")) as SchemaRoot;

    const lines: string[] = [];
    lines.push(`/**
 * AUTO-GENERATED FILE - DO NOT EDIT
 * Generated from: api.schema.json
 */

import type { MessageConnection } from "vscode-jsonrpc/node.js";
`);

    // Compile all result and params types using json-schema-to-typescript
    const allMethods = [
        ...collectMethods(schema.server || {}),
        ...collectMethods(schema.session || {}),
    ];

    for (const method of allMethods) {
        // Result type
        const compiled = await compile(method.result, resultTypeName(method.rpcMethod), {
            bannerComment: "",
            additionalProperties: false,
        });
        lines.push(compiled.trim());
        lines.push("");

        // Params type (if has params)
        if (method.params?.properties && Object.keys(method.params.properties).length > 0) {
            const paramsCompiled = await compile(method.params, paramsTypeName(method.rpcMethod), {
                bannerComment: "",
                additionalProperties: false,
            });
            lines.push(paramsCompiled.trim());
            lines.push("");
        }
    }

    // Emit serverRpc
    if (schema.server) {
        lines.push(`export const serverRpc = {`);
        lines.push(...emitGroup(schema.server, "    ", false));
        lines.push(`};`);
        lines.push("");
    }

    // Emit sessionRpc
    if (schema.session) {
        lines.push(`export const sessionRpc = {`);
        lines.push(...emitGroup(schema.session, "    ", true));
        lines.push(`};`);
        lines.push("");
    }

    const outputPath = path.join(__dirname, "../src/generated/rpc.ts");
    await fs.mkdir(path.dirname(outputPath), { recursive: true });
    await fs.writeFile(outputPath, lines.join("\n"), "utf-8");
    console.log(`✅ Generated TypeScript RPC types: ${outputPath}`);
}

// Run if invoked directly
if (process.argv[1]?.match(/generate-rpc-types\.[tj]s$/)) {
    generateTypeScriptRpc(process.argv[2]).catch((err) => {
        console.error("❌ TypeScript RPC generation failed:", err);
        process.exit(1);
    });
}
