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
    getApiSchemaPath,
    fixNullableRequiredRefsInApiSchema,
    getNullableInner,
    getRpcSchemaTypeName,
    getSessionEventsSchemaPath,
    postProcessSchema,
    writeGeneratedFile,
    collectDefinitionCollections,
    hasSchemaPayload,
    resolveObjectSchema,
    resolveSchema,
    withSharedDefinitions,
    isRpcMethod,
    isNodeFullyExperimental,
    isNodeFullyDeprecated,
    isVoidSchema,
    type ApiSchema,
    type DefinitionCollections,
    type RpcMethod,
} from "./utils.js";

function toPascalCase(s: string): string {
    return s.charAt(0).toUpperCase() + s.slice(1);
}

function appendUniqueExportBlocks(output: string[], compiled: string, seenBlocks: Map<string, string>): void {
    for (const block of splitExportBlocks(compiled)) {
        const nameMatch = /^export\s+(?:interface|type)\s+(\w+)/m.exec(block);
        if (!nameMatch) {
            output.push(block);
            continue;
        }

        const name = nameMatch[1];
        const normalizedBlock = normalizeExportBlock(block);
        const existing = seenBlocks.get(name);
        if (existing) {
            if (existing !== normalizedBlock) {
                throw new Error(`Duplicate generated TypeScript declaration for "${name}" with different content.`);
            }
            continue;
        }

        seenBlocks.set(name, normalizedBlock);
        output.push(block);
    }
}

function splitExportBlocks(compiled: string): string[] {
    const normalizedCompiled = compiled
        .trim()
        .replace(/;(export\s+(?:interface|type)\s+)/g, ";\n$1")
        .replace(/}(export\s+(?:interface|type)\s+)/g, "}\n$1");
    const lines = normalizedCompiled.split(/\r?\n/);
    const blocks: string[] = [];
    let pending: string[] = [];

    for (let index = 0; index < lines.length;) {
        const line = lines[index];
        if (!/^export\s+(?:interface|type)\s+\w+/.test(line)) {
            pending.push(line);
            index++;
            continue;
        }

        const blockLines = [...pending, line];
        pending = [];
        let braceDepth = countBraces(line);
        index++;

        if (braceDepth === 0 && line.trim().endsWith(";")) {
            blocks.push(blockLines.join("\n").trim());
            continue;
        }

        while (index < lines.length) {
            const nextLine = lines[index];
            blockLines.push(nextLine);
            braceDepth += countBraces(nextLine);
            index++;

            const trimmed = nextLine.trim();
            if (braceDepth === 0 && (trimmed === "}" || trimmed.endsWith(";"))) {
                break;
            }
        }

        blocks.push(blockLines.join("\n").trim());
    }

    return blocks;
}

function countBraces(line: string): number {
    let depth = 0;
    for (const char of line) {
        if (char === "{") depth++;
        if (char === "}") depth--;
    }
    return depth;
}

function normalizeExportBlock(block: string): string {
    return block
        .replace(/\/\*\*[\s\S]*?\*\//g, "")
        .split(/\r?\n/)
        .map((line) => line.trim())
        .filter((line) => line.length > 0)
        .join("\n");
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

function normalizeSchemaForTypeScript(schema: JSONSchema7): JSONSchema7 {
    const root = structuredClone(schema) as JSONSchema7 & {
        definitions?: Record<string, unknown>;
        $defs?: Record<string, unknown>;
    };
    const definitions = { ...(root.definitions ?? {}) };
    const draftDefinitionAliases = new Map<string, string>();

    for (const [key, value] of Object.entries(root.$defs ?? {})) {
        if (key in definitions) {
            // The definitions entry is authoritative (it went through the full pipeline).
            // Drop the $defs duplicate and rewrite any $ref pointing at it to use definitions.
            draftDefinitionAliases.set(key, key);
        } else {
            draftDefinitionAliases.set(key, key);
            definitions[key] = value;
        }
    }

    root.definitions = definitions;
    delete root.$defs;

    const rewrite = (value: unknown): unknown => {
        if (Array.isArray(value)) {
            return value.map(rewrite);
        }
        if (!value || typeof value !== "object") {
            return value;
        }

        const rewritten = Object.fromEntries(
            Object.entries(value as Record<string, unknown>).map(([key, child]) => [key, rewrite(child)])
        ) as Record<string, unknown>;

        if (typeof rewritten.$ref === "string") {
            if (rewritten.$ref.startsWith("#/$defs/")) {
                const definitionName = rewritten.$ref.slice("#/$defs/".length);
                rewritten.$ref = `#/definitions/${draftDefinitionAliases.get(definitionName) ?? definitionName}`;
            }
            // json-schema-to-typescript treats sibling keywords alongside $ref as a
            // new inline type instead of reusing the referenced definition.  Strip
            // siblings so that $ref-only objects compile to a single shared type.
            for (const key of Object.keys(rewritten)) {
                if (key !== "$ref") {
                    delete rewritten[key];
                }
            }
        }

        return rewritten;
    };

    return rewrite(root) as JSONSchema7;
}

// ── Session Events ──────────────────────────────────────────────────────────

async function generateSessionEvents(schemaPath?: string): Promise<void> {
    console.log("TypeScript: generating session-events...");

    const resolvedPath = schemaPath ?? (await getSessionEventsSchemaPath());
    const schema = JSON.parse(await fs.readFile(resolvedPath, "utf-8")) as JSONSchema7;
    const processed = postProcessSchema(schema);
    const definitionCollections = collectDefinitionCollections(processed as Record<string, unknown>);
    const sessionEvent =
        resolveSchema({ $ref: "#/definitions/SessionEvent" }, definitionCollections) ??
        resolveSchema({ $ref: "#/$defs/SessionEvent" }, definitionCollections) ??
        processed;
    const schemaForCompile = withSharedDefinitions(sessionEvent, definitionCollections);

    const ts = await compile(normalizeSchemaForTypeScript(schemaForCompile), "SessionEvent", {
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

let rpcDefinitions: DefinitionCollections = { definitions: {}, $defs: {} };

function withRootTitle(schema: JSONSchema7, title: string): JSONSchema7 {
    return { ...schema, title };
}

function rpcRequestFallbackName(method: RpcMethod): string {
    return method.rpcMethod.split(".").map(toPascalCase).join("") + "Request";
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

function resultTypeName(method: RpcMethod): string {
    return getRpcSchemaTypeName(
        getMethodResultSchema(method),
        method.rpcMethod.split(".").map(toPascalCase).join("") + "Result"
    );
}

function tsNullableResultTypeName(method: RpcMethod): string | undefined {
    const resultSchema = getMethodResultSchema(method);
    if (!resultSchema) return undefined;
    const inner = getNullableInner(resultSchema);
    if (!inner) return undefined;
    // Resolve $ref to a type name
    if (inner.$ref) {
        const refName = inner.$ref.split("/").pop();
        if (refName) return `${toPascalCase(refName)} | undefined`;
    }
    const innerName = getRpcSchemaTypeName(inner, method.rpcMethod.split(".").map(toPascalCase).join("") + "Result");
    return `${innerName} | undefined`;
}

function tsResultType(method: RpcMethod): string {
    if (isVoidSchema(getMethodResultSchema(method))) return "void";
    return tsNullableResultTypeName(method) ?? resultTypeName(method);
}

function paramsTypeName(method: RpcMethod): string {
    const fallback = rpcRequestFallbackName(method);
    if (method.rpcMethod.startsWith("session.") && method.params?.$ref) {
        return fallback;
    }
    return getRpcSchemaTypeName(getMethodParamsSchema(method), fallback);
}

async function generateRpc(schemaPath?: string): Promise<void> {
    console.log("TypeScript: generating RPC types...");

    const resolvedPath = schemaPath ?? (await getApiSchemaPath());
    const schema = fixNullableRequiredRefsInApiSchema(JSON.parse(await fs.readFile(resolvedPath, "utf-8")) as ApiSchema);

    const lines: string[] = [];
    lines.push(`/**
 * AUTO-GENERATED FILE - DO NOT EDIT
 * Generated from: api.schema.json
 */

import type { MessageConnection } from "vscode-jsonrpc/node.js";
`);

    const allMethods = [...collectRpcMethods(schema.server || {}), ...collectRpcMethods(schema.session || {})];
    const clientSessionMethods = collectRpcMethods(schema.clientSession || {});
    const seenBlocks = new Map<string, string>();

    // Build a single combined schema with shared definitions and all method types.
    // This ensures $ref-referenced types are generated exactly once.
    rpcDefinitions = collectDefinitionCollections(schema as Record<string, unknown>);
    const combinedSchema = withSharedDefinitions(
        {
            $schema: "http://json-schema.org/draft-07/schema#",
            type: "object",
        },
        rpcDefinitions
    );

    // Track which type names come from experimental methods for JSDoc annotations.
    const experimentalTypes = new Set<string>();
    // Track which type names come from deprecated methods for JSDoc annotations.
    const deprecatedTypes = new Set<string>();

    for (const method of [...allMethods, ...clientSessionMethods]) {
        const resultSchema = getMethodResultSchema(method);
        if (!isVoidSchema(resultSchema) && !getNullableInner(resultSchema)) {
            combinedSchema.definitions![resultTypeName(method)] = withRootTitle(
                schemaSourceForNamedDefinition(method.result, resultSchema),
                resultTypeName(method)
            );
            if (method.stability === "experimental") {
                experimentalTypes.add(resultTypeName(method));
            }
            if (method.deprecated && !method.result?.$ref) {
                deprecatedTypes.add(resultTypeName(method));
            }
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
                    combinedSchema.definitions![paramsTypeName(method)] = withRootTitle(
                        filtered,
                        paramsTypeName(method)
                    );
                    if (method.stability === "experimental") {
                        experimentalTypes.add(paramsTypeName(method));
                    }
                    if (method.deprecated) {
                        deprecatedTypes.add(paramsTypeName(method));
                    }
                }
            } else {
                combinedSchema.definitions![paramsTypeName(method)] = withRootTitle(
                    schemaSourceForNamedDefinition(method.params, resolvedParams),
                    paramsTypeName(method)
                );
                if (method.stability === "experimental") {
                    experimentalTypes.add(paramsTypeName(method));
                }
                if (method.deprecated && !method.params?.$ref) {
                    deprecatedTypes.add(paramsTypeName(method));
                }
            }
        }
    }

    const schemaForCompile = combinedSchema;

    const compiled = await compile(normalizeSchemaForTypeScript(schemaForCompile), "_RpcSchemaRoot", {
        bannerComment: "",
        additionalProperties: false,
        unreachableDefinitions: true,
    });

    // Strip the placeholder root type and keep only the definition-generated types
    const strippedTs = compiled
        .replace(
            /\/\*\*\n \* This (?:interface|type) was referenced by `_RpcSchemaRoot`'s JSON-Schema\n \* via the `definition` "[^"]+"\.\n \*\/\n/g,
            "\n"
        )
        .replace(/export interface _RpcSchemaRoot\s*\{[^}]*\}\s*/g, "")
        .replace(/export type _RpcSchemaRoot = [^;]+;\s*/g, "")
        .trim();

    if (strippedTs) {
        // Add @experimental JSDoc annotations for types from experimental methods
        let annotatedTs = strippedTs;
        for (const expType of experimentalTypes) {
            annotatedTs = annotatedTs.replace(
                new RegExp(`(^|\\n)(export (?:interface|type) ${expType}\\b)`, "m"),
                `$1/** @experimental */\n$2`
            );
        }
        // Add @deprecated JSDoc annotations for types from deprecated methods
        for (const depType of deprecatedTypes) {
            annotatedTs = annotatedTs.replace(
                new RegExp(`(^|\\n)(export (?:interface|type) ${depType}\\b)`, "m"),
                `$1/** @deprecated */\n$2`
            );
        }
        lines.push(annotatedTs);
        lines.push("");
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

    // Generate client session API handler interfaces and registration function
    if (schema.clientSession) {
        lines.push(...emitClientSessionApiRegistration(schema.clientSession));
    }

    const outPath = await writeGeneratedFile("nodejs/src/generated/rpc.ts", lines.join("\n"));
    console.log(`  ✓ ${outPath}`);
}

function emitGroup(node: Record<string, unknown>, indent: string, isSession: boolean, parentExperimental = false, parentDeprecated = false): string[] {
    const lines: string[] = [];
    for (const [key, value] of Object.entries(node)) {
        if (isRpcMethod(value)) {
            const { rpcMethod, params } = value;
            const resultType = tsResultType(value);
            const paramsType = paramsTypeName(value);
            const effectiveParams = getMethodParamsSchema(value);

            const paramEntries = effectiveParams?.properties
                ? Object.entries(effectiveParams.properties).filter(([k]) => k !== "sessionId")
                : [];
            const hasParams = hasSchemaPayload(effectiveParams);
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

            if ((value as RpcMethod).deprecated && !parentDeprecated) {
                lines.push(`${indent}/** @deprecated */`);
            }
            if ((value as RpcMethod).stability === "experimental" && !parentExperimental) {
                lines.push(`${indent}/** @experimental */`);
            }
            lines.push(`${indent}${key}: async (${sigParams.join(", ")}): Promise<${resultType}> =>`);
            lines.push(`${indent}    connection.sendRequest("${rpcMethod}", ${bodyArg}),`);
        } else if (typeof value === "object" && value !== null) {
            const groupExperimental = isNodeFullyExperimental(value as Record<string, unknown>);
            const groupDeprecated = isNodeFullyDeprecated(value as Record<string, unknown>);
            if (groupDeprecated) {
                lines.push(`${indent}/** @deprecated */`);
            }
            if (groupExperimental) {
                lines.push(`${indent}/** @experimental */`);
            }
            lines.push(`${indent}${key}: {`);
            lines.push(...emitGroup(value as Record<string, unknown>, indent + "    ", isSession, groupExperimental, groupDeprecated));
            lines.push(`${indent}},`);
        }
    }
    return lines;
}

// ── Client Session API Handler Generation ───────────────────────────────────

/**
 * Collect client API methods grouped by their top-level namespace.
 * Returns a map like: { sessionFs: [{ rpcMethod, params, result }, ...] }
 */
function collectClientGroups(node: Record<string, unknown>): Map<string, RpcMethod[]> {
    const groups = new Map<string, RpcMethod[]>();
    for (const [groupName, groupNode] of Object.entries(node)) {
        if (typeof groupNode === "object" && groupNode !== null) {
            groups.set(groupName, collectRpcMethods(groupNode as Record<string, unknown>));
        }
    }
    return groups;
}

/**
 * Derive the handler method name from the full RPC method name.
 * e.g., "sessionFs.readFile" → "readFile"
 */
function handlerMethodName(rpcMethod: string): string {
    const parts = rpcMethod.split(".");
    return parts[parts.length - 1];
}

/**
 * Generate handler interfaces and a registration function for client session API groups.
 *
 * Client session API methods have `sessionId` on the wire (injected by the
 * runtime's proxy layer). The generated registration function accepts a
 * `getHandler` callback that resolves a sessionId to a handler object.
 * Param types include sessionId — handler code can simply ignore it.
 */
function emitClientSessionApiRegistration(clientSchema: Record<string, unknown>): string[] {
    const lines: string[] = [];
    const groups = collectClientGroups(clientSchema);

    // Emit a handler interface per group
    for (const [groupName, methods] of groups) {
        const interfaceName = toPascalCase(groupName) + "Handler";
        const groupDeprecated = isNodeFullyDeprecated(clientSchema[groupName] as Record<string, unknown>);
        if (groupDeprecated) {
            lines.push(`/** @deprecated Handler for \`${groupName}\` client session API methods. */`);
        } else {
            lines.push(`/** Handler for \`${groupName}\` client session API methods. */`);
        }
        lines.push(`export interface ${interfaceName} {`);
        for (const method of methods) {
            const name = handlerMethodName(method.rpcMethod);
            const hasParams = hasSchemaPayload(getMethodParamsSchema(method));
            const pType = hasParams ? paramsTypeName(method) : "";
            const rType = tsResultType(method);

            if (method.deprecated && !groupDeprecated) {
                lines.push(`    /** @deprecated */`);
            }
            if (hasParams) {
                lines.push(`    ${name}(params: ${pType}): Promise<${rType}>;`);
            } else {
                lines.push(`    ${name}(): Promise<${rType}>;`);
            }
        }
        lines.push(`}`);
        lines.push("");
    }

    // Emit combined ClientSessionApiHandlers type
    lines.push(`/** All client session API handler groups. */`);
    lines.push(`export interface ClientSessionApiHandlers {`);
    for (const [groupName] of groups) {
        const interfaceName = toPascalCase(groupName) + "Handler";
        lines.push(`    ${groupName}?: ${interfaceName};`);
    }
    lines.push(`}`);
    lines.push("");

    // Emit registration function
    lines.push(`/**`);
    lines.push(` * Register client session API handlers on a JSON-RPC connection.`);
    lines.push(` * The server calls these methods to delegate work to the client.`);
    lines.push(` * Each incoming call includes a \`sessionId\` in the params; the registration`);
    lines.push(` * function uses \`getHandlers\` to resolve the session's handlers.`);
    lines.push(` */`);
    lines.push(`export function registerClientSessionApiHandlers(`);
    lines.push(`    connection: MessageConnection,`);
    lines.push(`    getHandlers: (sessionId: string) => ClientSessionApiHandlers,`);
    lines.push(`): void {`);

    for (const [groupName, methods] of groups) {
        for (const method of methods) {
            const name = handlerMethodName(method.rpcMethod);
            const pType = paramsTypeName(method);
            const hasParams = hasSchemaPayload(getMethodParamsSchema(method));

            if (hasParams) {
                lines.push(`    connection.onRequest("${method.rpcMethod}", async (params: ${pType}) => {`);
                lines.push(`        const handler = getHandlers(params.sessionId).${groupName};`);
                lines.push(`        if (!handler) throw new Error(\`No ${groupName} handler registered for session: \${params.sessionId}\`);`);
                lines.push(`        return handler.${name}(params);`);
                lines.push(`    });`);
            } else {
                lines.push(`    connection.onRequest("${method.rpcMethod}", async () => {`);
                lines.push(`        throw new Error("No params provided for ${method.rpcMethod}");`);
                lines.push(`    });`);
            }
        }
    }

    lines.push(`}`);
    lines.push("");

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
