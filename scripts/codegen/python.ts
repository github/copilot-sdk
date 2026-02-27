/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Python code generator for session-events and RPC types.
 */

import fs from "fs/promises";
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
function modernizePython(code: string): string {
    // Replace Optional[X] with X | None (handles nested brackets)
    code = code.replace(/Optional\[([^\[\]]*(?:\[[^\[\]]*\])*[^\[\]]*)\]/g, "$1 | None");

    // Replace Union[X, Y] with X | Y
    code = code.replace(/Union\[([^\[\]]*(?:\[[^\[\]]*\])*[^\[\]]*)\]/g, (_match, inner: string) => {
        return inner.split(",").map((s: string) => s.trim()).join(" | ");
    });

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

// ── Session Events ──────────────────────────────────────────────────────────

async function generateSessionEvents(schemaPath?: string): Promise<void> {
    console.log("Python: generating session-events...");

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
        lang: "python",
        rendererOptions: { "python-version": "3.7" },
    });

    let code = result.lines.join("\n");

    // Fix dataclass field ordering (Any fields need defaults)
    code = code.replace(/: Any$/gm, ": Any = None");
    // Fix bare except: to use Exception (required by ruff/pylint)
    code = code.replace(/except:/g, "except Exception:");
    // Modernize to Python 3.11+ syntax
    code = modernizePython(code);

    // Add UNKNOWN enum value for forward compatibility
    code = code.replace(
        /^(class SessionEventType\(Enum\):.*?)(^\s*\n@dataclass)/ms,
        `$1    # UNKNOWN is used for forward compatibility
    UNKNOWN = "unknown"

    @classmethod
    def _missing_(cls, value: object) -> "SessionEventType":
        """Handle unknown event types gracefully for forward compatibility."""
        return cls.UNKNOWN

$2`
    );

    const banner = `"""
AUTO-GENERATED FILE - DO NOT EDIT
Generated from: session-events.schema.json
"""

`;

    const outPath = await writeGeneratedFile("python/copilot/generated/session_events.py", banner + code);
    console.log(`  ✓ ${outPath}`);
}

// ── RPC Types ───────────────────────────────────────────────────────────────

async function generateRpc(schemaPath?: string): Promise<void> {
    console.log("Python: generating RPC types...");

    const resolvedPath = schemaPath ?? (await getApiSchemaPath());
    const schema = JSON.parse(await fs.readFile(resolvedPath, "utf-8")) as ApiSchema;

    const allMethods = [...collectRpcMethods(schema.server || {}), ...collectRpcMethods(schema.session || {})];

    // Build a combined schema for quicktype
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

    const lines: string[] = [];
    lines.push(`"""
AUTO-GENERATED FILE - DO NOT EDIT
Generated from: api.schema.json
"""

from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from ..jsonrpc import JsonRpcClient

`);
    lines.push(typesCode);
    lines.push(``);

    // Emit RPC wrapper classes
    if (schema.server) {
        emitRpcWrapper(lines, schema.server, false);
    }
    if (schema.session) {
        emitRpcWrapper(lines, schema.session, true);
    }

    const outPath = await writeGeneratedFile("python/copilot/generated/rpc.py", lines.join("\n"));
    console.log(`  ✓ ${outPath}`);
}

function emitRpcWrapper(lines: string[], node: Record<string, unknown>, isSession: boolean): void {
    const groups = Object.entries(node).filter(([, v]) => typeof v === "object" && v !== null && !isRpcMethod(v));
    const topLevelMethods = Object.entries(node).filter(([, v]) => isRpcMethod(v));

    const wrapperName = isSession ? "SessionRpc" : "ServerRpc";

    // Emit API classes for groups
    for (const [groupName, groupNode] of groups) {
        const apiName = toPascalCase(groupName) + "Api";
        if (isSession) {
            lines.push(`class ${apiName}:`);
            lines.push(`    def __init__(self, client: "JsonRpcClient", session_id: str):`);
            lines.push(`        self._client = client`);
            lines.push(`        self._session_id = session_id`);
        } else {
            lines.push(`class ${apiName}:`);
            lines.push(`    def __init__(self, client: "JsonRpcClient"):`);
            lines.push(`        self._client = client`);
        }
        lines.push(``);
        for (const [key, value] of Object.entries(groupNode as Record<string, unknown>)) {
            if (!isRpcMethod(value)) continue;
            emitMethod(lines, key, value, isSession);
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
            lines.push(`        self.${toSnakeCase(groupName)} = ${toPascalCase(groupName)}Api(client)`);
        }
    }
    lines.push(``);

    // Top-level methods
    for (const [key, value] of topLevelMethods) {
        if (!isRpcMethod(value)) continue;
        emitMethod(lines, key, value, isSession);
    }
    lines.push(``);
}

function emitMethod(lines: string[], name: string, method: RpcMethod, isSession: boolean): void {
    const methodName = toSnakeCase(name);
    const resultType = toPascalCase(method.rpcMethod) + "Result";

    const paramProps = method.params?.properties || {};
    const nonSessionParams = Object.keys(paramProps).filter((k) => k !== "sessionId");
    const hasParams = isSession ? nonSessionParams.length > 0 : Object.keys(paramProps).length > 0;
    const paramsType = toPascalCase(method.rpcMethod) + "Params";

    // Build signature with typed params
    const sig = hasParams
        ? `    async def ${methodName}(self, params: ${paramsType}) -> ${resultType}:`
        : `    async def ${methodName}(self) -> ${resultType}:`;

    lines.push(sig);

    // Build request body with proper serialization/deserialization
    if (isSession) {
        if (hasParams) {
            lines.push(`        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}`);
            lines.push(`        params_dict["sessionId"] = self._session_id`);
            lines.push(`        return ${resultType}.from_dict(await self._client.request("${method.rpcMethod}", params_dict))`);
        } else {
            lines.push(`        return ${resultType}.from_dict(await self._client.request("${method.rpcMethod}", {"sessionId": self._session_id}))`);
        }
    } else {
        if (hasParams) {
            lines.push(`        params_dict = {k: v for k, v in params.to_dict().items() if v is not None}`);
            lines.push(`        return ${resultType}.from_dict(await self._client.request("${method.rpcMethod}", params_dict))`);
        } else {
            lines.push(`        return ${resultType}.from_dict(await self._client.request("${method.rpcMethod}", {}))`);
        }
    }
    lines.push(``);
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

const sessionArg = process.argv[2] || undefined;
const apiArg = process.argv[3] || undefined;
generate(sessionArg, apiArg).catch((err) => {
    console.error("Python generation failed:", err);
    process.exit(1);
});
