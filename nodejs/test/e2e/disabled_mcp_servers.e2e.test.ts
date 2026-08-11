/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { randomUUID } from "node:crypto";
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import {
    approveAll,
    CopilotRequestHandler,
    RuntimeConnection,
    type CopilotSession,
} from "../../src/index.js";
import { createSdkTestContext, isInProcessTransport } from "./harness/sdkTestContext.js";
import { waitForCondition } from "./harness/sdkTestHelper.js";

const __dirname = resolve(fileURLToPath(new URL(".", import.meta.url)));
const TEST_MCP_SERVER = resolve(__dirname, "../../../test/harness/test-mcp-server.mjs");
const SYNTHETIC_RESPONSE = "PERSISTED_SESSION_READY";
const MCP_TRIGGER_PROMPT = "Reply with the configured MCP test completion marker.";

class PersistingRequestHandler extends CopilotRequestHandler {
    protected override async sendRequest(request: Request): Promise<Response> {
        const body = request.body ? await request.text() : "";
        const wantsStream = /"stream"\s*:\s*true/.test(body);
        const url = request.url.toLowerCase();

        if (url.endsWith("/models")) {
            return new Response(MODEL_CATALOG_JSON, {
                status: 200,
                headers: { "content-type": "application/json" },
            });
        }

        if (url.includes("/responses")) {
            return new Response(wantsStream ? RESPONSE_STREAM : RESPONSE_JSON, {
                status: 200,
                headers: {
                    "content-type": wantsStream ? "text/event-stream" : "application/json",
                },
            });
        }

        if (url.includes("/chat/completions")) {
            return new Response(
                wantsStream ? CHAT_COMPLETION_STREAM : CHAT_COMPLETION_RESPONSE_JSON,
                {
                    status: 200,
                    headers: {
                        "content-type": wantsStream ? "text/event-stream" : "application/json",
                    },
                }
            );
        }

        return new Response("{}", {
            status: 200,
            headers: { "content-type": "application/json" },
        });
    }
}

const RESPONSE_STREAM = [
    {
        event: "response.created",
        data: {
            type: "response.created",
            response: {
                id: "persisted-session",
                object: "response",
                status: "in_progress",
                output: [],
            },
        },
    },
    {
        event: "response.output_item.added",
        data: {
            type: "response.output_item.added",
            output_index: 0,
            item: { id: "message-1", type: "message", role: "assistant", content: [] },
        },
    },
    {
        event: "response.content_part.added",
        data: {
            type: "response.content_part.added",
            output_index: 0,
            content_index: 0,
            part: { type: "output_text", text: "" },
        },
    },
    {
        event: "response.output_text.delta",
        data: {
            type: "response.output_text.delta",
            output_index: 0,
            content_index: 0,
            delta: SYNTHETIC_RESPONSE,
        },
    },
    {
        event: "response.output_text.done",
        data: {
            type: "response.output_text.done",
            output_index: 0,
            content_index: 0,
            text: SYNTHETIC_RESPONSE,
        },
    },
    {
        event: "response.completed",
        data: {
            type: "response.completed",
            response: {
                id: "persisted-session",
                object: "response",
                status: "completed",
                output: [
                    {
                        id: "message-1",
                        type: "message",
                        role: "assistant",
                        content: [{ type: "output_text", text: SYNTHETIC_RESPONSE }],
                    },
                ],
                usage: { input_tokens: 1, output_tokens: 1, total_tokens: 2 },
            },
        },
    },
]
    .map(({ event, data }) => `event: ${event}\ndata: ${JSON.stringify(data)}\n\n`)
    .join("");

const RESPONSE_JSON = JSON.stringify({
    id: "persisted-session",
    object: "response",
    status: "completed",
    output: [
        {
            id: "message-1",
            type: "message",
            role: "assistant",
            content: [{ type: "output_text", text: SYNTHETIC_RESPONSE }],
        },
    ],
    usage: { input_tokens: 1, output_tokens: 1, total_tokens: 2 },
});

const CHAT_COMPLETION_STREAM = [
    {
        id: "persisted-session",
        object: "chat.completion.chunk",
        created: 1,
        model: "claude-sonnet-4.5",
        choices: [
            {
                index: 0,
                delta: { role: "assistant", content: SYNTHETIC_RESPONSE },
                finish_reason: null,
            },
        ],
    },
    {
        id: "persisted-session",
        object: "chat.completion.chunk",
        created: 1,
        model: "claude-sonnet-4.5",
        choices: [{ index: 0, delta: {}, finish_reason: "stop" }],
    },
]
    .map((data) => `data: ${JSON.stringify(data)}\n\n`)
    .concat("data: [DONE]\n\n")
    .join("");

const CHAT_COMPLETION_RESPONSE_JSON = JSON.stringify({
    id: "persisted-session",
    object: "chat.completion",
    created: 1,
    model: "claude-sonnet-4.5",
    choices: [
        {
            index: 0,
            message: { role: "assistant", content: SYNTHETIC_RESPONSE },
            finish_reason: "stop",
        },
    ],
    usage: { prompt_tokens: 1, completion_tokens: 1, total_tokens: 2 },
});

const MODEL_CATALOG_JSON = JSON.stringify({
    data: [
        {
            id: "claude-sonnet-4.5",
            name: "Claude Sonnet 4.5",
            object: "model",
            vendor: "Anthropic",
            version: "1",
            preview: false,
            model_picker_enabled: true,
            capabilities: {
                type: "chat",
                family: "claude-sonnet-4.5",
                tokenizer: "o200k_base",
                limits: { max_context_window_tokens: 200000, max_output_tokens: 8192 },
                supports: { streaming: true, tool_calls: true, parallel_tool_calls: true },
            },
        },
    ],
});

describe("disabled MCP servers", async () => {
    const {
        copilotClient: client,
        createClient,
        openAiEndpoint,
        workDir,
    } = await createSdkTestContext({
        copilotClientOptions: {
            requestHandler: new PersistingRequestHandler(),
        },
    });

    function createPluginDirectory(prefix: string): {
        pluginDirectory: string;
        controlMarker: string;
        disabledMarker: string;
    } {
        const pluginDirectory = join(workDir, `${prefix}-${randomUUID()}`);
        mkdirSync(pluginDirectory, { recursive: true });
        const controlMarker = join(pluginDirectory, "control-started.log");
        const disabledMarker = join(pluginDirectory, "disabled-started.log");

        writeFileSync(
            join(pluginDirectory, "plugin.json"),
            JSON.stringify({
                name: `${prefix}-${randomUUID()}`,
                version: "1.0.0",
            })
        );
        writeFileSync(
            join(pluginDirectory, ".mcp.json"),
            JSON.stringify({
                mcpServers: {
                    control: {
                        type: "stdio",
                        command: process.execPath,
                        args: [
                            TEST_MCP_SERVER,
                            "--startup-marker",
                            controlMarker,
                            "--server-name",
                            "control",
                        ],
                    },
                    disabled: {
                        type: "stdio",
                        command: process.execPath,
                        args: [
                            TEST_MCP_SERVER,
                            "--startup-marker",
                            disabledMarker,
                            "--server-name",
                            "disabled",
                        ],
                    },
                },
            })
        );

        return { pluginDirectory, controlMarker, disabledMarker };
    }

    function markerCount(markerPath: string): number {
        if (!existsSync(markerPath)) {
            return 0;
        }
        return readFileSync(markerPath, "utf8").trim().split("\n").filter(Boolean).length;
    }

    async function waitForMarkerCount(markerPath: string, expectedCount: number): Promise<void> {
        await waitForCondition(() => markerCount(markerPath) >= expectedCount, {
            timeoutMs: 60_000,
            intervalMs: 100,
            timeoutMessage: `Timed out waiting for ${markerPath} to be written ${expectedCount} time(s).`,
        });
    }

    async function waitForMcpStatus(
        session: CopilotSession,
        serverName: string,
        expectedStatus: string
    ): Promise<void> {
        let lastStatus = "<not listed>";
        await waitForCondition(
            async () => {
                const result = await session.rpc.mcp.list();
                const server = result.servers.find((candidate) => candidate.name === serverName);
                lastStatus = server?.status ?? "<not listed>";
                return lastStatus === expectedStatus;
            },
            {
                timeoutMs: 60_000,
                intervalMs: 100,
                timeoutMessage: `${serverName} did not reach ${expectedStatus}; last status was ${lastStatus}.`,
            }
        );
    }

    function expectSyntheticResponse(response: Awaited<ReturnType<CopilotSession["sendAndWait"]>>) {
        expect(response?.data.content).toBe(SYNTHETIC_RESPONSE);
    }

    async function drainPostCreateRpc(session: CopilotSession): Promise<void> {
        // Drain a non-MCP post-create RPC without initializing MCP before the first model turn.
        await session.rpc.metadata.snapshot();
    }

    async function mcpRequestCount(): Promise<number> {
        const requests = await openAiEndpoint.getRequests();
        return requests.filter((request) => request.method === "POST" && request.url === "/mcp")
            .length;
    }

    async function waitForMcpRequestCount(expectedCount: number): Promise<void> {
        let lastCount = 0;
        await waitForCondition(
            async () => {
                lastCount = await mcpRequestCount();
                return lastCount >= expectedCount;
            },
            {
                timeoutMs: 60_000,
                intervalMs: 100,
                timeoutMessage: `Timed out waiting for ${expectedCount} /mcp request(s); saw ${lastCount}.`,
            }
        );
    }

    it(
        "keeps disabled plugin MCP servers per-session on create",
        { timeout: 120_000 },
        async () => {
            const {
                pluginDirectory: disabledPluginDirectory,
                controlMarker: disabledControlMarker,
                disabledMarker,
            } = createPluginDirectory("disabled-mcp-create");

            await using disabledSession = await client.createSession({
                onPermissionRequest: approveAll,
                pluginDirectories: [disabledPluginDirectory],
                disabledMcpServers: ["disabled"],
            });

            await drainPostCreateRpc(disabledSession);
            expect(existsSync(disabledControlMarker)).toBe(false);
            expect(existsSync(disabledMarker)).toBe(false);
            expectSyntheticResponse(
                await disabledSession.sendAndWait({ prompt: MCP_TRIGGER_PROMPT })
            );
            await waitForMarkerCount(disabledControlMarker, 1);
            expect(existsSync(disabledMarker)).toBe(false);
            await waitForMcpStatus(disabledSession, "control", "connected");
            await waitForMcpStatus(disabledSession, "disabled", "disabled");

            const {
                pluginDirectory: enabledPluginDirectory,
                controlMarker: enabledControlMarker,
                disabledMarker: enabledDisabledMarker,
            } = createPluginDirectory("enabled-mcp-create");
            await using enabledSession = await client.createSession({
                onPermissionRequest: approveAll,
                pluginDirectories: [enabledPluginDirectory],
            });
            await drainPostCreateRpc(enabledSession);
            expect(existsSync(enabledControlMarker)).toBe(false);
            expect(existsSync(enabledDisabledMarker)).toBe(false);
            expectSyntheticResponse(
                await enabledSession.sendAndWait({ prompt: MCP_TRIGGER_PROMPT })
            );
            await waitForMarkerCount(enabledControlMarker, 1);
            await waitForMarkerCount(enabledDisabledMarker, 1);
            await waitForMcpStatus(enabledSession, "control", "connected");
            await waitForMcpStatus(enabledSession, "disabled", "connected");
        }
    );

    it(
        "keeps the built-in GitHub MCP server disabled on the first message",
        { timeout: 120_000 },
        async () => {
            const disabledSession = await client.createSession({
                onPermissionRequest: approveAll,
                enableConfigDiscovery: true,
                enableMcpApps: true,
                githubMcpToolConfig: { enableAllTools: true },
                disabledMcpServers: ["github-mcp-server"],
            });

            let disabledRequestsBeforeFirstMessage: number;
            try {
                await drainPostCreateRpc(disabledSession);
                disabledRequestsBeforeFirstMessage = await mcpRequestCount();
                expect(disabledRequestsBeforeFirstMessage).toBe(0);
                expectSyntheticResponse(
                    await disabledSession.sendAndWait({ prompt: MCP_TRIGGER_PROMPT })
                );
                expect(await mcpRequestCount()).toBe(disabledRequestsBeforeFirstMessage);
                await waitForMcpStatus(disabledSession, "github-mcp-server", "disabled");
                expect(await mcpRequestCount()).toBe(disabledRequestsBeforeFirstMessage);
            } finally {
                await disabledSession.disconnect();
            }

            expect(await mcpRequestCount()).toBe(disabledRequestsBeforeFirstMessage);

            await using enabledSession = await client.createSession({
                onPermissionRequest: approveAll,
                enableConfigDiscovery: true,
                enableMcpApps: true,
                githubMcpToolConfig: { enableAllTools: true },
            });
            await drainPostCreateRpc(enabledSession);
            const requestsBeforeFirstMessage = await mcpRequestCount();
            expect(requestsBeforeFirstMessage).toBe(0);
            expectSyntheticResponse(
                await enabledSession.sendAndWait({ prompt: MCP_TRIGGER_PROMPT })
            );
            await waitForMcpRequestCount(requestsBeforeFirstMessage + 1);
            await waitForMcpStatus(enabledSession, "github-mcp-server", "connected");
        }
    );

    it.skipIf(isInProcessTransport)(
        "applies disabled plugin MCP servers on cold stdio resume",
        async () => {
            const { pluginDirectory, controlMarker, disabledMarker } =
                createPluginDirectory("disabled-mcp-resume");
            const initialClient = createClient({
                connection: RuntimeConnection.forStdio({ path: process.env.COPILOT_CLI_PATH }),
                requestHandler: new PersistingRequestHandler(),
            });
            const resumeClient = createClient({
                connection: RuntimeConnection.forStdio({ path: process.env.COPILOT_CLI_PATH }),
            });

            try {
                const originalSession = await initialClient.createSession({
                    onPermissionRequest: approveAll,
                    enableSessionStore: true,
                });
                const sessionId = originalSession.sessionId;
                // A session.log entry alone does not materialize a session that a
                // restarted runtime can resume. This self-contained model turn
                // persists it without initializing MCP because no plugin directory
                // is supplied until the resume request below.
                const response = await originalSession.sendAndWait({
                    prompt: "Return the configured persistence marker.",
                });
                expectSyntheticResponse(response);

                expect(existsSync(controlMarker)).toBe(false);
                expect(existsSync(disabledMarker)).toBe(false);
                await initialClient.stop();

                await using resumedSession = await resumeClient.resumeSession(sessionId, {
                    onPermissionRequest: approveAll,
                    enableSessionStore: true,
                    pluginDirectories: [pluginDirectory],
                    disabledMcpServers: ["disabled"],
                });
                await waitForMcpStatus(resumedSession, "control", "connected");
                await waitForMcpStatus(resumedSession, "disabled", "disabled");
                await waitForMarkerCount(controlMarker, 1);
                expect(existsSync(disabledMarker)).toBe(false);
            } finally {
                await initialClient.stop().catch(() => {});
                await resumeClient.stop().catch(() => {});
            }
        }
    );
});
