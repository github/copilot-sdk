/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { createServer } from "node:http";
import { describe, expect, it, onTestFinished } from "vitest";
import { z } from "zod";
import { approveAll, defineTool, type CopilotSession, type SessionEvent } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { waitForCondition } from "./harness/sdkTestHelper.js";

const REPLACEMENT_INSTRUCTIONS =
    "CATALOG_RECOVERY_INSTRUCTIONS: Use read_record to retrieve the current record.";
const RECONNECT_RESULT = "Catalog reconnected. Read both server records now.";

describe("MCP catalog recovery", async () => {
    const { copilotClient: client, openAiEndpoint } = await createSdkTestContext();

    it.each([
        { serverName: "bluebird", surfaced: true, staleCall: false },
        { serverName: "workiq", surfaced: false, staleCall: true },
    ])(
        "should recover $serverName instructions without withdrawing peer tools mid-turn",
        { timeout: 120_000 },
        async ({ serverName, surfaced, staleCall }) => {
            const changing = await startCatalogServer("CATALOG_RECORD");
            const healthy = await startCatalogServer("PEER_RECORD");
            let session: CopilotSession;
            let reconnects = 0;
            const reconnect = async () => {
                reconnects++;
                changing.instructions = REPLACEMENT_INSTRUCTIONS;
                await session.rpc.mcp.restartServer({ serverName });
                await waitForConnected(session, serverName);
            };
            session = await client.createSession({
                onPermissionRequest: approveAll,
                availableTools: [
                    ...(!staleCall ? ["reconnect_catalog"] : []),
                    `${serverName}-read_record`,
                    "healthy-read_record",
                ],
                mcpServers: {
                    [serverName]: { type: "http", url: changing.url, tools: ["*"] },
                    healthy: { type: "http", url: healthy.url, tools: ["*"] },
                },
                hooks: staleCall
                    ? {
                          onPreMcpToolCall: async (input) => {
                              if (
                                  input.serverName === serverName &&
                                  input.toolName === "read_record" &&
                                  reconnects === 0
                              ) {
                                  // Invalidate the selected catalog before its invocation dispatches.
                                  await reconnect();
                              }
                          },
                      }
                    : undefined,
                tools: staleCall
                    ? []
                    : [
                          defineTool("reconnect_catalog", {
                              description:
                                  "Reconnect the changing catalog before reading either server.",
                              parameters: z.object({}),
                              handler: async () => {
                                  await reconnect();
                                  return RECONNECT_RESULT;
                              },
                          }),
                      ],
            });
            onTestFinished(() => session.disconnect());
            const events: SessionEvent[] = [];
            session.on((event) => events.push(event));
            await waitForConnected(session, serverName);
            await waitForConnected(session, "healthy");

            // The instructions change at an awaited tool boundary, not during setup or a new turn.
            // Tool definitions stay identical, exercising the instruction-only refresh fast path.
            const response = await session.sendAndWait({
                prompt:
                    (staleCall
                        ? `Call read_record on ${serverName}. If it reports a changed tool catalog, retry it. `
                        : `Call reconnect_catalog exactly once. After it returns, call read_record on ${serverName}. `) +
                    "then call read_record on healthy. Do not use other tools or invent their outputs. " +
                    "Finish by reporting both record values and CATALOG_RECOVERY_COMPLETE.",
            });
            expect(response?.data.content).toContain("CATALOG_RECOVERY_COMPLETE");
            expect(response?.data.content).toContain("CATALOG_RECORD");
            expect(response?.data.content).toContain("PEER_RECORD");
            expect(reconnects).toBe(1);
            expect(changing.calls).toBeGreaterThan(0);
            expect(healthy.calls).toBeGreaterThan(0);
            expect(changing.initializations).toBe(2);
            expect(healthy.initializations).toBe(1);
            if (staleCall) {
                expect(
                    events.some(
                        (event) =>
                            event.type === "tool.execution_complete" &&
                            !event.data.success &&
                            event.data.error?.message.includes(
                                "MCP tool catalog changed before tool"
                            )
                    ),
                    "the first stale call must be refused before dispatch"
                ).toBe(true);
                expect(changing.calls, "only the recovered call may reach the MCP server").toBe(1);
            }

            const exchanges = await openAiEndpoint.getExchanges();
            const initial = exchanges[0].request;
            expect(
                JSON.stringify(initial.messages.filter((message) => message.role === "system"))
            ).not.toContain(REPLACEMENT_INSTRUCTIONS);
            const afterReconnect = exchanges.filter(({ request }) =>
                request.messages.some(
                    (message) =>
                        message.role === "tool" &&
                        JSON.stringify(message.content).includes(
                            staleCall ? "MCP tool catalog changed before tool" : RECONNECT_RESULT
                        )
                )
            );
            expect(
                afterReconnect.length,
                "the same turn must continue after reconnect"
            ).toBeGreaterThan(0);
            for (const { request } of afterReconnect) {
                const toolNames = request.tools
                    ?.filter((tool) => tool.type === "function")
                    .map((tool) => tool.function.name);
                expect(toolNames).toContain(`${serverName}-read_record`);
                expect(toolNames).toContain("healthy-read_record");
                const system = JSON.stringify(
                    request.messages.filter((message) => message.role === "system")
                );
                if (surfaced) {
                    expect(system).toContain(REPLACEMENT_INSTRUCTIONS);
                } else {
                    expect(system).not.toContain(REPLACEMENT_INSTRUCTIONS);
                }
            }
        }
    );
});

async function waitForConnected(session: CopilotSession, serverName: string): Promise<void> {
    await waitForCondition(
        async () =>
            (await session.rpc.mcp.list()).servers.some(
                (server) => server.name === serverName && server.status === "connected"
            ),
        {
            timeoutMs: 60_000,
            timeoutMessage: `${serverName} should connect`,
        }
    );
}

async function startCatalogServer(record: string) {
    const state = { instructions: "", calls: 0, initializations: 0, url: "" };
    const server = createServer(async (request, response) => {
        if (request.method === "DELETE") {
            response.writeHead(200).end();
            return;
        }
        if (request.method !== "POST") {
            response.writeHead(405).end();
            return;
        }
        const chunks: Buffer[] = [];
        for await (const chunk of request) {
            chunks.push(Buffer.from(chunk));
        }
        const message = JSON.parse(Buffer.concat(chunks).toString("utf8")) as {
            id?: string | number;
            method: string;
        };
        if (message.id === undefined) {
            response.writeHead(202).end();
            return;
        }
        let result: unknown;
        switch (message.method) {
            case "initialize":
                state.initializations++;
                result = {
                    protocolVersion: "2025-03-26",
                    capabilities: { tools: {} },
                    serverInfo: { name: "catalog-recovery", version: "1" },
                    ...(state.instructions ? { instructions: state.instructions } : {}),
                };
                break;
            case "tools/list":
                result = {
                    tools: [
                        {
                            name: "read_record",
                            description: "Read this server's current record.",
                            inputSchema: {
                                type: "object",
                                properties: {},
                                additionalProperties: false,
                            },
                            annotations: { readOnlyHint: true },
                        },
                    ],
                };
                break;
            case "tools/call":
                state.calls++;
                result = { content: [{ type: "text", text: record }], isError: false };
                break;
            default:
                response.writeHead(200, { "content-type": "application/json" }).end(
                    JSON.stringify({
                        jsonrpc: "2.0",
                        id: message.id,
                        error: { code: -32601, message: "Method not found" },
                    })
                );
                return;
        }
        response
            .writeHead(200, { "content-type": "application/json" })
            .end(JSON.stringify({ jsonrpc: "2.0", id: message.id, result }));
    });
    await new Promise<void>((resolve, reject) => {
        server.once("error", reject);
        server.listen(0, "127.0.0.1", () => {
            server.off("error", reject);
            resolve();
        });
    });
    onTestFinished(
        () =>
            new Promise<void>((resolve, reject) => {
                server.close((error) => (error ? reject(error) : resolve()));
                server.closeAllConnections();
            })
    );
    const address = server.address();
    if (!address || typeof address === "string") {
        throw new Error("Expected a TCP MCP server address");
    }
    state.url = `http://127.0.0.1:${address.port}/mcp`;
    return state;
}
