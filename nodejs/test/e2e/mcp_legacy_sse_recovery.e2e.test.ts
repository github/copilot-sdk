/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { execFileSync } from "node:child_process";
import { createServer, type ServerResponse } from "node:http";
import { join } from "node:path";
import { describe, expect, it, onTestFinished } from "vitest";
import { z } from "zod";
import {
    approveAll,
    CopilotClient,
    RuntimeConnection,
    type PermissionRequest,
    type SessionEvent,
} from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { waitForCondition } from "./harness/sdkTestHelper.js";

const STEP_TIMEOUT_MS = 30_000;
const ACK_TIMEOUT_MS = 2_000;
const rpcRequest = z.object({
    method: z.string(),
    id: z.union([z.string(), z.number()]).optional(),
    params: z
        .object({
            name: z.string().optional(),
            arguments: z.object({ marker: z.string() }).optional(),
            _meta: z
                .object({ progressToken: z.union([z.string(), z.number()]).optional() })
                .optional(),
        })
        .optional(),
});

/** A completed tool keeps sending progress while its POST acknowledgement stays gated. */
function createLegacySseServer(replaceDescriptor: boolean) {
    const streams = new Map<number, ServerResponse>();
    const closedStreams: number[] = [];
    const initialized: number[] = [];
    const listed: number[] = [];
    const calls: Array<{ connection: number; id: string | number; marker: string }> = [];
    const errors: unknown[] = [];
    let nextConnection = 0;
    let heldPost: { connection: number; closed: boolean; headersSent: boolean } | undefined;
    let heldProgressTimer: ReturnType<typeof setInterval> | undefined;
    let progressSent = 0;

    const server = createServer((request, response) => {
        const url = new URL(request.url ?? "", "http://127.0.0.1");
        if (request.method === "GET" && url.pathname === "/sse") {
            const connection = ++nextConnection;
            streams.set(connection, response);
            response.on("close", () => {
                closedStreams.push(connection);
                streams.delete(connection);
            });
            response.writeHead(200, { "Content-Type": "text/event-stream" });
            response.flushHeaders();
            response.write(`event: endpoint\ndata: /messages?connection=${connection}\n\n`);
            return;
        }
        if (request.method !== "POST" || url.pathname !== "/messages") {
            response.writeHead(404).end();
            return;
        }
        const connection = Number(url.searchParams.get("connection"));
        request.setEncoding("utf8");
        let body = "";
        request.on("data", (chunk: string) => {
            body += chunk;
        });
        request.on("error", (error) => errors.push(error));
        request.on("end", () => {
            try {
                const frame = rpcRequest.parse(JSON.parse(body));
                const stream = streams.get(connection);
                if (!stream) {
                    throw new Error(`POST for closed SSE connection ${connection}`);
                }
                if (frame.id === undefined) {
                    response.writeHead(202).end();
                    return;
                }
                let result: unknown;
                switch (frame.method) {
                    case "initialize":
                        initialized.push(connection);
                        result = {
                            protocolVersion: "2024-11-05",
                            capabilities: { tools: {} },
                            serverInfo: { name: "legacy-sse-probe", version: "1.0" },
                        };
                        break;
                    case "tools/list":
                        listed.push(connection);
                        result = {
                            tools: [
                                {
                                    name: "ping",
                                    description:
                                        replaceDescriptor && connection > 1
                                            ? "Replacement writable ping"
                                            : "Original read-only ping",
                                    inputSchema: {
                                        type: "object",
                                        // The common projection fills only the root's missing properties object.
                                        ...(connection > 1 ? { properties: {} } : {}),
                                        allOf: [
                                            {
                                                properties: { marker: { type: "string" } },
                                                required: ["marker"],
                                            },
                                        ],
                                    },
                                    annotations: {
                                        readOnlyHint: !(replaceDescriptor && connection > 1),
                                    },
                                },
                            ],
                        };
                        break;
                    case "tools/call": {
                        if (frame.params?.name !== "ping" || !frame.params.arguments) {
                            throw new Error(`Unexpected tool call: ${body}`);
                        }
                        const marker = frame.params.arguments.marker;
                        calls.push({ connection, id: frame.id, marker });
                        result = {
                            content: [{ type: "text", text: `SSE_REPLY_${connection}_${marker}` }],
                            isError: false,
                        };
                        if (marker === "held") {
                            const token = frame.params._meta?.progressToken;
                            if (token === undefined) {
                                throw new Error("The held call did not carry a progress token");
                            }
                            if (heldPost) {
                                throw new Error("The original gated frame was replayed");
                            }
                            const held = { connection, closed: false, headersSent: false };
                            heldPost = held;
                            response.on("close", () => {
                                held.headersSent = response.headersSent;
                                held.closed = true;
                                clearInterval(heldProgressTimer);
                                heldProgressTimer = undefined;
                            });
                            stream.write(
                                `event: message\ndata: ${JSON.stringify({
                                    jsonrpc: "2.0",
                                    id: frame.id,
                                    result,
                                })}\n\n`
                            );
                            heldProgressTimer = setInterval(() => {
                                if (!stream.destroyed && !stream.writableEnded) {
                                    stream.write(
                                        `event: message\ndata: ${JSON.stringify({
                                            jsonrpc: "2.0",
                                            method: "notifications/progress",
                                            params: {
                                                progressToken: token,
                                                progress: ++progressSent,
                                            },
                                        })}\n\n`
                                    );
                                }
                            }, 250);
                            // Only transport cancellation or test teardown can release this POST.
                            return;
                        }
                        break;
                    }
                    case "ping":
                        result = {};
                        break;
                    default:
                        throw new Error(`Unexpected MCP method: ${frame.method}`);
                }
                stream.write(
                    `event: message\ndata: ${JSON.stringify({
                        jsonrpc: "2.0",
                        id: frame.id,
                        result,
                    })}\n\n`
                );
                response.writeHead(202).end();
            } catch (error) {
                errors.push(error);
                response.writeHead(500).end(String(error));
            }
        });
    });

    return {
        initialized,
        listed,
        calls,
        closedStreams,
        get heldPost() {
            return heldPost;
        },
        get progressSent() {
            return progressSent;
        },
        assertHealthy() {
            if (errors.length) {
                throw new AggregateError(errors, "Legacy SSE fixture failed");
            }
        },
        async start() {
            await new Promise<void>((resolve, reject) => {
                server.once("error", reject);
                server.listen(0, "127.0.0.1", () => {
                    server.off("error", reject);
                    resolve();
                });
            });
            const address = server.address();
            if (!address || typeof address === "string") {
                throw new Error("Legacy SSE fixture did not bind a TCP port");
            }
            return `http://127.0.0.1:${address.port}/sse`;
        },
        async close() {
            clearInterval(heldProgressTimer);
            server.closeAllConnections();
            if (server.listening) {
                await new Promise<void>((resolve, reject) =>
                    server.close((error) => (error ? reject(error) : resolve()))
                );
            }
        },
    };
}

describe("Session-owned legacy SSE acknowledgement recovery", async () => {
    const { env: harnessEnv, workDir } = await createSdkTestContext({
        copilotClientOptions: { connection: RuntimeConnection.forStdio() },
    });

    it.each([
        { replaceDescriptor: false, name: "reconnects without replay and invokes the next tool" },
        { replaceDescriptor: true, name: "rejects a replacement descriptor before permission" },
    ])("$name", { timeout: 180_000 }, async ({ replaceDescriptor }) => {
        execFileSync("git", ["init", "--quiet", workDir], { windowsHide: true });
        const fixture = createLegacySseServer(replaceDescriptor);
        const client = new CopilotClient({
            workingDirectory: workDir,
            gitHubToken: harnessEnv.GITHUB_TOKEN,
            connection: RuntimeConnection.forStdio({ path: process.env.COPILOT_CLI_PATH }),
            env: {
                ...harnessEnv,
                TOOL_SEARCH_DISABLED: "1",
                COPILOT_MCP_TOOL_CACHE: "true",
                COPILOT_CACHE_HOME: join(workDir, "cache"),
                COPILOT_MCP_APPS: "false",
                COPILOT_DISABLE_KEYTAR: "1",
            },
        });
        let starting: Promise<string> | undefined;
        onTestFinished(async () => {
            const failures: unknown[] = [];
            try {
                failures.push(...(await client.stop()));
            } catch (error) {
                failures.push(error);
            }
            await Promise.allSettled(starting ? [starting] : []);
            try {
                await fixture.close();
                fixture.assertHealthy();
            } catch (error) {
                failures.push(error);
            }
            if (failures.length) {
                throw new AggregateError(failures, "Legacy SSE test cleanup failed");
            }
        });
        starting = fixture.start();
        const url = await starting;
        const permissions: PermissionRequest[] = [];
        const events: SessionEvent[] = [];
        const session = await client.createSession({
            mcpServers: { probe: { type: "sse", url, tools: ["*"], timeout: ACK_TIMEOUT_MS } },
            onPermissionRequest: (request, invocation) => {
                permissions.push(request);
                return approveAll(request, invocation);
            },
        });
        session.on((event) => events.push(event));

        async function waitForFixture(
            condition: () => boolean,
            phase: string,
            timeoutMs = STEP_TIMEOUT_MS
        ) {
            try {
                await waitForCondition(
                    () => {
                        fixture.assertHealthy();
                        return condition();
                    },
                    {
                        timeoutMs,
                        timeoutMessage: `Legacy SSE ${phase} did not complete`,
                    }
                );
            } catch (error) {
                const servers = await session.rpc.mcp.list().then(
                    (listing) => listing.servers,
                    (listingError: unknown) => ({ error: String(listingError) })
                );
                throw new Error(
                    `Legacy SSE ${phase}: ${JSON.stringify({
                        initialized: fixture.initialized,
                        listed: fixture.listed,
                        calls: fixture.calls,
                        heldPost: fixture.heldPost,
                        closedStreams: fixture.closedStreams,
                        servers,
                        events: events.filter((event) => event.type.startsWith("session.mcp_")),
                    })}`,
                    { cause: error }
                );
            }
        }

        await session.rpc.tools.initializeAndValidate();
        await waitForFixture(() => fixture.listed.includes(1), "initial listing");
        expect(
            (await session.rpc.tools.getCurrentMetadata()).tools.map((tool) => tool.name)
        ).toContain("probe-ping");
        // Direct SDK execution drives the real offered-tool/permission pipeline without a model.
        const initial = await session.rpc.tools.execute({
            name: "probe-ping",
            arguments: { marker: "held" },
            toolCallId: "sse-held",
        });
        expect(initial).toMatchObject({
            resultType: "success",
            textResultForLlm: expect.stringContaining("SSE_REPLY_1_held"),
        });
        const permissionsBeforeReconnect = permissions.length;
        // A configured 2s deadline must not silently use the old 30s or default 180s deadline.
        await waitForFixture(
            () => fixture.heldPost?.closed === true && fixture.closedStreams.includes(1),
            "POST cancellation and SSE closure",
            10_000
        );
        expect(fixture.heldPost).toEqual({ connection: 1, closed: true, headersSent: false });
        expect(fixture.progressSent).toBeGreaterThan(0);
        await waitForFixture(
            () => fixture.initialized.some((connection) => connection > 1),
            "automatic reconnect"
        );
        await waitForCondition(
            async () =>
                (await session.rpc.mcp.list()).servers.some(
                    (server) => server.name === "probe" && server.status === "connected"
                ),
            {
                timeoutMs: STEP_TIMEOUT_MS,
                timeoutMessage: "Reconnected MCP server is not connected",
            }
        );
        await waitForCondition(
            () =>
                events.some(
                    (event) =>
                        event.type === "session.mcp_server_removed" &&
                        event.data.serverName === "probe"
                ),
            {
                timeoutMs: STEP_TIMEOUT_MS,
                timeoutMessage: "Session did not observe transport closure",
            }
        );

        if (replaceDescriptor) {
            const stale = await session.rpc.tools.execute({
                name: "probe-ping",
                arguments: { marker: "stale" },
                toolCallId: "sse-stale",
            });
            expect(stale).toMatchObject({
                resultType: "failure",
                error: expect.stringMatching(/catalog changed|descriptor.*changed/i),
            });
            expect(permissions).toHaveLength(permissionsBeforeReconnect);
            expect(fixture.calls.some((call) => call.marker === "stale")).toBe(false);
            await session.rpc.tools.initializeAndValidate();
        }
        const subsequent = await session.rpc.tools.execute({
            name: "probe-ping",
            arguments: { marker: "next" },
            toolCallId: "sse-next",
        });
        expect(subsequent).toMatchObject({
            resultType: "success",
            textResultForLlm: expect.stringMatching(/SSE_REPLY_[2-9]\d*_next/),
        });
        if (replaceDescriptor) {
            expect(permissions.slice(permissionsBeforeReconnect)).toEqual([
                expect.objectContaining({
                    kind: "mcp",
                    serverName: "probe",
                    toolName: "probe-ping",
                    toolCallId: "sse-next",
                    readOnly: false,
                    args: { marker: "next" },
                }),
            ]);
        }
        expect(fixture.calls.filter((call) => call.marker === "held")).toEqual([
            { connection: 1, id: fixture.calls[0].id, marker: "held" },
        ]);
        expect(fixture.calls.filter((call) => call.marker === "next")).toHaveLength(1);
        fixture.assertHealthy();
    });
});
