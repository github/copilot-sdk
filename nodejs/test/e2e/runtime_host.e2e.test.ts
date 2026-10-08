/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { ChildProcess } from "node:child_process";
import { randomUUID } from "node:crypto";
import { mkdir, writeFile } from "node:fs/promises";
import { createServer, type Socket } from "node:net";
import { join } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import {
    ActionType,
    ResponsePartKind,
    ToolCallConfirmationReason,
    ToolCallContributorKind,
    ToolResultContentType,
    TurnState,
    sessionReducer,
    type SessionAction,
    type ChatState,
    type SessionState,
} from "@microsoft/agent-host-protocol-v09";
import { describe, expect, it, vi } from "vitest";
import { z } from "zod";
import {
    approveAll,
    CopilotClient,
    defineTool,
    RuntimeConnection,
    type AhpHostExit,
    type AhpSessionCreateRequest,
    type AhpSessionResumeRequest,
    type CopilotSession,
} from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { connectRawAhp } from "./harness/rawAhpClient.js";
import {
    assertHostStopped as assertHostStoppedWithPid,
    assertProcessStopped,
    assertRuntimeListener,
    authenticateAhp,
    connectAhp,
    createAhpSession,
    localHostArtifacts,
    streamedTurn,
    withDeadline,
} from "./harness/runtimeHost.js";

const enabled = process.env.COPILOT_RUNTIME_HOST_E2E === "1";

// Linux /proc lets this opt-in source-build suite verify executable identity as
// well as process ancestry. No released CLI or runtime package may substitute.
describe.skipIf(!enabled)("Runtime-supervised AHP host", async () => {
    if (!enabled) return;
    if (process.platform !== "linux") throw new Error("Runtime host topology E2Es require Linux");
    const artifacts = localHostArtifacts();
    const connectionToken = "runtime-host-e2e-runtime-connection";
    const ctx = await createSdkTestContext({
        copilotClientOptions: {
            connection: RuntimeConnection.forTcp({
                path: artifacts.runtimePath,
                connectionToken,
            }),
            env: {
                ...artifacts.env,
                COPILOT_ENABLE_SECRET_FILTERING: undefined,
                COPILOT_MCP_APPS: "true",
            },
        },
    });
    const owner = ctx.copilotClient;

    it("preserves MCP Apps advertisement and channel access on callback-backed attachment", async () => {
        let original: CopilotSession | undefined;
        await using host = await owner.startAhpHost({
            localServer: {},
            createSession: async ({ config }) => {
                expect(config.enableMcpApps).toBe(true);
                original = await owner.createSession({
                    ...config,
                    onPermissionRequest: approveAll,
                    mcpServers: {
                        "app-mcp": {
                            type: "stdio",
                            command: process.execPath,
                            args: [
                                fileURLToPath(
                                    new URL(
                                        "../../../test/harness/test-mcp-server.mjs",
                                        import.meta.url
                                    )
                                ),
                            ],
                            tools: ["*"],
                        },
                    },
                });
                await vi.waitFor(
                    async () => {
                        const { servers } = await original!.rpc.mcp.list();
                        expect(servers.find((server) => server.name === "app-mcp")?.status).toBe(
                            "connected"
                        );
                    },
                    { timeout: 10_000 }
                );
                return original;
            },
        });
        const ahp = await connectAhp(host, { mcpApps: {} });
        try {
            const session = await createAhpSession(ahp, ctx.workDir, ctx.env.GITHUB_TOKEN);
            const server = await vi.waitFor(
                async () => {
                    const { result } = await ahp.client.subscribe(session.sessionUri);
                    const state = result.snapshot?.state as SessionState;
                    const server = state.customizations?.find((entry) => entry.id === "app-mcp");
                    if (server?.type !== "mcpServer")
                        throw new Error("Missing app MCP customization");
                    return server;
                },
                { timeout: 10_000 }
            );
            expect(server.mcpApp?.capabilities.serverTools).toEqual({});
            expect(server.channel).toMatch(/^mcp:\/\//);
            // MCP side-channel methods are outside AHP's typed CommandMap.
            const request = ahp.client.request as (
                method: string,
                params: { channel: string }
            ) => Promise<{ tools: { name: string }[] }>;
            const listed = await request.call(ahp.client, "tools/list", {
                channel: server.channel!,
            });
            expect(listed.tools.some((tool) => tool.name === "get_env")).toBe(true);
        } finally {
            await host.dispose();
            await ahp.client.shutdown();
            await original?.disconnect();
        }
    });

    it("hands the same app session and custom tool to AHP and releases its original object", async () => {
        if (process.env.GITHUB_ACTIONS !== "true") {
            throw new Error("Set GITHUB_ACTIONS=true for read-only canonical inference replay");
        }
        await ctx.openAiEndpoint.updateConfig({
            filePath: fileURLToPath(
                new URL(
                    "../../../test/snapshots/multi_client/both_clients_see_tool_request_and_completion_events.yaml",
                    import.meta.url
                )
            ),
            workDir: ctx.workDir,
        });
        const marker = "APPLICATION_OWNED_AHP_PROMPT";
        const tool = vi.fn(({ seed }: { seed: string }) => `MAGIC_${seed}_42`);
        const released = vi.fn();
        let original: CopilotSession | undefined;
        let requestedSessionId: string | undefined;
        await using host = await owner.startAhpHost({
            localServer: {},
            createSession: async ({ config, signal }) => {
                expect(signal.aborted).toBe(false);
                expect(config.sessionId).toBeTruthy();
                requestedSessionId = config.sessionId;
                original = await owner.createSession({
                    ...config,
                    onPermissionRequest: approveAll,
                    systemMessage: { mode: "append", content: marker },
                    tools: [
                        defineTool("magic_number", {
                            description: "Returns a magic number",
                            parameters: z.object({ seed: z.string().describe("A seed value") }),
                            handler: tool,
                        }),
                    ],
                });
                return original;
            },
            onSessionReleased: released,
        });
        const ahp = await connectAhp(host);
        try {
            const session = await createAhpSession(ahp, ctx.workDir, ctx.env.GITHUB_TOKEN);
            expect(original?.sessionId).toBe(requestedSessionId);
            expect(original?.sessionId).not.toBe(session.sessionId);
            await assertRuntimeListener(host, runtimeDetails().pid, artifacts);
            const response = await streamedTurn(
                ahp.client,
                session.chatUri,
                session.subscription,
                "Use the magic_number tool with seed 'hello' and tell me the result"
            );
            expect(tool).toHaveBeenCalledExactlyOnceWith(
                { seed: "hello" },
                expect.objectContaining({ sessionId: requestedSessionId, toolName: "magic_number" })
            );
            // Validate the final assistant response arrived (guards against truncated captures).
            expect(response.text).toContain("MAGIC_hello_42");
            expect(response.deltas).toBeGreaterThan(0);
            const exchanges = await ctx.openAiEndpoint.getExchanges();
            expect(exchanges.length).toBeGreaterThan(0);
            expect(JSON.stringify(exchanges[0].request.messages)).toContain(marker);
            expect(
                exchanges[0].request.tools?.some((entry) => entry.function.name === "magic_number")
            ).toBe(true);
            expect(released).not.toHaveBeenCalled();
            await host.dispose();
            await assertHostStopped(host, ahp);
            await vi.waitFor(() => expect(released).toHaveBeenCalledExactlyOnceWith(original), {
                timeout: 10_000,
            });
            await expect(original!.getEvents()).resolves.toEqual(expect.any(Array));
            await host.dispose();
            expect(released).toHaveBeenCalledOnce();
        } finally {
            await ahp.client.shutdown();
            // Disposal is explicitly the application's decision, after AHP released it.
            await original?.disconnect();
        }
    });

    it("routes application and AHP client tools to their respective callbacks in one session", async () => {
        if (process.env.GITHUB_ACTIONS !== "true") {
            throw new Error("Set GITHUB_ACTIONS=true for read-only canonical inference replay");
        }
        await ctx.openAiEndpoint.updateConfig({
            filePath: fileURLToPath(
                new URL(
                    "../../../test/snapshots/tools/should_execute_multiple_custom_tools_in_parallel_single_turn.yaml",
                    import.meta.url
                )
            ),
            workDir: ctx.workDir,
        });
        const appTool = vi.fn(({ city }: { city: string }) => `CITY_${city.toUpperCase()}`);
        const clientTool = vi.fn(
            ({ country }: { country: string }) => `COUNTRY_${country.toUpperCase()}`
        );
        let original: CopilotSession | undefined;
        let requestedSessionId: string | undefined;
        await using host = await owner.startAhpHost({
            localServer: {},
            createSession: async ({ config }) => {
                expect(config.sessionId).toBeTruthy();
                requestedSessionId = config.sessionId;
                original = await owner.createSession({
                    ...config,
                    onPermissionRequest: approveAll,
                    tools: [
                        defineTool("lookup_city", {
                            description: "Looks up city information",
                            parameters: z.object({ city: z.string() }),
                            handler: appTool,
                        }),
                    ],
                });
                return original;
            },
        });
        const ahp = await connectAhp(host);
        try {
            const session = await createAhpSession(ahp, ctx.workDir, ctx.env.GITHUB_TOKEN, [
                {
                    name: "lookup_country",
                    description: "Looks up country information",
                    inputSchema: {
                        type: "object",
                        properties: { country: { type: "string" } },
                        required: ["country"],
                    },
                },
            ]);
            expect(original?.sessionId).toBe(requestedSessionId);
            expect(original?.sessionId).not.toBe(session.sessionId);
            const clientCalls = new Set<string>();
            const response = await streamedTurn(
                ahp.client,
                session.chatUri,
                session.subscription,
                "Use lookup_city with 'Paris' and lookup_country with 'France' at the same time, then combine both results in your reply.",
                "claude-sonnet-5",
                (action) => {
                    if (
                        action.type === ActionType.ChatToolCallStart &&
                        action.contributor?.kind === ToolCallContributorKind.Client
                    ) {
                        expect(action.toolName).toBe("lookup_country");
                        expect(action.contributor.clientId).toBe(ahp.clientId);
                        clientCalls.add(action.toolCallId);
                    }
                    if (
                        action.type === ActionType.ChatToolCallReady &&
                        clientCalls.has(action.toolCallId)
                    ) {
                        expect(action.confirmed).toBe(ToolCallConfirmationReason.NotNeeded);
                        expect(typeof action.toolInput).toBe("string");
                        const args = JSON.parse(action.toolInput as string) as { country: string };
                        expect(args).toEqual({ country: "France" });
                        const text = clientTool(args);
                        ahp.client.dispatch(session.chatUri, {
                            type: ActionType.ChatToolCallComplete,
                            turnId: action.turnId,
                            toolCallId: action.toolCallId,
                            result: {
                                success: true,
                                pastTenseMessage: "Looked up country information",
                                content: [{ type: ToolResultContentType.Text, text }],
                            },
                        });
                    }
                }
            );
            expect(appTool).toHaveBeenCalledExactlyOnceWith(
                { city: "Paris" },
                expect.objectContaining({ sessionId: requestedSessionId, toolName: "lookup_city" })
            );
            expect(clientTool).toHaveBeenCalledExactlyOnceWith({ country: "France" });
            expect(clientCalls.size).toBe(1);
            expect(response.text).toContain("CITY_PARIS");
            expect(response.text).toContain("COUNTRY_FRANCE");
            await host.dispose();
            await assertHostStopped(host, ahp);
            await expect(original!.getEvents()).resolves.toEqual(expect.any(Array));
        } finally {
            await host.dispose();
            await ahp.client.shutdown();
            await original?.disconnect();
        }
    });

    it("releases the original app session after a genuine AHP attach failure", async () => {
        if (process.env.GITHUB_ACTIONS !== "true") {
            throw new Error("Set GITHUB_ACTIONS=true for read-only canonical inference replay");
        }
        await ctx.openAiEndpoint.updateConfig({
            filePath: fileURLToPath(
                new URL(
                    "../../../test/snapshots/multi_client/both_clients_see_tool_request_and_completion_events.yaml",
                    import.meta.url
                )
            ),
            workDir: ctx.workDir,
        });
        const released = vi.fn();
        const tool = vi.fn(({ seed }: { seed: string }) => `MAGIC_${seed}_42`);
        let original: CopilotSession | undefined;
        await using host = await owner.startAhpHost({
            localServer: {},
            createSession: async ({ config }) => {
                original = await owner.createSession({
                    ...config,
                    onPermissionRequest: approveAll,
                    tools: [
                        defineTool("magic_number", {
                            description: "Returns a magic number",
                            parameters: z.object({ seed: z.string().describe("A seed value") }),
                            handler: tool,
                        }),
                    ],
                });
                return original;
            },
            onSessionReleased: released,
        });
        const ahp = await connectAhp(host);
        try {
            await authenticateAhp(ahp, ctx.env.GITHUB_TOKEN);
            await expect(
                ahp.client.request("createSession", {
                    channel: `ahp-session:/${randomUUID()}`,
                    provider: "copilot",
                    workingDirectories: [pathToFileURL(ctx.workDir).href],
                    activeClient: {
                        clientId: ahp.clientId,
                        displayName: "Clashing AHP participant",
                        tools: [
                            {
                                name: "magic_number",
                                description:
                                    "A different participant cannot own the application's tool",
                                inputSchema: { type: "object", properties: {} },
                            },
                        ],
                    },
                })
            ).rejects.toThrow("attach failed");
            expect(original).toBeDefined();
            await vi.waitFor(() => expect(released).toHaveBeenCalledExactlyOnceWith(original), {
                timeout: 10_000,
            });
            const response = await original!.sendAndWait({
                prompt: "Use the magic_number tool with seed 'hello' and tell me the result",
            });
            // Validate the final assistant response arrived (guards against truncated captures).
            expect(response?.data.content).toContain("MAGIC_hello_42");
            expect(tool).toHaveBeenCalledOnce();
            await host.dispose();
            expect(released).toHaveBeenCalledOnce();
        } finally {
            await ahp.client.shutdown();
            await original?.disconnect();
        }
    });

    it("releases the original app session after listener disposal without disconnecting it", async () => {
        const released = vi.fn();
        let original: CopilotSession | undefined;
        await using host = await owner.startAhpHost({
            localServer: {},
            createSession: async ({ config }) => {
                original = await owner.createSession({
                    ...config,
                    onPermissionRequest: approveAll,
                });
                await original.rpc.mode.set({ mode: "plan" });
                return original;
            },
            onSessionReleased: released,
        });
        const ahp = await connectAhp(host);
        try {
            await createAhpSession(ahp, ctx.workDir, ctx.env.GITHUB_TOKEN);
            expect(await original!.rpc.mode.get()).toBe("interactive");
            await host.dispose();
            await vi.waitFor(() => expect(released).toHaveBeenCalledExactlyOnceWith(original), {
                timeout: 10_000,
            });
            await assertHostStopped(host, ahp);
            await expect(original!.getEvents()).resolves.toEqual(expect.any(Array));
            await host.dispose();
            expect(released).toHaveBeenCalledOnce();
        } finally {
            await ahp.client.shutdown();
            await original?.disconnect();
        }
    });

    it("resumes a durable app-owned session and composes tools after runtime restart", async () => {
        await configureReplay("runtime_host/app_resume_callback_composes_tools_after_history");
        const firstOwner = ctx.createClient();
        const resumedOwner = ctx.createClient();
        // Restore the persisted client's tools, not dynamically register a new tool on resume.
        const clientId = randomUUID();
        const appTool = vi.fn(({ seed }: { seed: string }) => `MAGIC_${seed}_42`);
        const clientTool = vi.fn(({ text }: { text: string }) => `CLIENT_ECHO_${text}`);
        const marker = "RESUMED_APPLICATION_CONFIG";
        const clientEchoTool = {
            name: "client_echo",
            description: "Echoes text from the AHP client",
            inputSchema: {
                type: "object" as const,
                properties: { text: { type: "string" } },
                required: ["text"],
            },
        };
        const released = vi.fn();
        let original: CopilotSession | undefined;
        let resumedOriginal: CopilotSession | undefined;
        let sessionUri = "";
        let sessionId = "";
        const create = vi.fn(async ({ config }: AhpSessionCreateRequest) => {
            expect(config.sessionId).toBeTruthy();
            sessionId = config.sessionId!;
            original = await firstOwner.createSession({
                ...config,
                onPermissionRequest: approveAll,
            });
            return original;
        });
        const first = await firstOwner.startAhpHost({
            localServer: {},
            createSession: create,
            onSessionReleased: released,
        });
        const firstAhp = await connectAhp(first, undefined, clientId);
        try {
            const session = await createAhpSession(firstAhp, ctx.workDir, ctx.env.GITHUB_TOKEN, [
                clientEchoTool,
            ]);
            sessionUri = session.sessionUri;
            expect(create).toHaveBeenCalledOnce();
            expect(original?.sessionId).toBe(sessionId);
            expect(sessionId).not.toBe(session.sessionId);
            await assertRuntimeListener(first, runtimeDetails(firstOwner).pid, artifacts);
            expect(
                (
                    await streamedTurn(
                        firstAhp.client,
                        session.chatUri,
                        session.subscription,
                        "What is 2+2?"
                    )
                ).text
            ).toContain("4");
            await first.dispose();
            await assertHostStopped(first, firstAhp, runtimeDetails(firstOwner).pid);
            await vi.waitFor(() => expect(released).toHaveBeenCalledExactlyOnceWith(original));
            await expect(original!.getEvents()).resolves.toEqual(expect.any(Array));
        } finally {
            await first.dispose();
            await firstAhp.client.shutdown();
            await original?.disconnect();
            const pid = runtimeDetails(firstOwner).pid;
            await firstOwner.stop();
            await assertProcessStopped(pid, "first application runtime");
        }

        const createAgain = vi.fn(async (): Promise<CopilotSession> => {
            throw new Error("Durable resume must not invoke createSession");
        });
        const resume = vi.fn(
            async ({ sessionId: requestedId, config, signal }: AhpSessionResumeRequest) => {
                expect(requestedId).toBe(sessionId);
                expect(signal.aborted).toBe(false);
                expect(config.continuePendingWork).toBe(false);
                expect(config.workingDirectory).toBe(ctx.workDir);
                resumedOriginal = await resumedOwner.resumeSession(requestedId, {
                    ...config,
                    onPermissionRequest: approveAll,
                    systemMessage: { mode: "append", content: marker },
                    tools: [
                        defineTool("magic_number", {
                            description: "Returns a magic number",
                            parameters: z.object({ seed: z.string().describe("A seed value") }),
                            handler: appTool,
                        }),
                    ],
                });
                return resumedOriginal;
            }
        );
        const resumedReleased = vi.fn();
        const replacement = await resumedOwner.startAhpHost({
            localServer: {},
            createSession: createAgain,
            resumeSession: resume,
            onSessionReleased: resumedReleased,
        });
        const replacementAhp = await connectAhp(replacement, undefined, clientId);
        try {
            await assertRuntimeListener(replacement, runtimeDetails(resumedOwner).pid, artifacts);
            const history = await resumeAhp(replacementAhp, sessionUri);
            expect(history.turns.map((turn) => turn.message.text)).toEqual(["What is 2+2?"]);
            expect(resume).toHaveBeenCalledOnce();
            expect(createAgain).not.toHaveBeenCalled();
            expect(resumedOriginal?.sessionId).toBe(sessionId);
            expect(resumedOriginal).not.toBe(original);
            expect(resumedReleased).not.toHaveBeenCalled();
            const { result } = await replacementAhp.client.subscribe(sessionUri);
            const chatUri = (result.snapshot?.state as SessionState).defaultChat!;
            const { subscription } = await replacementAhp.client.subscribe(chatUri);
            replacementAhp.client.dispatch(sessionUri, {
                type: ActionType.SessionActiveClientSet,
                activeClient: {
                    clientId: replacementAhp.clientId,
                    displayName: "Resumed tool owner",
                    tools: [clientEchoTool],
                },
            });
            await vi.waitFor(async () => {
                const { result } = await replacementAhp.client.subscribe(sessionUri);
                const activeClients = (result.snapshot?.state as SessionState).activeClients;
                expect(activeClients.map((entry) => entry.clientId)).toContain(
                    replacementAhp.clientId
                );
            });
            const response = await streamedTurn(
                replacementAhp.client,
                chatUri,
                subscription,
                "Call magic_number with seed 'hello' and client_echo with text 'ping', then report both results",
                "claude-sonnet-5",
                undefined,
                {
                    clientId: replacementAhp.clientId,
                    handlers: { client_echo: (input) => clientTool(input as { text: string }) },
                }
            );
            expect(appTool).toHaveBeenCalledExactlyOnceWith(
                { seed: "hello" },
                expect.objectContaining({ sessionId, toolName: "magic_number" })
            );
            expect(clientTool).toHaveBeenCalledExactlyOnceWith({ text: "ping" });
            expect(response.text).toContain("MAGIC_hello_42");
            expect(response.text).toContain("CLIENT_ECHO_ping");
            const exchanges = await ctx.openAiEndpoint.getExchanges();
            const composed = exchanges.filter((exchange) =>
                JSON.stringify(exchange.request.messages).includes("Call magic_number")
            );
            expect(composed).toHaveLength(2);
            for (const { request } of composed) {
                const names =
                    request.tools?.flatMap((tool) =>
                        tool.type === "function" ? [tool.function.name] : []
                    ) ?? [];
                expect(names.filter((name) => name === "magic_number")).toHaveLength(1);
                expect(names.filter((name) => name === "client_echo")).toHaveLength(1);
                expect(JSON.stringify(request.messages)).toContain(marker);
                expect(JSON.stringify(request.messages)).toContain("What is 2+2?");
            }
            expect(
                composed
                    .at(-1)!
                    .request.messages.filter((message) => message.role === "tool")
                    .map((message) => message.content)
                    .sort()
            ).toEqual(["CLIENT_ECHO_ping", "MAGIC_hello_42"]);
            await replacementAhp.client.subscribe(sessionUri);
            expect(resume).toHaveBeenCalledOnce();
            await replacement.dispose();
            await assertHostStopped(replacement, replacementAhp, runtimeDetails(resumedOwner).pid);
            await vi.waitFor(() =>
                expect(resumedReleased).toHaveBeenCalledExactlyOnceWith(resumedOriginal)
            );
            await expect(resumedOriginal!.getEvents()).resolves.toEqual(expect.any(Array));
            await replacement.dispose();
            expect(resumedReleased).toHaveBeenCalledOnce();
        } finally {
            await replacement.dispose();
            await replacementAhp.client.shutdown();
            await resumedOriginal?.disconnect();
            await resumedOwner.stop();
        }
    });

    it("resumes a durable app-owned session through its callback after listener restart", async () => {
        await configureReplay();
        const released = vi.fn();
        let original: CopilotSession | undefined;
        let resumedOriginal: CopilotSession | undefined;
        let sessionUri = "";
        let sessionId = "";
        await using first = await owner.startAhpHost({
            localServer: {},
            createSession: async ({ config }) => {
                expect(config.sessionId).toBeTruthy();
                sessionId = config.sessionId!;
                original = await owner.createSession({
                    ...config,
                    onPermissionRequest: approveAll,
                });
                return original;
            },
            onSessionReleased: released,
        });
        const firstAhp = await connectAhp(first);
        try {
            const session = await createAhpSession(firstAhp, ctx.workDir, ctx.env.GITHUB_TOKEN);
            sessionUri = session.sessionUri;
            expect(original?.sessionId).toBe(sessionId);
            expect(sessionId).not.toBe(session.sessionId);
            expect(
                (
                    await streamedTurn(
                        firstAhp.client,
                        session.chatUri,
                        session.subscription,
                        "What is 2+2?"
                    )
                ).text
            ).toContain("4");
            await first.dispose();
            await assertHostStopped(first, firstAhp);
            await vi.waitFor(() => expect(released).toHaveBeenCalledExactlyOnceWith(original));
        } finally {
            await firstAhp.client.shutdown();
            await original?.disconnect();
        }

        const createAgain = vi.fn(async (): Promise<CopilotSession> => {
            throw new Error("Durable resume must not invoke createSession");
        });
        const resume = vi.fn(
            async ({ sessionId: requestedId, config, signal }: AhpSessionResumeRequest) => {
                expect(requestedId).toBe(sessionId);
                expect(signal.aborted).toBe(false);
                expect(config.continuePendingWork).toBe(false);
                resumedOriginal = await owner.resumeSession(requestedId, {
                    ...config,
                    onPermissionRequest: approveAll,
                });
                return resumedOriginal;
            }
        );
        const resumedReleased = vi.fn();
        await using replacement = await owner.startAhpHost({
            localServer: {},
            createSession: createAgain,
            resumeSession: resume,
            onSessionReleased: resumedReleased,
        });
        const replacementAhp = await connectAhp(replacement);
        try {
            await resumeAhp(replacementAhp, sessionUri);
            expect(resume).toHaveBeenCalledOnce();
            expect(createAgain).not.toHaveBeenCalled();
            expect(resumedOriginal?.sessionId).toBe(sessionId);
            expect(resumedOriginal).not.toBe(original);
            expect(resumedReleased).not.toHaveBeenCalled();
            await replacement.dispose();
            await assertHostStopped(replacement, replacementAhp);
            await vi.waitFor(() =>
                expect(resumedReleased).toHaveBeenCalledExactlyOnceWith(resumedOriginal)
            );
            await expect(resumedOriginal!.getEvents()).resolves.toEqual(expect.any(Array));
            await replacement.dispose();
            expect(resumedReleased).toHaveBeenCalledOnce();
            const retainedResume = vi.fn(
                async ({ sessionId: requestedId }: AhpSessionResumeRequest) => {
                    expect(requestedId).toBe(sessionId);
                    return resumedOriginal!;
                }
            );
            const retainedReleased = vi.fn();
            await using retainedHost = await owner.startAhpHost({
                localServer: {},
                resumeSession: retainedResume,
                onSessionReleased: retainedReleased,
            });
            const retainedAhp = await connectAhp(retainedHost);
            try {
                await resumeAhp(retainedAhp, sessionUri);
                expect(retainedResume).toHaveBeenCalledOnce();
                await retainedHost.dispose();
                await assertHostStopped(retainedHost, retainedAhp);
                await vi.waitFor(() =>
                    expect(retainedReleased).toHaveBeenCalledExactlyOnceWith(resumedOriginal)
                );
            } finally {
                await retainedAhp.client.shutdown();
            }
        } finally {
            await replacementAhp.client.shutdown();
            await resumedOriginal?.disconnect();
        }
    });

    it("publishes resident sessions without invoking factories or replacing application config", async () => {
        await configureReplay();
        const marker = "PUBLISHED_APPLICATION_PROMPT";
        await using original = await owner.createSession({
            model: "claude-sonnet-5",
            onPermissionRequest: approveAll,
            systemMessage: { mode: "append", content: marker },
            excludedTools: ["bash"],
            tools: [
                defineTool("published_marker", {
                    description: "Application-owned publication marker",
                    parameters: z.object({}),
                    handler: () => "marker",
                }),
            ],
        });
        await original.rpc.mode.set({ mode: "plan" });
        const factory = vi.fn(async (): Promise<CopilotSession> => {
            throw new Error("Publication must not invoke an application factory");
        });
        const released = vi.fn();
        await using host = await owner.startAhpHost({
            localServer: {},
            createSession: factory,
            resumeSession: factory,
            onSessionReleased: released,
        });
        const ahp = await connectAhp(host);
        try {
            const published = await host.publishSession(original.sessionId);
            await authenticateAhp(ahp, ctx.env.GITHUB_TOKEN);
            const { result } = await ahp.client.subscribe(published.sessionUri);
            const state = result.snapshot?.state as SessionState;
            expect(state.lifecycle).toBe("ready");
            expect(await original.rpc.mode.get()).toBe("plan");
            expect(factory).not.toHaveBeenCalled();
            // The application, not the listener, chooses the mode for this turn.
            await original.rpc.mode.set({ mode: "interactive" });
            expect(
                (await original.sendAndWait({ prompt: "What is 2+2?" }))?.data.content
            ).toContain("4");
            const exchanges = await ctx.openAiEndpoint.getExchanges();
            expect(exchanges.length).toBeGreaterThan(0);
            const request = exchanges.at(-1)!.request;
            expect(JSON.stringify(request.messages)).toContain(marker);
            expect(request.tools?.some((tool) => tool.function.name === "published_marker")).toBe(
                true
            );
            expect(request.tools?.some((tool) => tool.function.name === "bash")).toBe(false);
            await host.dispose();
            await assertHostStopped(host, ahp);
            expect(factory).not.toHaveBeenCalled();
            expect(released).not.toHaveBeenCalled();
            await expect(original.getEvents()).resolves.toEqual(expect.any(Array));
        } finally {
            await ahp.client.shutdown();
        }
    });

    // Catalog-only flow: no model turn or replay capture is required.
    it("lists saved publications after hosting stops without restarting a listener", async () => {
        const baseDirectory = join(ctx.env.COPILOT_HOME, "offline-catalog");
        await mkdir(baseDirectory, { recursive: true });
        const offlineOwner = ctx.createClient({ baseDirectory });
        try {
            await offlineOwner.start();
            expect((await offlineOwner.rpc.host.listSessions({})).sessions).toEqual([]);
            await using session = await offlineOwner.createSession({
                onPermissionRequest: approveAll,
            });
            await using host = await offlineOwner.startAhpHost({ localServer: {} });
            const ahp = await connectAhp(host);
            const runtimePid = runtimeDetails(offlineOwner).pid;
            try {
                const publication = await host.publishSession(session.sessionId);
                const live = (await host.listSessions()).sessions;
                expect(live).toHaveLength(1);
                expect(live[0]?.resource).toBe(publication.sessionUri);
                await expect(offlineOwner.rpc.host.listSessions({})).rejects.toThrow(
                    /catalog.*in use/i
                );
                await host.dispose();
                await assertHostStopped(host, ahp, runtimePid);
                const saved = (await offlineOwner.rpc.host.listSessions({})).sessions;
                expect(saved).toHaveLength(1);
                expect(saved[0]).toMatchObject({
                    resource: live[0]!.resource,
                    title: live[0]!.title,
                    createdAt: live[0]!.createdAt,
                    modifiedAt: live[0]!.modifiedAt,
                });
                await assertHostStopped(host, ahp, runtimePid);
                expect((await offlineOwner.rpc.host.listSessions({})).sessions).toEqual(saved);
            } finally {
                await host.dispose();
                await ahp.client.shutdown();
            }
        } finally {
            await offlineOwner.stop();
        }
    });

    it("projects publication creation and metadata deltas to simultaneous 0.9 and 1.0 observers", async () => {
        if (process.env.GITHUB_ACTIONS !== "true") {
            throw new Error("Set GITHUB_ACTIONS=true to use fixture-only authentication");
        }
        await using session = await owner.createSession({
            onPermissionRequest: approveAll,
        });
        const initialTitle = `publication wire ${randomUUID()}`;
        await session.rpc.name.set({ name: initialTitle });
        await using host = await owner.startAhpHost({ localServer: {}, computeId: randomUUID() });
        await using legacy = await connectRawAhp(host, "0.9.0", ctx.env.GITHUB_TOKEN);
        await using native = await connectRawAhp(host, "1.0.0", ctx.env.GITHUB_TOKEN);
        const publication = await host.publishSession(session.sessionId);
        type Summary = {
            resource: string;
            title: string;
            createdAt: string;
            modifiedAt: string;
            chats?: { resource: string; title: string }[];
            defaultChat?: string;
        };
        function checkTopology(summary: Partial<Summary>, version: string) {
            if (version === "1.0.0") {
                expect(summary.defaultChat).toEqual(expect.any(String));
                expect(summary.chats).toHaveLength(1);
                expect(summary.chats![0]).toMatchObject({
                    resource: summary.defaultChat,
                    title: summary.title,
                });
            } else {
                expect(summary).not.toHaveProperty("chats");
                expect(summary).not.toHaveProperty("defaultChat");
            }
        }
        const observers = [
            { version: "0.9.0", client: legacy },
            { version: "1.0.0", client: native },
        ];
        const created = new Map<string, Summary>();
        for (const { version, client } of observers) {
            const added = await client.notification(
                "root/sessionAdded",
                (params) => (params.summary as Summary).resource === publication.sessionUri
            );
            const summary = added.summary as Summary;
            expect(summary).toMatchObject({
                resource: publication.sessionUri,
                title: initialTitle,
            });
            checkTopology(summary, version);
            const listed = await client.request("listSessions", { channel: "ahp-root://" });
            expect(
                (listed.items as Summary[]).find((entry) => entry.resource === summary.resource)
            ).toEqual(summary);
            created.set(version, summary);
        }
        const renamed = `publication updated ${randomUUID()}`;
        await session.rpc.name.set({ name: renamed });
        for (const { version, client } of observers) {
            const changed = await client.notification(
                "root/sessionSummaryChanged",
                (params) =>
                    params.session === publication.sessionUri &&
                    (params.changes as Partial<Summary>).title === renamed
            );
            const changes = changed.changes as Partial<Summary>;
            expect(changes).not.toHaveProperty("createdAt");
            checkTopology(changes, version);
            const listed = await client.request("listSessions", { channel: "ahp-root://" });
            const summary = (listed.items as Summary[]).find(
                (entry) => entry.resource === publication.sessionUri
            );
            expect(summary).toMatchObject({
                ...created.get(version),
                ...changes,
                title: renamed,
            });
            checkTopology(summary!, version);
        }
        const [canonical] = (await host.listSessions()).sessions;
        expect(canonical).toMatchObject({
            resource: publication.sessionUri,
            title: renamed,
        });
        // Publication/list/root observation must not cold-resume the session or
        // manufacture an inference turn just to produce the metadata delta.
        expect((await session.getEvents()).some((event) => event.type === "user.message")).toBe(
            false
        );
    });

    it("lists published session metadata without subscribing or stopping the host", async () => {
        await configureReplay();
        const title = `host catalog ${randomUUID()}`;
        const createdBefore = Date.now();
        await using session = await owner.createSession({
            model: "claude-sonnet-5",
            onPermissionRequest: approveAll,
        });
        const host = await owner.startAhpHost({ localServer: {}, computeId: randomUUID() });
        const otherOwner = new CopilotClient({
            connection: RuntimeConnection.forUri(`localhost:${runtimeDetails().port}`, {
                connectionToken,
            }),
        });
        const changedTitles: string[] = [];
        const removeTitleListener = session.on("session.title_changed", (event) => {
            changedTitles.push(event.data.title);
        });
        try {
            const publication = await host.publishSession(session.sessionId);
            const initial = (await host.listSessions()).sessions.find(
                (entry) => entry.resource === publication.sessionUri
            );
            expect(initial).toBeDefined();
            expect(initial!.title.length).toBeGreaterThan(0);
            expect((await session.getEvents()).some((event) => event.type === "user.message")).toBe(
                false
            );
            expect((await session.sendAndWait({ prompt: "What is 2+2?" }))?.data.content).toContain(
                "4"
            );
            await vi.waitFor(
                async () => {
                    const { name } = await session.rpc.name.get();
                    expect(name).toBeTruthy();
                    expect(name).not.toBe(initial!.title);
                    expect(name).not.toBe(session.sessionId);
                    const events = await session.getEvents();
                    expect(events).toEqual(
                        expect.arrayContaining([
                            expect.objectContaining({
                                type: "user.message",
                                data: expect.objectContaining({ content: "What is 2+2?" }),
                            }),
                        ])
                    );
                    expect(changedTitles).toContain(name);
                    const updated = (await host.listSessions()).sessions.find(
                        (entry) => entry.resource === publication.sessionUri
                    );
                    expect(updated).toMatchObject({ title: name, createdAt: initial!.createdAt });
                    expect(Date.parse(updated!.modifiedAt)).toBeGreaterThan(
                        Date.parse(initial!.modifiedAt)
                    );
                },
                { timeout: 15_000 }
            );

            await session.rpc.name.set({ name: title });
            await expect(session.rpc.name.get()).resolves.toMatchObject({ name: title });
            const listed = await host.listSessions();
            const summary = listed.sessions.find(
                (entry) => entry.resource === publication.sessionUri
            );
            expect(summary).toBeDefined();
            expect(summary).toMatchObject({
                resource: publication.sessionUri,
                title,
                createdAt: initial!.createdAt,
            });
            expect(Date.parse(summary!.modifiedAt)).toBeGreaterThanOrEqual(
                Date.parse(initial!.modifiedAt)
            );
            await expect(
                session.rpc.name.setAuto({ summary: "Ignored automatic rename" })
            ).resolves.toEqual({ applied: false });
            await expect(host.listSessions()).resolves.toMatchObject({
                sessions: expect.arrayContaining([
                    expect.objectContaining({ resource: publication.sessionUri, title }),
                ]),
            });
            expect(Number.isSafeInteger(summary!.status)).toBe(true);
            expect(summary!.status).toBeGreaterThanOrEqual(0);
            for (const timestamp of [summary!.createdAt, summary!.modifiedAt]) {
                expect(Number.isFinite(Date.parse(timestamp))).toBe(true);
                expect(timestamp).toMatch(/^\d{4}-\d{2}-\d{2}T/);
                expect(Date.parse(timestamp)).toBeGreaterThanOrEqual(createdBefore - 60_000);
                expect(Date.parse(timestamp)).toBeLessThanOrEqual(Date.now() + 60_000);
            }
            if (summary!.activity !== undefined) {
                expect(typeof summary!.activity).toBe("string");
            }

            await otherOwner.start();
            await expect(
                otherOwner.rpc.host.listSessions({ hostId: host.hostId })
            ).rejects.toThrow();
            await otherOwner.stop();

            // Listing must leave both the published session and its listener usable.
            await expect(session.getEvents()).resolves.toEqual(expect.any(Array));
            await expect(host.listSessions()).resolves.toMatchObject({
                sessions: expect.arrayContaining([
                    expect.objectContaining({ resource: publication.sessionUri }),
                ]),
            });
            await assertRuntimeListener(host, runtimeDetails().pid, artifacts);

            await host.dispose();
            await expect(host.listSessions()).rejects.toThrow();
            await expect(session.getEvents()).resolves.toEqual(expect.any(Array));
        } finally {
            removeTitleListener();
            await host.dispose();
            await otherOwner.stop();
        }
    });

    it("retains a compute-scoped catalog across process restart and restores application tools", async () => {
        if (process.env.GITHUB_ACTIONS !== "true") {
            throw new Error("Set GITHUB_ACTIONS=true to use fixture-only authentication");
        }
        const computeId = randomUUID();
        const title = `durable catalog ${randomUUID()}`;
        const home = join(ctx.workDir, "durable-catalog-home");
        await mkdir(home);
        const clientOptions = {
            connection: RuntimeConnection.forTcp({
                path: artifacts.runtimePath,
                connectionToken,
            }),
            workingDirectory: ctx.workDir,
            gitHubToken: ctx.env.GITHUB_TOKEN,
            env: { ...ctx.env, ...artifacts.env, COPILOT_HOME: home },
        };
        const first = new CopilotClient(clientOptions);
        try {
            const record = await (async () => {
                await using session = await first.createSession({
                    model: "claude-sonnet-5",
                    onPermissionRequest: approveAll,
                });
                await using host = await first.startAhpHost({ computeId, localServer: {} });
                const publication = await host.publishSession(session.sessionId);
                await session.rpc.name.set({ name: title });
                const summary = (await host.listSessions()).sessions.find(
                    (entry) => entry.resource === publication.sessionUri
                );
                expect(summary).toMatchObject({ resource: publication.sessionUri, title });
                await host.dispose();
                await expect(host.listSessions()).rejects.toThrow();
                await expect(session.getEvents()).resolves.toEqual(expect.any(Array));

                await using replacement = await first.startAhpHost({ computeId, localServer: {} });
                expect((await replacement.listSessions()).sessions).toContainEqual(summary);
                await session.disconnect();
                const saved = (await replacement.listSessions()).sessions.find(
                    (entry) => entry.resource === publication.sessionUri
                );
                expect(saved).toMatchObject({
                    resource: publication.sessionUri,
                    title,
                    createdAt: summary!.createdAt,
                });
                expect(Date.parse(saved!.modifiedAt)).toBeGreaterThanOrEqual(
                    Date.parse(summary!.modifiedAt)
                );
                await using isolated = await first.startAhpHost({
                    computeId: randomUUID(),
                    localServer: {},
                });
                expect((await isolated.listSessions()).sessions).toEqual([]);
                return { sessionId: session.sessionId, summary: saved! };
            })();
            const firstPid = runtimeDetails(first).pid;
            expect(await first.stop()).toEqual([]);
            await assertProcessStopped(firstPid, "Original catalog runtime");

            const restarted = new CopilotClient(clientOptions);
            let restored: CopilotSession | undefined;
            try {
                const toolHandler = vi.fn(({ value }: { value: string }) => `RESTORED_${value}`);
                const resume = vi.fn(async ({ sessionId, config }: AhpSessionResumeRequest) => {
                    expect(sessionId).toBe(record.sessionId);
                    expect(config.continuePendingWork).toBe(false);
                    restored = await restarted.resumeSession(sessionId, {
                        ...config,
                        model: "claude-sonnet-5",
                        onPermissionRequest: approveAll,
                        tools: [
                            defineTool("durable_catalog_marker", {
                                description: "Proves the application restored its tool handler",
                                parameters: z.object({ value: z.string() }),
                                handler: toolHandler,
                            }),
                        ],
                    });
                    await restored.rpc.tools.initializeAndValidate();
                    return restored;
                });
                await using host = await restarted.startAhpHost({
                    computeId,
                    localServer: {},
                    resumeSession: resume,
                });
                expect(runtimeDetails(restarted).pid).not.toBe(firstPid);
                expect((await host.listSessions()).sessions).toContainEqual(record.summary);
                expect(resume).not.toHaveBeenCalled();
                await using isolated = await restarted.startAhpHost({
                    computeId: randomUUID(),
                    localServer: {},
                });
                expect((await isolated.listSessions()).sessions).toEqual([]);
                const isolatedAhp = await connectAhp(isolated);
                try {
                    await authenticateAhp(isolatedAhp, ctx.env.GITHUB_TOKEN);
                    const catalog = await isolatedAhp.client.request("listSessions", {
                        channel: "ahp-root://",
                    });
                    expect(catalog.items.map((item) => item.resource)).not.toContain(
                        record.summary.resource
                    );
                } finally {
                    await isolatedAhp.client.shutdown();
                }

                const ahp = await connectAhp(host);
                try {
                    await authenticateAhp(ahp, ctx.env.GITHUB_TOKEN);
                    const catalog = await ahp.client.request("listSessions", {
                        channel: "ahp-root://",
                    });
                    expect(catalog.items).toEqual(
                        expect.arrayContaining([
                            expect.objectContaining({ resource: record.summary.resource, title }),
                        ])
                    );
                    expect(resume).not.toHaveBeenCalled();
                    const { result, subscription } = await ahp.client
                        .subscribe(record.summary.resource)
                        .catch(async (error: unknown) => {
                            for (const call of resume.mock.results) {
                                if (call.type === "return") await call.value;
                            }
                            throw error;
                        });
                    const state = result.snapshot?.state as SessionState | undefined;
                    expect(state?.lifecycle).toBe("ready");
                    if (!result.snapshot)
                        throw new Error("AHP resume returned no session snapshot");
                    let initialized = state!;
                    const actions: string[] = [];
                    const hasMarker = () =>
                        initialized.serverTools?.some(
                            (tool) => tool.name === "durable_catalog_marker"
                        );
                    try {
                        await withDeadline(
                            (async () => {
                                if (hasMarker()) return;
                                for await (const event of subscription) {
                                    if (event.type !== "action") continue;
                                    actions.push(event.params.action.type);
                                    if (event.params.rejectionReason)
                                        throw new Error(event.params.rejectionReason);
                                    initialized = sessionReducer(
                                        initialized,
                                        event.params.action as SessionAction
                                    );
                                    if (hasMarker()) return;
                                }
                                throw new Error(
                                    "AHP session stream closed before marker tools arrived"
                                );
                            })(),
                            "AHP serverTools projection",
                            10_000
                        );
                    } catch (error) {
                        throw new Error(
                            `AHP marker tools missing; actions=${JSON.stringify(actions)}, serverTools=${JSON.stringify(initialized.serverTools)}, lifecycle=${initialized.lifecycle}`,
                            { cause: error }
                        );
                    } finally {
                        await subscription.return();
                    }
                    expect(initialized.serverTools).toEqual(
                        expect.arrayContaining([
                            expect.objectContaining({ name: "durable_catalog_marker" }),
                        ])
                    );
                    expect(resume).toHaveBeenCalledOnce();
                    if (!restored)
                        throw new Error("AHP subscribe did not invoke the resume factory");
                    expect(restored.sessionId).toBe(record.sessionId);
                    const metadata = await restored.rpc.tools.getCurrentMetadata();
                    expect(metadata.tools?.map((tool) => tool.name)).toContain(
                        "durable_catalog_marker"
                    );
                    // Exercise the restored handler directly, without starting a model turn.
                    const executed = await restored.rpc.tools.execute({
                        name: "durable_catalog_marker",
                        arguments: { value: "fixture" },
                    });
                    expect(
                        typeof executed === "string" ? executed : executed.textResultForLlm
                    ).toBe("RESTORED_fixture");
                    if (typeof executed !== "string") {
                        expect(executed.resultType).toBe("success");
                    }
                    expect(toolHandler).toHaveBeenCalledOnce();
                    expect(toolHandler.mock.calls[0]?.[0]).toEqual({ value: "fixture" });
                    expect(await ctx.openAiEndpoint.getExchanges()).toEqual([]);
                    await host.dispose();
                    await assertHostStopped(host, ahp, runtimeDetails(restarted).pid);
                    await expect(restored.getEvents()).resolves.toEqual(expect.any(Array));
                } finally {
                    await host.dispose();
                    await ahp.client.shutdown();
                }
            } finally {
                await restored?.disconnect();
                expect(await restarted.stop()).toEqual([]);
            }
        } finally {
            expect(await first.stop()).toEqual([]);
        }
    });

    function exitObserver() {
        const exits: AhpHostExit[] = [];
        let resolve!: (exit: AhpHostExit) => void;
        const notified = new Promise<AhpHostExit>((done) => {
            resolve = done;
        });
        return {
            exits,
            notified,
            onExit: (exit: AhpHostExit) => {
                exits.push(exit);
                resolve(exit);
            },
        };
    }

    async function configureReplay(
        snapshot = "session/sendandwait_blocks_until_session_idle_and_returns_final_assistant_message"
    ) {
        if (process.env.GITHUB_ACTIONS !== "true") {
            throw new Error("Set GITHUB_ACTIONS=true for read-only canonical inference replay");
        }
        await ctx.openAiEndpoint.updateConfig({
            filePath: fileURLToPath(
                new URL(`../../../test/snapshots/${snapshot}.yaml`, import.meta.url)
            ),
            workDir: ctx.workDir,
        });
    }

    async function resumeAhp(
        ahp: Awaited<ReturnType<typeof connectAhp>>,
        sessionUri: string,
        excludedSdkSessionId?: string
    ) {
        await authenticateAhp(ahp, ctx.env.GITHUB_TOKEN);
        const listed = await ahp.client.request("listSessions", { channel: "ahp-root://" });
        const resources = listed.items.map((item) => item.resource);
        expect(resources).toContain(sessionUri);
        if (excludedSdkSessionId) {
            expect(resources).not.toContain(`ahp-session:/${excludedSdkSessionId}`);
        }
        // Subscribing a dormant catalog URI is the standard AHP resume path.
        const { result } = await ahp.client.subscribe(sessionUri);
        const session = result.snapshot?.state as SessionState | undefined;
        expect(session?.lifecycle).toBe("ready");
        expect(session?.defaultChat).toBeTruthy();
        const chat = await ahp.client.subscribe(session!.defaultChat!);
        expect(chat.result.snapshot).toBeDefined();
        const state = chat.result.snapshot?.state as ChatState;
        const turn = state.turns.find((entry) => entry.message.text === "What is 2+2?");
        expect(turn?.state).toBe(TurnState.Complete);
        expect(
            turn?.responseParts
                .filter((part) => part.kind === ResponsePartKind.Markdown)
                .map((part) => part.content)
                .join("")
        ).toContain("4");
        return state;
    }

    function runtimeDetails(client = owner) {
        const internals = client as unknown as {
            runtimePort: number;
            cliProcess: ChildProcess;
        };
        const pid = internals.cliProcess.pid;
        expect(pid).toBeGreaterThan(0);
        return { pid: pid!, port: internals.runtimePort };
    }

    function assertHostStopped(
        host: Parameters<typeof assertHostStoppedWithPid>[0],
        ahp: Parameters<typeof assertHostStoppedWithPid>[1],
        runtimePid: number | false = runtimeDetails().pid
    ) {
        return assertHostStoppedWithPid(host, ahp, runtimePid);
    }

    it("disposes listener and client without closing the runtime or owner session", async () => {
        await using session = await owner.createSession({ onPermissionRequest: approveAll });
        const observed = exitObserver();
        await using host = await owner.startAhpHost({ localServer: {}, onExit: observed.onExit });
        const ahp = await connectAhp(host);
        try {
            await ahp.client.ping();
            await assertRuntimeListener(host, runtimeDetails().pid, artifacts);
            expect(host.url).toBeDefined();
            expect(host.environmentId).toBeUndefined();
            expect(new URL(host.url!).hostname).toBe("127.0.0.1");
            expect(Number(new URL(host.url!).port)).toBeGreaterThan(0);
            expect(host.token?.length).toBeGreaterThan(0);
            await Promise.all([
                owner.rpc.host.dispose({ hostId: host.hostId }),
                owner.rpc.host.dispose({ hostId: host.hostId }),
            ]);
            const exit = await withDeadline(observed.notified, "disposed host notification");
            expect(exit.hostId).toBe(host.hostId);
            expect(exit.reason).toBe("disposed");
            expect(exit.exitCode).toBeUndefined();
            await assertHostStopped(host, ahp);
            await expect(session.getEvents()).resolves.toEqual(expect.any(Array));
            // Empty sessions are not persisted/listed until their first turn.
            await using additionalSession = await owner.createSession({
                onPermissionRequest: approveAll,
            });
            expect(additionalSession.sessionId).not.toBe(session.sessionId);
            await host.dispose();
            await owner.rpc.host.dispose({ hostId: randomUUID() });
            expect(observed.exits).toHaveLength(1);
        } finally {
            await ahp.client.shutdown();
        }
    });

    it("registers AHP credentials through the host without enabling ordinary SDK registration", async () => {
        const registerFromOwner = () =>
            owner.rpc.secrets.addFilterValues({ values: ["ordinary-sdk-filter-sentinel"] });
        await expect(registerFromOwner()).rejects.toThrow("COPILOT_ENABLE_SECRET_FILTERING");
        await using host = await owner.startAhpHost({ localServer: {} });
        const ahp = await connectAhp(host);
        try {
            const session = await createAhpSession(ahp, ctx.workDir, ctx.env.GITHUB_TOKEN);
            expect(session.sessionUri).toMatch(/^ahp-session:/);
            await expect(registerFromOwner()).rejects.toThrow("COPILOT_ENABLE_SECRET_FILTERING");
        } finally {
            await ahp.client.shutdown();
        }
    });

    it("recovers from listener startup failure while the owner session remains usable", async () => {
        await using session = await owner.createSession({ onPermissionRequest: approveAll });
        const observed = exitObserver();
        const blocker = createServer();
        await new Promise<void>((resolve) => blocker.listen(0, "127.0.0.1", resolve));
        const address = blocker.address();
        if (!address || typeof address === "string") throw new Error("Missing blocker address");
        try {
            await expect(
                owner.startAhpHost({ localServer: { port: address.port } })
            ).rejects.toThrow();
            await expect(session.getEvents()).resolves.toEqual(expect.any(Array));
        } finally {
            await new Promise<void>((resolve, reject) =>
                blocker.close((error) => (error ? reject(error) : resolve()))
            );
        }
        // There is no host process to kill: listener recovery is not process isolation.
        await using host = await owner.startAhpHost({
            localServer: { port: address.port },
            onExit: observed.onExit,
        });
        const ahp = await connectAhp(host);
        try {
            await assertRuntimeListener(host, runtimeDetails().pid, artifacts);
            await ahp.client.ping();
            await host.dispose();
            const exit = await withDeadline(observed.notified, "recovered listener disposal");
            expect(exit.reason).toBe("disposed");
            expect(exit.exitCode).toBeUndefined();
            await assertHostStopped(host, ahp);
            expect(observed.exits).toHaveLength(1);
        } finally {
            await ahp.client.shutdown();
        }
    });

    it("honors listener endpoints and supplied tokens through framed RPC", async () => {
        const token = `ahp-e2e-${randomUUID()}`;
        const ephemeral = await owner.startAhpHost({
            localServer: { hostname: "127.0.0.1", port: 0, token },
        });
        const port = Number(new URL(ephemeral.url!).port);
        expect(port).toBeGreaterThan(0);
        expect(ephemeral.token).toBe(token);
        const initialAhp = await connectAhp(ephemeral);
        try {
            await ephemeral.dispose();
            await assertHostStopped(ephemeral, initialAhp);
        } finally {
            await initialAhp.client.shutdown();
        }

        for (const hostname of ["0.0.0.0", "localhost", "::1"]) {
            await using host = await owner.startAhpHost({
                localServer: {
                    hostname,
                    port: hostname === "0.0.0.0" ? port : 0,
                    token,
                    requireConnectionToken: true,
                },
            });
            const bound = new URL(host.url!);
            expect(Number(bound.port)).toBeGreaterThan(0);
            if (hostname === "0.0.0.0") {
                expect(bound.hostname).toBe("0.0.0.0");
                expect(Number(bound.port)).toBe(port);
            } else if (hostname === "::1") {
                expect(bound.hostname).toBe("[::1]");
            } else {
                expect(["127.0.0.1", "[::1]"]).toContain(bound.hostname);
            }
            const reachable = new URL(host.url!);
            if (reachable.hostname === "0.0.0.0") reachable.hostname = "127.0.0.1";
            await expect(connectAhp({ url: reachable.href, token: undefined })).rejects.toThrow();
            await expect(
                connectAhp({ url: reachable.href, token: "wrong-token" })
            ).rejects.toThrow();
            const ahp = await connectAhp({ url: reachable.href, token: host.token });
            try {
                await ahp.client.ping();
                await assertRuntimeListener(host, runtimeDetails().pid, artifacts);
                await host.dispose();
                await assertHostStopped(host, ahp);
            } finally {
                await ahp.client.shutdown();
            }
        }
    });

    it("disables only connection-token auth and rejects invalid listener options through direct RPC", async () => {
        await expect(owner.rpc.host.start({ hostId: randomUUID() })).rejects.toThrow();
        for (const options of [
            { port: -1 },
            { port: 65536 },
            { port: 1.5 },
            { token: "" },
            { token: "supplied", requireConnectionToken: false },
        ]) {
            await expect(
                owner.rpc.host.start({ hostId: randomUUID(), localServer: options })
            ).rejects.toThrow();
        }
        await using host = await owner.startAhpHost({
            localServer: { requireConnectionToken: false },
        });
        expect(host.token).toBeUndefined();
        const ahp = await connectAhp(host);
        try {
            await ahp.client.ping();
            const file = join(ctx.workDir, "ahp-resource.txt");
            await writeFile(file, "workspace data");
            const resource = { channel: "ahp-root://", uri: pathToFileURL(file).href };
            await expect(ahp.client.request("resourceRead", resource)).rejects.toMatchObject({
                code: -32007,
            });
            await expect(
                ahp.client.request("createSession", {
                    channel: `ahp-session:/${randomUUID()}`,
                    provider: "copilot",
                    workingDirectories: [pathToFileURL(ctx.workDir).href],
                })
            ).rejects.toThrow();
            await createAhpSession(ahp, ctx.workDir, ctx.env.GITHUB_TOKEN);
            await expect(ahp.client.request("resourceRead", resource)).resolves.toMatchObject({
                data: "workspace data",
            });
            await Promise.all([
                owner.rpc.host.dispose({ hostId: host.hostId }),
                owner.rpc.host.dispose({ hostId: host.hostId }),
            ]);
            await assertHostStopped(host, ahp);
            await owner.rpc.host.dispose({ hostId: host.hostId });
        } finally {
            await ahp.client.shutdown();
        }
    });

    it("rejects a second same-home host and releases ownership on disconnect without stopping SDK sessions", async () => {
        await configureReplay();
        await using survivingSession = await owner.createSession({
            onPermissionRequest: approveAll,
        });
        const otherOwner = new CopilotClient({
            connection: RuntimeConnection.forUri(`localhost:${runtimeDetails().port}`, {
                connectionToken,
            }),
        });
        try {
            const observed = exitObserver();
            const abandonedHost = await otherOwner.startAhpHost({
                localServer: {},
                onExit: observed.onExit,
            });
            const abandonedAhp = await connectAhp(abandonedHost);
            try {
                const session = await createAhpSession(
                    abandonedAhp,
                    ctx.workDir,
                    ctx.env.GITHUB_TOKEN
                );
                const response = await streamedTurn(
                    abandonedAhp.client,
                    session.chatUri,
                    session.subscription,
                    "What is 2+2?"
                );
                expect(response.text).toContain("4");
                await assertRuntimeListener(abandonedHost, runtimeDetails().pid, artifacts);
                await expect(owner.startAhpHost({ localServer: {} })).rejects.toThrow(
                    /catalog (?:is )?already in use/
                );
                await expect(
                    owner.rpc.host.dispose({ hostId: abandonedHost.hostId })
                ).rejects.toThrow();
                await abandonedAhp.client.ping();
                await expect(survivingSession.getEvents()).resolves.toEqual(expect.any(Array));
                const socket = (otherOwner as unknown as { socket: Socket }).socket;
                expect(socket.destroyed).toBe(false);
                // Lose only this owner transport, without calling host.dispose or
                // client.stop, and without killing the shared runtime process.
                socket.destroy();
                const exit = await withDeadline(observed.notified, "owner connection loss");
                expect(exit.reason).toBe("ownerDisconnected");
                await assertHostStopped(abandonedHost, abandonedAhp);
                await expect(survivingSession.getEvents()).resolves.toEqual(expect.any(Array));
                expect(observed.exits).toHaveLength(1);
                await using replacement = await owner.startAhpHost({ localServer: {} });
                const replacementAhp = await connectAhp(replacement);
                try {
                    await resumeAhp(replacementAhp, session.sessionUri, survivingSession.sessionId);
                } finally {
                    await replacementAhp.client.shutdown();
                }
                await using additionalSession = await owner.createSession({
                    onPermissionRequest: approveAll,
                });
                expect(additionalSession.sessionId).not.toBe(survivingSession.sessionId);
            } finally {
                await abandonedAhp.client.shutdown();
            }
        } finally {
            await otherOwner.stop();
        }
    });

    it("streams beside an SDK session and recovers its catalog after repeated listener disposal", async () => {
        // Reuse the existing canonical 2+2 conversation through the existing
        // matcher. AHP and SDK both send this exact prompt/model; incompatible
        // requests still fail in replay-only mode rather than inventing replies.
        await configureReplay();
        await using sdkSession = await owner.createSession({
            model: "claude-sonnet-5",
            onPermissionRequest: approveAll,
            streaming: true,
        });
        const previousRuntimeIds = new Set(
            (await owner.listSessions()).map((item) => item.sessionId)
        );
        await using host = await owner.startAhpHost({ localServer: {} });
        const ahp = await connectAhp(host);
        try {
            const session = await createAhpSession(ahp, ctx.workDir, ctx.env.GITHUB_TOKEN);
            await assertRuntimeListener(host, runtimeDetails().pid, artifacts);
            const [response, sdkResponse] = await Promise.all([
                streamedTurn(ahp.client, session.chatUri, session.subscription, "What is 2+2?"),
                sdkSession.sendAndWait({ prompt: "What is 2+2?" }),
            ]);
            expect(response.text).toContain("4");
            expect(response.deltas).toBeGreaterThan(0);
            expect(sdkResponse?.data.content).toContain("4");

            const runtimeSessions = await vi.waitFor(
                async () => {
                    const ids = (await owner.listSessions()).map((item) => item.sessionId);
                    expect(ids).toContain(sdkSession.sessionId);
                    expect(
                        ids.filter(
                            (id) => id !== sdkSession.sessionId && !previousRuntimeIds.has(id)
                        )
                    ).toHaveLength(1);
                    return ids;
                },
                { timeout: 10_000 }
            );
            expect(runtimeSessions).toContain(sdkSession.sessionId);
            const [ahpRuntimeSessionId] = runtimeSessions.filter(
                (id) => id !== sdkSession.sessionId && !previousRuntimeIds.has(id)
            );
            expect(ahpRuntimeSessionId).not.toBe(session.sessionId);
            await using observer = await owner.resumeSession(ahpRuntimeSessionId, {
                onPermissionRequest: approveAll,
            });
            expect(observer.sessionId).toBe(ahpRuntimeSessionId);
            expect(
                (await observer.getEvents()).some(
                    (event) =>
                        event.type === "assistant.message" && event.data.content.includes("4")
                )
            ).toBe(true);
            await host.dispose();
            await assertHostStopped(host, ahp);
            expect(
                (await sdkSession.getEvents()).some((event) => event.type === "assistant.message")
            ).toBe(true);

            const observed = exitObserver();
            await using replacement = await owner.startAhpHost({
                localServer: {},
                onExit: observed.onExit,
            });
            const replacementAhp = await connectAhp(replacement);
            try {
                const resumed = await resumeAhp(
                    replacementAhp,
                    session.sessionUri,
                    sdkSession.sessionId
                );
                expect(resumed.turns.length).toBeGreaterThan(0);
                await replacement.dispose();
                expect(
                    (await withDeadline(observed.notified, "catalog owner disposal")).reason
                ).toBe("disposed");
                await assertHostStopped(replacement, replacementAhp);
            } finally {
                await replacementAhp.client.shutdown();
            }
            await using recovered = await owner.startAhpHost({ localServer: {} });
            const recoveredAhp = await connectAhp(recovered);
            try {
                const resumed = await resumeAhp(
                    recoveredAhp,
                    session.sessionUri,
                    sdkSession.sessionId
                );
                expect(resumed.turns.length).toBeGreaterThan(0);
                await expect(sdkSession.getEvents()).resolves.toEqual(expect.any(Array));
            } finally {
                await recoveredAhp.client.shutdown();
            }
        } finally {
            await ahp.client.shutdown();
        }
    });

    it("uses SDK baseDirectory for a durable catalog across runtime restart and rejects another runtime's writer", async () => {
        await configureReplay();
        const baseDirectory = join(ctx.env.COPILOT_HOME, "explicit-base");
        await mkdir(baseDirectory, { recursive: true });
        const first = ctx.createClient({ baseDirectory });
        const second = ctx.createClient({ baseDirectory });
        let sessionUri: string;
        try {
            await using host = await first.startAhpHost({ localServer: {} });
            const ahp = await connectAhp(host);
            try {
                const runtime = (first as unknown as { cliProcess: ChildProcess }).cliProcess;
                await assertRuntimeListener(host, runtime.pid!, artifacts);
                const session = await createAhpSession(ahp, ctx.workDir, ctx.env.GITHUB_TOKEN);
                sessionUri = session.sessionUri;
                const response = await streamedTurn(
                    ahp.client,
                    session.chatUri,
                    session.subscription,
                    "What is 2+2?"
                );
                expect(response.text).toContain("4");
                // Independent ordinary SDK runtimes may use this home; only a
                // second AHP server is excluded from the catalog.
                await using ordinary = await second.createSession({
                    onPermissionRequest: approveAll,
                });
                await expect(second.startAhpHost({ localServer: {} })).rejects.toThrow(
                    /catalog (?:is )?already in use/
                );
                await expect(ordinary.getEvents()).resolves.toEqual(expect.any(Array));
                await ahp.client.ping();
                await host.dispose();
                await assertHostStopped(host, ahp, runtimeDetails(first).pid);
            } finally {
                await ahp.client.shutdown();
            }
        } finally {
            await first.stop();
            await second.stop();
        }

        const restarted = ctx.createClient({ baseDirectory });
        try {
            await using host = await restarted.startAhpHost({ localServer: {} });
            const ahp = await connectAhp(host);
            try {
                const resumed = await resumeAhp(ahp, sessionUri!);
                expect(resumed.turns.length).toBeGreaterThan(0);
            } finally {
                await ahp.client.shutdown();
            }
        } finally {
            await restarted.stop();
        }
    });

    it("gracefully shuts down the runtime with an attached AHP session", async () => {
        const observed = exitObserver();
        const host = await owner.startAhpHost({ localServer: {}, onExit: observed.onExit });
        const ahp = await connectAhp(host);
        const runtimePid = runtimeDetails().pid;
        try {
            await createAhpSession(ahp, ctx.workDir, ctx.env.GITHUB_TOKEN);
            await assertRuntimeListener(host, runtimePid, artifacts);

            // Prove the actual shutdown RPC succeeds before allowing SDK stop
            // to reap its process. Eventual forced cleanup is not success.
            await withDeadline(owner.rpc.runtime.shutdown(), "runtime shutdown response");
            const exit = await withDeadline(
                observed.notified,
                "runtime shutdown host notification"
            );
            expect(exit.hostId).toBe(host.hostId);
            expect(exit.reason).toBe("runtimeShutdown");
            expect(exit.error).toBeUndefined();
            expect(exit.exitCode).toBeUndefined();
            await assertHostStopped(host, ahp, false);

            await owner.stop();
            await assertProcessStopped(runtimePid, "SDK-owned runtime");
        } finally {
            await ahp.client.shutdown();
        }
    });
});
