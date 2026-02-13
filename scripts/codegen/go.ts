/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Go code generator for session-events and RPC types.
 */

import { execFile } from "child_process";
import fs from "fs/promises";
import { promisify } from "util";
import type { JSONSchema7 } from "json-schema";
import { FetchingJSONSchemaStore, InputData, JSONSchemaInput, quicktype } from "quicktype-core";
import {
    getSessionEventsSchemaPath,
    getApiSchemaPath,
    postProcessSchema,
    writeGeneratedFile,
    isRpcMethod,
    type ApiSchema,
    type RpcMethod,
} from "./utils.js";

const execFileAsync = promisify(execFile);

// ── Utilities ───────────────────────────────────────────────────────────────

// Go initialisms that should be all-caps
const goInitialisms = new Set(["id", "url", "api", "http", "https", "json", "xml", "html", "css", "sql", "ssh", "tcp", "udp", "ip", "rpc"]);

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

// ── Session Events ──────────────────────────────────────────────────────────

async function generateSessionEvents(schemaPath?: string): Promise<void> {
    console.log("Go: generating session-events...");

    const resolvedPath = schemaPath ?? (await getSessionEventsSchemaPath());
    const schema = JSON.parse(await fs.readFile(resolvedPath, "utf-8")) as JSONSchema7;
    const resolvedSchema = (schema.definitions?.SessionEvent as JSONSchema7) || schema;
    const processed = postProcessSchema(resolvedSchema);

    const schemaInput = new JSONSchemaInput(new FetchingJSONSchemaStore());
    await schemaInput.addSource({ name: "SessionEvent", schema: JSON.stringify(processed) });

    const inputData = new InputData();
    inputData.addInput(schemaInput);

    const result = await quicktype({
        inputData,
        lang: "go",
        rendererOptions: { package: "copilot" },
    });

    const banner = `// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

`;

    const outPath = await writeGeneratedFile("go/generated_session_events.go", banner + result.lines.join("\n"));
    console.log(`  ✓ ${outPath}`);

    await formatGoFile(outPath);
}

// ── RPC Types ───────────────────────────────────────────────────────────────

async function generateRpc(schemaPath?: string): Promise<void> {
    console.log("Go: generating RPC types...");

    const resolvedPath = schemaPath ?? (await getApiSchemaPath());
    const schema = JSON.parse(await fs.readFile(resolvedPath, "utf-8")) as ApiSchema;

    const allMethods = [...collectRpcMethods(schema.server || {}), ...collectRpcMethods(schema.session || {})];

    // Build a combined schema for quicktype - prefix types to avoid conflicts
    const combinedSchema: JSONSchema7 = {
        $schema: "http://json-schema.org/draft-07/schema#",
        definitions: {},
    };

    for (const method of allMethods) {
        const baseName = toPascalCase(method.rpcMethod);
        if (method.result) {
            combinedSchema.definitions![baseName + "Result"] = method.result;
        }
        if (method.params?.properties && Object.keys(method.params.properties).length > 0) {
            // For session methods, filter out sessionId from params type
            if (method.rpcMethod.startsWith("session.")) {
                const filtered: JSONSchema7 = {
                    ...method.params,
                    properties: Object.fromEntries(
                        Object.entries(method.params.properties).filter(([k]) => k !== "sessionId")
                    ),
                    required: method.params.required?.filter((r) => r !== "sessionId"),
                };
                if (Object.keys(filtered.properties!).length > 0) {
                    combinedSchema.definitions![baseName + "Params"] = filtered;
                }
            } else {
                combinedSchema.definitions![baseName + "Params"] = method.params;
            }
        }
    }

    // Generate types via quicktype
    const schemaInput = new JSONSchemaInput(new FetchingJSONSchemaStore());
    for (const [name, def] of Object.entries(combinedSchema.definitions!)) {
        await schemaInput.addSource({ name, schema: JSON.stringify(def) });
    }

    const inputData = new InputData();
    inputData.addInput(schemaInput);

    const qtResult = await quicktype({
        inputData,
        lang: "go",
        rendererOptions: { package: "copilot", "just-types": "true" },
    });

    // Build method wrappers
    const lines: string[] = [];
    lines.push(`// AUTO-GENERATED FILE - DO NOT EDIT`);
    lines.push(`// Generated from: api.schema.json`);
    lines.push(``);
    lines.push(`package rpc`);
    lines.push(``);
    lines.push(`import (`);
    lines.push(`    "context"`);
    lines.push(`    "encoding/json"`);
    lines.push(``);
    lines.push(`    "github.com/github/copilot-sdk/go/internal/jsonrpc2"`);
    lines.push(`)`);
    lines.push(``);

    // Add quicktype-generated types (skip package line)
    const qtLines = qtResult.lines.filter((l) => !l.startsWith("package "));
    lines.push(...qtLines);
    lines.push(``);

    // Emit ServerRpc
    if (schema.server) {
        emitRpcWrapper(lines, schema.server, false);
    }

    // Emit SessionRpc
    if (schema.session) {
        emitRpcWrapper(lines, schema.session, true);
    }

    const outPath = await writeGeneratedFile("go/rpc/generated_rpc.go", lines.join("\n"));
    console.log(`  ✓ ${outPath}`);

    await formatGoFile(outPath);
}

function emitRpcWrapper(lines: string[], node: Record<string, unknown>, isSession: boolean): void {
    const groups = Object.entries(node).filter(([, v]) => typeof v === "object" && v !== null && !isRpcMethod(v));
    const topLevelMethods = Object.entries(node).filter(([, v]) => isRpcMethod(v));

    const wrapperName = isSession ? "SessionRpc" : "ServerRpc";
    const apiSuffix = "RpcApi";

    // Emit API structs for groups
    for (const [groupName, groupNode] of groups) {
        const apiName = toPascalCase(groupName) + apiSuffix;
        const fields = isSession ? "client *jsonrpc2.Client; sessionID string" : "client *jsonrpc2.Client";
        lines.push(`type ${apiName} struct { ${fields} }`);
        lines.push(``);
        for (const [key, value] of Object.entries(groupNode as Record<string, unknown>)) {
            if (!isRpcMethod(value)) continue;
            emitMethod(lines, apiName, key, value, isSession);
        }
    }

    // Emit wrapper struct
    lines.push(`// ${wrapperName} provides typed ${isSession ? "session" : "server"}-scoped RPC methods.`);
    lines.push(`type ${wrapperName} struct {`);
    lines.push(`    client *jsonrpc2.Client`);
    if (isSession) lines.push(`    sessionID string`);
    for (const [groupName] of groups) {
        lines.push(`    ${toPascalCase(groupName)} *${toPascalCase(groupName)}${apiSuffix}`);
    }
    lines.push(`}`);
    lines.push(``);

    // Top-level methods (server only)
    for (const [key, value] of topLevelMethods) {
        if (!isRpcMethod(value)) continue;
        emitMethod(lines, wrapperName, key, value, isSession);
    }

    // Constructor
    const ctorParams = isSession ? "client *jsonrpc2.Client, sessionID string" : "client *jsonrpc2.Client";
    const ctorFields = isSession ? "client: client, sessionID: sessionID," : "client: client,";
    lines.push(`func New${wrapperName}(${ctorParams}) *${wrapperName} {`);
    lines.push(`    return &${wrapperName}{${ctorFields}`);
    for (const [groupName] of groups) {
        const apiInit = isSession
            ? `&${toPascalCase(groupName)}${apiSuffix}{client: client, sessionID: sessionID}`
            : `&${toPascalCase(groupName)}${apiSuffix}{client: client}`;
        lines.push(`        ${toPascalCase(groupName)}: ${apiInit},`);
    }
    lines.push(`    }`);
    lines.push(`}`);
    lines.push(``);
}

function emitMethod(lines: string[], receiver: string, name: string, method: RpcMethod, isSession: boolean): void {
    const methodName = toPascalCase(name);
    const resultType = toPascalCase(method.rpcMethod) + "Result";

    const paramProps = method.params?.properties || {};
    const nonSessionParams = Object.keys(paramProps).filter((k) => k !== "sessionId");
    const hasParams = isSession ? nonSessionParams.length > 0 : Object.keys(paramProps).length > 0;
    const paramsType = hasParams ? toPascalCase(method.rpcMethod) + "Params" : "";

    const sig = hasParams
        ? `func (a *${receiver}) ${methodName}(ctx context.Context, params *${paramsType}) (*${resultType}, error)`
        : `func (a *${receiver}) ${methodName}(ctx context.Context) (*${resultType}, error)`;

    lines.push(sig + ` {`);

    if (isSession) {
        lines.push(`    req := map[string]interface{}{"sessionId": a.sessionID}`);
        if (hasParams) {
            lines.push(`    if params != nil {`);
            for (const pName of nonSessionParams) {
                lines.push(`        req["${pName}"] = params.${toGoFieldName(pName)}`);
            }
            lines.push(`    }`);
        }
        lines.push(`    raw, err := a.client.Request("${method.rpcMethod}", req)`);
    } else {
        const arg = hasParams ? "params" : "map[string]interface{}{}";
        lines.push(`    raw, err := a.client.Request("${method.rpcMethod}", ${arg})`);
    }

    lines.push(`    if err != nil { return nil, err }`);
    lines.push(`    var result ${resultType}`);
    lines.push(`    if err := json.Unmarshal(raw, &result); err != nil { return nil, err }`);
    lines.push(`    return &result, nil`);
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
