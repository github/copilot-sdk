/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Generate C# RPC client wrappers from api.schema.json.
 *
 * Produces:
 * - Data classes for params and results (public / internal as appropriate)
 * - `ServerRpc` static helper class for stateless server methods
 * - `SessionRpc` / per-group public API classes that bind `sessionId` once
 *
 * Usage:
 *   npm run generate:rpc-types
 */

import fs from "fs/promises";
import path from "path";
import { fileURLToPath } from "url";
import type { JSONSchema7 } from "json-schema";
import {
    toPascalCase,
    schemaTypeToCSharp,
    typeToClassName,
    CSHARP_COPYRIGHT,
} from "./codegen-csharp-utils.js";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// ── Schema helpers ──────────────────────────────────────────────────────────

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

// ── C# class generation ─────────────────────────────────────────────────────

const knownTypes = new Map<string, string>();
const emittedClasses = new Set<string>();

function singularPascal(s: string): string {
    const p = toPascalCase(s);
    return p.endsWith("s") ? p.slice(0, -1) : p;
}

/**
 * Resolve a property's C# type, recursing into nested objects / arrays
 * and emitting additional classes as needed.
 */
function resolveType(
    schema: JSONSchema7,
    isRequired: boolean,
    parentClassName: string,
    propName: string,
    classes: string[]
): string {
    // Nested object → emit a new class
    if (schema.type === "object" && schema.properties) {
        const className = `${parentClassName}${propName}`;
        classes.push(emitClass(className, schema, "public", classes));
        return isRequired ? className : `${className}?`;
    }

    // Array of objects → emit a new class for the item type
    if (schema.type === "array" && schema.items) {
        const items = schema.items as JSONSchema7;
        if (items.type === "object" && items.properties) {
            const itemClass = `${parentClassName}${propName}Item` === `${parentClassName}Item`
                ? parentClassName
                : singularPascal(propName);
            if (!emittedClasses.has(itemClass)) {
                classes.push(emitClass(itemClass, items, "public", classes));
            }
            return isRequired ? `List<${itemClass}>` : `List<${itemClass}>?`;
        }
        const itemType = schemaTypeToCSharp(items, true, knownTypes);
        return isRequired ? `List<${itemType}>` : `List<${itemType}>?`;
    }

    // Object with additionalProperties (map type)
    if (schema.type === "object" && schema.additionalProperties) {
        const valSchema = schema.additionalProperties;
        if (typeof valSchema === "object") {
            const vs = valSchema as JSONSchema7;
            if (vs.type === "object" && vs.properties) {
                const valClass = `${parentClassName}${propName}Value`;
                classes.push(emitClass(valClass, vs, "public", classes));
                return isRequired
                    ? `Dictionary<string, ${valClass}>`
                    : `Dictionary<string, ${valClass}>?`;
            }
            const valueType = schemaTypeToCSharp(vs, true, knownTypes);
            return isRequired
                ? `Dictionary<string, ${valueType}>`
                : `Dictionary<string, ${valueType}>?`;
        }
        return isRequired ? "Dictionary<string, object>" : "Dictionary<string, object>?";
    }

    return schemaTypeToCSharp(schema, isRequired, knownTypes);
}

/**
 * Emit a C# class with properties derived from a JSON Schema object.
 */
function emitClass(
    className: string,
    schema: JSONSchema7,
    visibility: "public" | "internal",
    extraClasses: string[]
): string {
    if (emittedClasses.has(className)) return "";
    emittedClasses.add(className);

    const requiredSet = new Set(schema.required || []);
    const lines: string[] = [];

    if (schema.description) {
        lines.push(`/// <summary>${schema.description}</summary>`);
    }
    lines.push(`${visibility} class ${className}`);
    lines.push(`{`);

    const props = Object.entries(schema.properties || {});
    for (let i = 0; i < props.length; i++) {
        const [propName, propSchema] = props[i];
        if (typeof propSchema !== "object") continue;
        const prop = propSchema as JSONSchema7;
        const isReq = requiredSet.has(propName);
        const csharpName = toPascalCase(propName);
        const csharpType = resolveType(prop, isReq, className, csharpName, extraClasses);

        if (prop.description && visibility === "public") {
            lines.push(`    /// <summary>${prop.description}</summary>`);
        }
        lines.push(`    [JsonPropertyName("${propName}")]`);

        // Default values for non-nullable types
        let defaultVal = "";
        if (isReq && !csharpType.endsWith("?")) {
            if (csharpType === "string") defaultVal = " = string.Empty;";
            else if (csharpType.startsWith("List<")) defaultVal = " = new();";
            else if (csharpType.startsWith("Dictionary<")) defaultVal = " = new();";
            else if (emittedClasses.has(csharpType)) defaultVal = " = new();";
        }
        lines.push(`    public ${csharpType} ${csharpName} { get; set; }${defaultVal}`);

        if (i < props.length - 1) lines.push("");
    }

    lines.push(`}`);
    return lines.join("\n");
}

// ── Server RPC emission ─────────────────────────────────────────────────────

function emitServerRpcClass(
    node: Record<string, unknown>,
    classes: string[]
): string {
    const lines: string[] = [];
    lines.push(`internal static class ServerRpc`);
    lines.push(`{`);
    emitServerGroup(node, lines, classes, "    ");
    lines.push(`}`);
    return lines.join("\n");
}

function emitServerGroup(
    node: Record<string, unknown>,
    lines: string[],
    classes: string[],
    indent: string
): void {
    const entries = Object.entries(node);
    for (let i = 0; i < entries.length; i++) {
        const [key, value] = entries[i];
        if (isRpcMethod(value)) {
            emitServerMethod(key, value, lines, classes, indent);
            if (i < entries.length - 1) lines.push("");
        } else if (typeof value === "object" && value !== null) {
            const className = toPascalCase(key);
            lines.push(`${indent}internal static class ${className}`);
            lines.push(`${indent}{`);
            emitServerGroup(
                value as Record<string, unknown>,
                lines,
                classes,
                indent + "    "
            );
            lines.push(`${indent}}`);
            if (i < entries.length - 1) lines.push("");
        }
    }
}

function emitServerMethod(
    name: string,
    method: RpcMethod,
    lines: string[],
    classes: string[],
    indent: string
): void {
    const methodName = toPascalCase(name);

    // Build result class name
    const resultClassName = `${typeToClassName(method.rpcMethod)}Result`;
    const resultClass = emitClass(resultClassName, method.result, "public", classes);
    if (resultClass) classes.push(resultClass);

    // Build params
    const paramEntries = method.params?.properties
        ? Object.entries(method.params.properties)
        : [];
    const requiredSet = new Set(method.params?.required || []);

    // If there are params, emit a request class
    let requestClassName: string | null = null;
    if (paramEntries.length > 0) {
        requestClassName = `${toPascalCase(name)}Request`;
        const reqClass = emitClass(requestClassName, method.params!, "internal", classes);
        if (reqClass) classes.push(reqClass);
    }

    // Emit doc comment
    lines.push(
        `${indent}/// <summary>Calls "${method.rpcMethod}" via JSON-RPC.</summary>`
    );
    for (const [pName, pSchema] of paramEntries) {
        if (typeof pSchema !== "object") continue;
        const desc = (pSchema as JSONSchema7).description;
        if (desc) {
            lines.push(`${indent}/// <param name="${pName}">${desc}</param>`);
        }
    }

    // Build signature
    const sigParams = ["JsonRpc rpc"];
    const bodyAssignments: string[] = [];

    for (const [pName, pSchema] of paramEntries) {
        if (typeof pSchema !== "object") continue;
        const isReq = requiredSet.has(pName);
        const csType = schemaTypeToCSharp(pSchema as JSONSchema7, isReq, knownTypes);
        const paramDefault = isReq ? "" : " = null";
        sigParams.push(`${csType} ${pName}${paramDefault}`);
        bodyAssignments.push(
            `${toPascalCase(pName)} = ${pName}`
        );
    }
    sigParams.push("CancellationToken cancellationToken = default");

    lines.push(
        `${indent}internal static async Task<${resultClassName}> ${methodName}Async(${sigParams.join(", ")})`
    );
    lines.push(`${indent}{`);

    if (requestClassName && bodyAssignments.length > 0) {
        lines.push(
            `${indent}    var request = new ${requestClassName} { ${bodyAssignments.join(", ")} };`
        );
        lines.push(
            `${indent}    return await CopilotClient.InvokeRpcAsync<${resultClassName}>(rpc, "${method.rpcMethod}", [request], cancellationToken);`
        );
    } else {
        lines.push(
            `${indent}    return await CopilotClient.InvokeRpcAsync<${resultClassName}>(rpc, "${method.rpcMethod}", [], cancellationToken);`
        );
    }

    lines.push(`${indent}}`);
}

// ── Session RPC emission ────────────────────────────────────────────────────

/**
 * Emit the top-level `SessionRpc` class plus nested API classes.
 */
function emitSessionRpcClasses(
    node: Record<string, unknown>,
    classes: string[]
): string[] {
    const result: string[] = [];

    // Collect top-level group names (e.g. "model")
    const groups = Object.entries(node).filter(
        ([, v]) => typeof v === "object" && v !== null && !isRpcMethod(v)
    );

    // SessionRpc class
    const srLines: string[] = [];
    srLines.push(
        `/// <summary>Typed session-scoped RPC methods. Automatically binds the session ID.</summary>`
    );
    srLines.push(`public class SessionRpc`);
    srLines.push(`{`);
    srLines.push(`    private readonly JsonRpc _rpc;`);
    srLines.push(`    private readonly string _sessionId;`);
    srLines.push("");
    srLines.push(`    internal SessionRpc(JsonRpc rpc, string sessionId)`);
    srLines.push(`    {`);
    srLines.push(`        _rpc = rpc;`);
    srLines.push(`        _sessionId = sessionId;`);
    for (const [groupName] of groups) {
        const propName = toPascalCase(groupName);
        srLines.push(
            `        ${propName} = new ${propName}Api(rpc, sessionId);`
        );
    }
    srLines.push(`    }`);

    for (const [groupName] of groups) {
        const propName = toPascalCase(groupName);
        srLines.push("");
        srLines.push(`    /// <summary>${propName} APIs.</summary>`);
        srLines.push(`    public ${propName}Api ${propName} { get; }`);
    }

    srLines.push(`}`);
    result.push(srLines.join("\n"));

    // Per-group API classes
    for (const [groupName, groupNode] of groups) {
        const apiClassName = `${toPascalCase(groupName)}Api`;
        result.push(
            emitSessionApiClass(
                apiClassName,
                groupNode as Record<string, unknown>,
                classes
            )
        );
    }

    return result;
}

function emitSessionApiClass(
    className: string,
    node: Record<string, unknown>,
    classes: string[]
): string {
    const lines: string[] = [];
    lines.push(`/// <summary>Session-scoped ${className.replace("Api", "")} APIs.</summary>`);
    lines.push(`public class ${className}`);
    lines.push(`{`);
    lines.push(`    private readonly JsonRpc _rpc;`);
    lines.push(`    private readonly string _sessionId;`);
    lines.push("");
    lines.push(`    internal ${className}(JsonRpc rpc, string sessionId)`);
    lines.push(`    {`);
    lines.push(`        _rpc = rpc;`);
    lines.push(`        _sessionId = sessionId;`);
    lines.push(`    }`);

    for (const [key, value] of Object.entries(node)) {
        if (!isRpcMethod(value)) continue;
        emitSessionMethod(key, value, lines, classes);
    }

    lines.push(`}`);
    return lines.join("\n");
}

function emitSessionMethod(
    name: string,
    method: RpcMethod,
    lines: string[],
    classes: string[]
): void {
    const methodName = toPascalCase(name);

    // Result class
    const resultClassName = `${typeToClassName(method.rpcMethod.replace(/^session\./, ""))}Result`;
    const resultClass = emitClass(resultClassName, method.result, "public", classes);
    if (resultClass) classes.push(resultClass);

    // Params (exclude sessionId)
    const allParams = method.params?.properties
        ? Object.entries(method.params.properties)
        : [];
    const paramEntries = allParams.filter(([k]) => k !== "sessionId");
    const requiredSet = new Set(method.params?.required || []);

    // Request class (always needed for session methods — includes sessionId)
    const requestClassName = `${toPascalCase(name)}Request`;
    if (method.params) {
        const reqClass = emitClass(requestClassName, method.params, "internal", classes);
        if (reqClass) classes.push(reqClass);
    }

    lines.push("");
    lines.push(
        `    /// <summary>Calls "${method.rpcMethod}" via JSON-RPC.</summary>`
    );

    // Signature (no sessionId — it comes from _sessionId)
    const sigParams: string[] = [];
    const bodyAssignments = [`SessionId = _sessionId`];

    for (const [pName, pSchema] of paramEntries) {
        if (typeof pSchema !== "object") continue;
        const isReq = requiredSet.has(pName);
        const csType = schemaTypeToCSharp(pSchema as JSONSchema7, isReq, knownTypes);
        sigParams.push(`${csType} ${pName}`);
        bodyAssignments.push(`${toPascalCase(pName)} = ${pName}`);
    }
    sigParams.push("CancellationToken cancellationToken = default");

    lines.push(`    [Experimental("CopilotSdk001")]`);
    lines.push(
        `    public async Task<${resultClassName}> ${methodName}Async(${sigParams.join(", ")})`
    );
    lines.push(`    {`);
    lines.push(
        `        var request = new ${requestClassName} { ${bodyAssignments.join(", ")} };`
    );
    lines.push(
        `        return await CopilotClient.InvokeRpcAsync<${resultClassName}>(_rpc, "${method.rpcMethod}", [request], cancellationToken);`
    );
    lines.push(`    }`);
}

// ── Main ────────────────────────────────────────────────────────────────────

async function getApiSchemaPath(): Promise<string> {
    const schemaPath = path.join(
        __dirname,
        "../../../copilot-agent-runtime/generated/api.schema.json"
    );
    await fs.access(schemaPath);
    console.log(`✅ Found API schema at: ${schemaPath}`);
    return schemaPath;
}

export async function generateCSharpRpc(schemaPath?: string): Promise<void> {
    console.log("🔄 Generating C# RPC types…");

    // Reset state
    emittedClasses.clear();
    knownTypes.clear();

    const resolvedPath = schemaPath ?? (await getApiSchemaPath());
    const schema = JSON.parse(
        await fs.readFile(resolvedPath, "utf-8")
    ) as SchemaRoot;
    const generatedAt = new Date().toISOString();

    const classes: string[] = [];

    // Build server RPC
    let serverRpc = "";
    if (schema.server) {
        serverRpc = emitServerRpcClass(schema.server, classes);
    }

    // Build session RPC
    let sessionRpcParts: string[] = [];
    if (schema.session) {
        sessionRpcParts = emitSessionRpcClasses(schema.session, classes);
    }

    // Assemble file
    const lines: string[] = [];
    lines.push(`${CSHARP_COPYRIGHT}

// AUTO-GENERATED FILE - DO NOT EDIT
//
// Generated from: api.schema.json
// Generated at: ${generatedAt}

using System.Diagnostics.CodeAnalysis;
using System.Text.Json.Serialization;
using StreamJsonRpc;

namespace GitHub.Copilot.SDK.Rpc;
`);

    // Data classes first
    for (const cls of classes) {
        if (cls) {
            lines.push(cls);
            lines.push("");
        }
    }

    // ServerRpc
    if (serverRpc) {
        lines.push(serverRpc);
        lines.push("");
    }

    // SessionRpc
    for (const part of sessionRpcParts) {
        lines.push(part);
        lines.push("");
    }

    const outputPath = path.join(__dirname, "../../dotnet/src/Generated/Rpc.cs");
    await fs.mkdir(path.dirname(outputPath), { recursive: true });
    await fs.writeFile(outputPath, lines.join("\n") + "\n", "utf-8");
    console.log(`✅ Generated C# RPC types: ${outputPath}`);
}

// Run if invoked directly
const isMain =
    process.argv[1] &&
    (process.argv[1].endsWith("generate-csharp-rpc-types.ts") ||
        process.argv[1].endsWith("generate-csharp-rpc-types.js"));
if (isMain) {
    const schemaArg = process.argv[2] || undefined;
    generateCSharpRpc(schemaArg).catch((err) => {
        console.error("❌ C# RPC generation failed:", err);
        process.exit(1);
    });
}
