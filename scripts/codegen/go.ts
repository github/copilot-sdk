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
    isNodeFullyExperimental,
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

    const outPath = await writeGeneratedFile("go/generated_session_events.go", banner + postProcessEnumConstants(result.lines.join("\n")));
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

    // Post-process quicktype output: fix enum constant names
    let qtCode = qtResult.lines.filter((l) => !l.startsWith("package ")).join("\n");
    qtCode = postProcessEnumConstants(qtCode);
    // Strip trailing whitespace from quicktype output (gofmt requirement)
    qtCode = qtCode.replace(/[ \t]+$/gm, "");

    // Extract actual type names generated by quicktype (may differ from toPascalCase)
    const actualTypeNames = new Map<string, string>();
    const structRe = /^type\s+(\w+)\s+struct\b/gm;
    let sm;
    while ((sm = structRe.exec(qtCode)) !== null) {
        actualTypeNames.set(sm[1].toLowerCase(), sm[1]);
    }
    const resolveType = (name: string): string => actualTypeNames.get(name.toLowerCase()) ?? name;

    // Extract field name mappings (quicktype may rename fields to avoid Go keyword conflicts)
    const fieldNames = extractFieldNames(qtCode);

    // Annotate experimental data types
    const experimentalTypeNames = new Set<string>();
    for (const method of allMethods) {
        if (method.stability !== "experimental") continue;
        experimentalTypeNames.add(toPascalCase(method.rpcMethod) + "Result");
        const baseName = toPascalCase(method.rpcMethod);
        if (combinedSchema.definitions![baseName + "Params"]) {
            experimentalTypeNames.add(baseName + "Params");
        }
    }
    for (const typeName of experimentalTypeNames) {
        qtCode = qtCode.replace(
            new RegExp(`^(type ${typeName} struct)`, "m"),
            `// Experimental: ${typeName} is part of an experimental API and may change or be removed.\n$1`
        );
    }
    // Remove trailing blank lines from quicktype output before appending
    qtCode = qtCode.replace(/\n+$/, "");

    // Build method wrappers
    const lines: string[] = [];
    lines.push(`// AUTO-GENERATED FILE - DO NOT EDIT`);
    lines.push(`// Generated from: api.schema.json`);
    lines.push(``);
    lines.push(`package rpc`);
    lines.push(``);
    lines.push(`import (`);
    lines.push(`\t"context"`);
    lines.push(`\t"encoding/json"`);
    lines.push(``);
    lines.push(`\t"github.com/github/copilot-sdk/go/internal/jsonrpc2"`);
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

    const outPath = await writeGeneratedFile("go/rpc/generated_rpc.go", lines.join("\n"));
    console.log(`  ✓ ${outPath}`);

    await formatGoFile(outPath);
}

function emitRpcWrapper(lines: string[], node: Record<string, unknown>, isSession: boolean, resolveType: (name: string) => string, fieldNames: Map<string, Map<string, string>>): void {
    const groups = Object.entries(node).filter(([, v]) => typeof v === "object" && v !== null && !isRpcMethod(v));
    const topLevelMethods = Object.entries(node).filter(([, v]) => isRpcMethod(v));

    const wrapperName = isSession ? "SessionRpc" : "ServerRpc";
    const apiSuffix = "RpcApi";

    // Emit API structs for groups
    for (const [groupName, groupNode] of groups) {
        const prefix = isSession ? "" : "Server";
        const apiName = prefix + toPascalCase(groupName) + apiSuffix;
        const groupExperimental = isNodeFullyExperimental(groupNode as Record<string, unknown>);
        if (groupExperimental) {
            lines.push(`// Experimental: ${apiName} contains experimental APIs that may change or be removed.`);
        }
        lines.push(`type ${apiName} struct {`);
        if (isSession) {
            lines.push(`\tclient    *jsonrpc2.Client`);
            lines.push(`\tsessionID string`);
        } else {
            lines.push(`\tclient *jsonrpc2.Client`);
        }
        lines.push(`}`);
        lines.push(``);
        for (const [key, value] of Object.entries(groupNode as Record<string, unknown>)) {
            if (!isRpcMethod(value)) continue;
            emitMethod(lines, apiName, key, value, isSession, resolveType, fieldNames, groupExperimental);
        }
    }

    // Compute field name lengths for gofmt-compatible column alignment
    const groupPascalNames = groups.map(([g]) => toPascalCase(g));
    const allFieldNames = isSession ? ["client", "sessionID", ...groupPascalNames] : ["client", ...groupPascalNames];
    const maxFieldLen = Math.max(...allFieldNames.map((n) => n.length));
    const pad = (name: string) => name.padEnd(maxFieldLen);

    // Emit wrapper struct
    lines.push(`// ${wrapperName} provides typed ${isSession ? "session" : "server"}-scoped RPC methods.`);
    lines.push(`type ${wrapperName} struct {`);
    lines.push(`\t${pad("client")} *jsonrpc2.Client`);
    if (isSession) lines.push(`\t${pad("sessionID")} string`);
    for (const [groupName] of groups) {
        const prefix = isSession ? "" : "Server";
        lines.push(`\t${pad(toPascalCase(groupName))} *${prefix}${toPascalCase(groupName)}${apiSuffix}`);
    }
    lines.push(`}`);
    lines.push(``);

    // Top-level methods (server only)
    for (const [key, value] of topLevelMethods) {
        if (!isRpcMethod(value)) continue;
        emitMethod(lines, wrapperName, key, value, isSession, resolveType, fieldNames, false);
    }

    // Compute key alignment for constructor composite literal (gofmt aligns key: value)
    const maxKeyLen = Math.max(...groupPascalNames.map((n) => n.length + 1)); // +1 for colon
    const padKey = (name: string) => (name + ":").padEnd(maxKeyLen + 1); // +1 for min trailing space

    // Constructor
    const ctorParams = isSession ? "client *jsonrpc2.Client, sessionID string" : "client *jsonrpc2.Client";
    const ctorFields = isSession ? "client: client, sessionID: sessionID," : "client: client,";
    lines.push(`func New${wrapperName}(${ctorParams}) *${wrapperName} {`);
    lines.push(`\treturn &${wrapperName}{${ctorFields}`);
    for (const [groupName] of groups) {
        const prefix = isSession ? "" : "Server";
        const apiInit = isSession
            ? `&${toPascalCase(groupName)}${apiSuffix}{client: client, sessionID: sessionID}`
            : `&${prefix}${toPascalCase(groupName)}${apiSuffix}{client: client}`;
        lines.push(`\t\t${padKey(toPascalCase(groupName))}${apiInit},`);
    }
    lines.push(`\t}`);
    lines.push(`}`);
    lines.push(``);
}

function emitMethod(lines: string[], receiver: string, name: string, method: RpcMethod, isSession: boolean, resolveType: (name: string) => string, fieldNames: Map<string, Map<string, string>>, groupExperimental = false): void {
    const methodName = toPascalCase(name);
    const resultType = resolveType(toPascalCase(method.rpcMethod) + "Result");

    const paramProps = method.params?.properties || {};
    const requiredParams = new Set(method.params?.required || []);
    const nonSessionParams = Object.keys(paramProps).filter((k) => k !== "sessionId");
    const hasParams = isSession ? nonSessionParams.length > 0 : Object.keys(paramProps).length > 0;
    const paramsType = hasParams ? resolveType(toPascalCase(method.rpcMethod) + "Params") : "";

    if (method.stability === "experimental" && !groupExperimental) {
        lines.push(`// Experimental: ${methodName} is an experimental API and may change or be removed in future versions.`);
    }
    const sig = hasParams
        ? `func (a *${receiver}) ${methodName}(ctx context.Context, params *${paramsType}) (*${resultType}, error)`
        : `func (a *${receiver}) ${methodName}(ctx context.Context) (*${resultType}, error)`;

    lines.push(sig + ` {`);

    if (isSession) {
        lines.push(`\treq := map[string]interface{}{"sessionId": a.sessionID}`);
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
        lines.push(`\traw, err := a.client.Request("${method.rpcMethod}", req)`);
    } else {
        const arg = hasParams ? "params" : "map[string]interface{}{}";
        lines.push(`\traw, err := a.client.Request("${method.rpcMethod}", ${arg})`);
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
