/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { mkdtempSync, readFileSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { rimraf } from "rimraf";
import ts from "typescript";
import { describe, expect, it, onTestFinished } from "vitest";
import { approveAll, CopilotClient, RuntimeConnection } from "../../src/index.js";

const FAKE_RPC_CLI = `const fs = require("fs");

const captureIndex = process.argv.indexOf("--capture-file");
const captureFile = process.argv[captureIndex + 1];
const requests = [];
let mode = "interactive";
let buffer = Buffer.alloc(0);

function saveCapture() {
  fs.writeFileSync(captureFile, JSON.stringify(requests));
}

function writeResponse(id, result) {
  const body = JSON.stringify({ jsonrpc: "2.0", id, result });
  process.stdout.write(\`Content-Length: \${Buffer.byteLength(body, "utf8")}\\r\\n\\r\\n\${body}\`);
}

function writeError(id, code, message, data) {
  const body = JSON.stringify({ jsonrpc: "2.0", id, error: { code, message, data } });
  process.stdout.write(\`Content-Length: \${Buffer.byteLength(body, "utf8")}\\r\\n\\r\\n\${body}\`);
}

function handle(message) {
  if (!Object.prototype.hasOwnProperty.call(message, "id")) {
    return;
  }

  requests.push({ method: message.method, params: message.params ?? null });
  saveCapture();

  if (message.method === "connect") {
    writeResponse(message.id, { ok: true, protocolVersion: 3, version: "fake-rpc-surface" });
    return;
  }

  if (message.method === "session.create" || message.method === "session.resume") {
    writeResponse(message.id, {
      sessionId: message.params?.sessionId ?? "rpc-surface-session",
      workspacePath: null,
      capabilities: { supportsStreaming: true }
    });
    return;
  }

  if (message.params?.__forceError === true) {
    writeError(message.id, -32042, "deterministic fake failure", {
      method: message.method,
      nested: { retryable: false }
    });
    return;
  }

  if (message.method === "ping") {
    writeResponse(message.id, {
      message: \`pong: \${message.params?.message ?? ""}\`,
      timestamp: "2026-09-18T15:00:00.000Z",
      protocolVersion: 3
    });
    return;
  }

  if (message.method === "models.list") {
    writeResponse(message.id, {
      models: [{
        id: "fake/model",
        name: "Fake Model",
        capabilities: {
          supports: { vision: true, reasoningEffort: true },
          limits: {
            maxContextWindowTokens: 128000,
            maxPromptTokens: 120000,
            maxOutputTokens: 8000
          }
        },
        billing: { multiplier: 1 }
      }]
    });
    return;
  }

  if (message.method === "session.mode.set") {
    mode = message.params.mode;
    writeResponse(message.id, null);
    return;
  }

  if (message.method === "session.mode.get") {
    writeResponse(message.id, mode);
    return;
  }

  writeResponse(message.id, {
    method: message.method,
    params: message.params ?? null,
    state: {
      phase: "covered",
      nested: {
        items: [
          { kind: "success", value: 42 },
          { kind: "empty", value: null }
        ]
      }
    }
  });
}

function processBuffer() {
  while (true) {
    const headerEnd = buffer.indexOf("\\r\\n\\r\\n");
    if (headerEnd < 0) {
      return;
    }
    const header = buffer.subarray(0, headerEnd).toString("utf8");
    const match = /Content-Length:\\s*(\\d+)/i.exec(header);
    if (!match) {
      throw new Error("Missing Content-Length header");
    }
    const length = Number(match[1]);
    const bodyStart = headerEnd + 4;
    const bodyEnd = bodyStart + length;
    if (buffer.length < bodyEnd) {
      return;
    }
    const body = buffer.subarray(bodyStart, bodyEnd).toString("utf8");
    buffer = buffer.subarray(bodyEnd);
    handle(JSON.parse(body));
  }
}

process.stdin.on("data", chunk => {
  buffer = Buffer.concat([buffer, chunk]);
  processBuffer();
});
process.stdin.resume();
`;

type RpcMethod = {
    scope: "server" | "session";
    path: string;
    wireMethod: string;
    parameterCount: number;
    returnType: string;
};

type RpcFunction = ((params?: Record<string, unknown>) => Promise<unknown>) & {
    length: number;
};

function getPropertyName(node: ts.PropertyName): string {
    if (ts.isIdentifier(node) || ts.isStringLiteral(node) || ts.isNumericLiteral(node)) {
        return node.text;
    }
    throw new Error(`Unsupported generated RPC property name: ${node.getText()}`);
}

function findSendRequestMethod(initializer: ts.ArrowFunction): string {
    let wireMethod: string | undefined;
    const visit = (node: ts.Node): void => {
        if (
            ts.isCallExpression(node) &&
            ts.isPropertyAccessExpression(node.expression) &&
            node.expression.name.text === "sendRequest" &&
            node.arguments.length > 0 &&
            ts.isStringLiteral(node.arguments[0])
        ) {
            wireMethod = node.arguments[0].text;
        }
        ts.forEachChild(node, visit);
    };
    visit(initializer.body);
    if (!wireMethod) {
        throw new Error(
            `Generated RPC method does not call connection.sendRequest: ${initializer.getText()}`
        );
    }
    return wireMethod;
}

function collectRpcMethods(
    object: ts.ObjectLiteralExpression,
    scope: RpcMethod["scope"],
    prefix: string[] = []
): RpcMethod[] {
    const methods: RpcMethod[] = [];
    for (const property of object.properties) {
        if (!ts.isPropertyAssignment(property)) {
            continue;
        }
        const path = [...prefix, getPropertyName(property.name)];
        if (ts.isObjectLiteralExpression(property.initializer)) {
            methods.push(...collectRpcMethods(property.initializer, scope, path));
        } else if (ts.isArrowFunction(property.initializer)) {
            methods.push({
                scope,
                path: path.join("."),
                wireMethod: findSendRequestMethod(property.initializer),
                parameterCount: property.initializer.parameters.length,
                returnType: property.initializer.type?.getText() ?? "unknown",
            });
        }
    }
    return methods;
}

function getGeneratedRpcInventory(): RpcMethod[] {
    const path = fileURLToPath(new URL("../../src/generated/rpc.ts", import.meta.url));
    const source = ts.createSourceFile(
        path,
        readFileSync(path, "utf8"),
        ts.ScriptTarget.Latest,
        true,
        ts.ScriptKind.TS
    );
    const methods: RpcMethod[] = [];

    for (const statement of source.statements) {
        if (
            !ts.isFunctionDeclaration(statement) ||
            !statement.name ||
            (statement.name.text !== "createServerRpc" &&
                statement.name.text !== "createSessionRpc") ||
            !statement.body
        ) {
            continue;
        }
        const returnStatement = statement.body.statements.find(ts.isReturnStatement);
        if (
            !returnStatement?.expression ||
            !ts.isObjectLiteralExpression(returnStatement.expression)
        ) {
            throw new Error(`${statement.name.text} does not return an object literal`);
        }
        methods.push(
            ...collectRpcMethods(
                returnStatement.expression,
                statement.name.text === "createServerRpc" ? "server" : "session"
            )
        );
    }

    return methods.sort((left, right) =>
        `${left.scope}.${left.path}`.localeCompare(`${right.scope}.${right.path}`)
    );
}

function collectRuntimeFunctions(
    value: object,
    scope: RpcMethod["scope"],
    prefix: string[] = []
): Map<string, RpcFunction> {
    const functions = new Map<string, RpcFunction>();
    for (const [name, member] of Object.entries(value)) {
        const path = [...prefix, name];
        if (typeof member === "function") {
            functions.set(`${scope}.${path.join(".")}`, member as RpcFunction);
        } else if (member && typeof member === "object") {
            for (const [nestedPath, nestedFunction] of collectRuntimeFunctions(
                member,
                scope,
                path
            )) {
                functions.set(nestedPath, nestedFunction);
            }
        }
    }
    return functions;
}

describe("Generated RPC surface coverage", () => {
    it("serializes and projects every public generated RPC method", async () => {
        const directory = mkdtempSync(join(tmpdir(), "copilot-node-rpc-surface-"));
        const cliPath = join(directory, "fake-rpc-cli.js");
        const capturePath = join(directory, "capture.json");
        writeFileSync(cliPath, FAKE_RPC_CLI);

        const client = new CopilotClient({
            workingDirectory: directory,
            baseDirectory: directory,
            env: { ...process.env, COPILOT_HOME: directory },
            connection: RuntimeConnection.forStdio({
                path: cliPath,
                args: ["--capture-file", capturePath],
            }),
            useLoggedInUser: false,
        });
        onTestFinished(async () => {
            await client.forceStop();
            await rimraf(directory, { maxRetries: 10, retryDelay: 100 });
        });

        await client.start();
        const session = await client.createSession({
            sessionId: "rpc-surface-session",
            onPermissionRequest: approveAll,
        });

        const inventory = getGeneratedRpcInventory();
        const runtimeFunctions = new Map([
            ...collectRuntimeFunctions(client.rpc, "server"),
            ...collectRuntimeFunctions(session.rpc, "session"),
        ]);

        expect(inventory).toHaveLength(353);
        const boundInstallationMethods = [
            "mcp.prepareInstall",
            "mcp.applyInstall",
            "mcp.planUninstall",
            "mcp.applyUninstall",
            "mcp.installations.list",
            "mcp.installations.recover",
            "mcp.installations.status",
            "mcp.installations.cancel",
        ];
        for (const wireMethod of boundInstallationMethods) {
            expect(
                inventory.filter((method) => method.wireMethod === wireMethod),
                `Missing generated server RPC ${wireMethod}`
            ).toEqual([expect.objectContaining({ scope: "server", path: wireMethod })]);
        }
        expect([...runtimeFunctions.keys()].sort()).toEqual(
            inventory.map((method) => `${method.scope}.${method.path}`)
        );

        const invokedWireMethods = new Set<string>();
        for (const method of inventory) {
            const fullPath = `${method.scope}.${method.path}`;
            const fn = runtimeFunctions.get(fullPath);
            expect(fn, `Missing runtime RPC function ${fullPath}`).toBeDefined();
            expect(fn!.length, `Signature arity changed for ${fullPath}`).toBe(
                method.parameterCount
            );
            expect(method.returnType, `Missing generated return type for ${fullPath}`).not.toBe(
                "unknown"
            );

            const marker = { __coveragePath: fullPath };
            const result = method.parameterCount === 0 ? await fn!() : await fn!(marker);
            invokedWireMethods.add(method.wireMethod);

            if (
                method.wireMethod === "ping" ||
                method.wireMethod === "models.list" ||
                method.wireMethod === "session.mode.get" ||
                method.wireMethod === "session.mode.set"
            ) {
                continue;
            }

            expect(result).toMatchObject({
                method: method.wireMethod,
                state: {
                    phase: "covered",
                    nested: {
                        items: [
                            { kind: "success", value: 42 },
                            { kind: "empty", value: null },
                        ],
                    },
                },
            });
            const params = (result as { params: Record<string, unknown> | null }).params;
            if (method.scope === "session") {
                expect(params).toMatchObject({ sessionId: "rpc-surface-session" });
            }
            if (method.parameterCount === 1) {
                expect(params).toMatchObject(marker);
            }
        }

        const ping = await client.rpc.ping({ message: "typed projection" });
        expect(ping).toEqual({
            message: "pong: typed projection",
            timestamp: "2026-09-18T15:00:00.000Z",
            protocolVersion: 3,
        });

        const models = await client.rpc.models.list({});
        expect(models.models[0]).toMatchObject({
            id: "fake/model",
            capabilities: {
                supports: { vision: true, reasoningEffort: true },
                limits: { maxContextWindowTokens: 128000 },
            },
            billing: { multiplier: 1 },
        });

        await session.rpc.mode.set({ mode: "plan" });
        expect(await session.rpc.mode.get()).toBe("plan");
        await session.rpc.mode.set({ mode: "interactive" });
        expect(await session.rpc.mode.get()).toBe("interactive");

        await expect(
            client.rpc.ping({ message: "error", __forceError: true } as never)
        ).rejects.toMatchObject({
            code: -32042,
            message: "deterministic fake failure",
            data: {
                method: "ping",
                nested: { retryable: false },
            },
        });

        const captured = JSON.parse(readFileSync(capturePath, "utf8")) as Array<{
            method: string;
            params: Record<string, unknown> | null;
        }>;
        expect(invokedWireMethods).toEqual(new Set(inventory.map((method) => method.wireMethod)));
        const capturedWireMethods = new Set(captured.map((request) => request.method));
        for (const wireMethod of invokedWireMethods) {
            expect(capturedWireMethods.has(wireMethod), `Missing captured ${wireMethod}`).toBe(
                true
            );
        }
    });
});
