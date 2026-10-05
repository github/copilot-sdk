/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { writeFile } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import type {
    CopilotRequestContext,
    CopilotSession,
    CustomAgentConfig,
    MCPStdioServerConfig,
} from "../../src/index.js";
import { approveAll, CopilotRequestHandler } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { waitForCondition } from "./harness/sdkTestHelper.js";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const TEST_MCP_SERVER = resolve(__dirname, "../../../test/harness/test-mcp-server.mjs");
const TEST_HARNESS_DIR = dirname(TEST_MCP_SERVER);

interface InferenceRequest {
    agentId?: string;
    parentAgentId?: string;
    systemPrompt: string;
    toolNames: string[];
}

class RecordingRequestHandler extends CopilotRequestHandler {
    readonly inferenceRequests: InferenceRequest[] = [];

    protected override async sendRequest(
        request: Request,
        ctx: CopilotRequestContext
    ): Promise<Response> {
        const body = request.body ? await request.text() : "";
        if (isInferenceUrl(request.url)) {
            this.inferenceRequests.push({
                agentId: ctx.agentId,
                parentAgentId: ctx.parentAgentId,
                systemPrompt: systemPromptFromRequest(request.url, body),
                toolNames: toolNamesFromRequest(body),
            });
            return inferenceResponse(request.url);
        }
        return bootstrapResponse(request.url);
    }
}

function toolNamesFromRequest(body: string): string[] {
    const request = JSON.parse(body) as {
        tools?: Array<{ name?: string; function?: { name?: string } }>;
    };
    return (request.tools ?? [])
        .map((tool) => tool.name ?? tool.function?.name)
        .filter((name): name is string => name !== undefined);
}

function isInferenceUrl(url: string): boolean {
    const normalized = url.toLowerCase();
    return (
        normalized.endsWith("/chat/completions") ||
        normalized.endsWith("/responses") ||
        normalized.endsWith("/v1/messages") ||
        normalized.endsWith("/messages")
    );
}

function systemPromptFromRequest(url: string, body: string): string {
    const request = JSON.parse(body) as {
        instructions?: string;
        messages?: Array<{ role?: string; content?: string }>;
        system?: string | Array<{ text?: string }>;
    };
    const normalized = url.toLowerCase();
    if (normalized.endsWith("/responses")) {
        return request.instructions ?? "";
    }
    if (normalized.endsWith("/messages")) {
        return typeof request.system === "string"
            ? request.system
            : (request.system ?? []).map((block) => block.text ?? "").join("");
    }
    return request.messages?.find((message) => message.role === "system")?.content ?? "";
}

function json(body: unknown): Response {
    return new Response(JSON.stringify(body), {
        status: 200,
        headers: { "content-type": "application/json" },
    });
}

function bootstrapResponse(url: string): Response {
    const normalized = url.toLowerCase();
    if (normalized.endsWith("/models")) {
        return json({
            data: [
                {
                    id: "claude-sonnet-5",
                    name: "Claude Sonnet 5",
                    object: "model",
                    vendor: "Anthropic",
                    version: "1",
                    preview: false,
                    model_picker_enabled: true,
                    capabilities: {
                        type: "chat",
                        family: "claude-sonnet-5",
                        tokenizer: "o200k_base",
                        limits: {
                            max_context_window_tokens: 200000,
                            max_output_tokens: 8192,
                        },
                        supports: {
                            streaming: true,
                            tool_calls: true,
                            parallel_tool_calls: true,
                            vision: true,
                        },
                    },
                },
            ],
        });
    }
    if (normalized.includes("/models/session")) {
        return json({});
    }
    if (normalized.includes("/policy")) {
        return json({ state: "enabled" });
    }
    return json({});
}

function inferenceResponse(url: string): Response {
    if (url.toLowerCase().endsWith("/messages")) {
        const events: Array<[string, unknown]> = [
            [
                "message_start",
                {
                    type: "message_start",
                    message: {
                        id: "msg_subagent_prompt_resume",
                        type: "message",
                        role: "assistant",
                        model: "claude-sonnet-5",
                        content: [],
                        stop_reason: null,
                        stop_sequence: null,
                        usage: { input_tokens: 5, output_tokens: 1 },
                    },
                },
            ],
            [
                "content_block_start",
                {
                    type: "content_block_start",
                    index: 0,
                    content_block: { type: "text", text: "" },
                },
            ],
            [
                "content_block_delta",
                {
                    type: "content_block_delta",
                    index: 0,
                    delta: { type: "text_delta", text: "SUBAGENT_TURN_COMPLETE" },
                },
            ],
            ["content_block_stop", { type: "content_block_stop", index: 0 }],
            [
                "message_delta",
                {
                    type: "message_delta",
                    delta: { stop_reason: "end_turn", stop_sequence: null },
                    usage: { output_tokens: 4 },
                },
            ],
            ["message_stop", { type: "message_stop" }],
        ];
        return new Response(
            events
                .map(([event, data]) => `event: ${event}\ndata: ${JSON.stringify(data)}\n\n`)
                .join(""),
            {
                status: 200,
                headers: { "content-type": "text/event-stream" },
            }
        );
    }
    return json({
        id: "chatcmpl-subagent-prompt-resume",
        object: "chat.completion",
        created: 1,
        model: "claude-sonnet-5",
        choices: [
            {
                index: 0,
                message: { role: "assistant", content: "SUBAGENT_TURN_COMPLETE" },
                finish_reason: "stop",
            },
        ],
        usage: { prompt_tokens: 5, completion_tokens: 4, total_tokens: 9 },
    });
}

describe("Subagent prompt resume", async () => {
    const requestHandler = new RecordingRequestHandler();
    const { copilotClient: client, env } = await createSdkTestContext({
        copilotClientOptions: {
            requestHandler,
            env: { COPILOT_EXP_COPILOT_CLI_SESSION_BASED_SUBAGENTS: "true" },
        },
    });

    async function waitForIdleAgent(session: CopilotSession, agentId: string): Promise<void> {
        await waitForCondition(
            async () => {
                const task = (await session.rpc.tasks.list()).tasks.find(
                    (candidate) => candidate.id === agentId
                );
                return task?.status === "idle";
            },
            { timeoutMessage: `Agent ${agentId} did not become idle.` }
        );
    }

    async function runResumeScenario(
        trigger: (session: CopilotSession) => Promise<void>,
        createOptions: Parameters<typeof client.createSession>[0] = {},
        beforeAgentStart: (session: CopilotSession) => Promise<void> = async () => {},
        agentType = "explore"
    ): Promise<[InferenceRequest, InferenceRequest]> {
        await writeFile(join(env.COPILOT_HOME, "settings.json"), "{}");
        const requestBaseline = requestHandler.inferenceRequests.length;
        const session = await client.createSession({
            model: "claude-sonnet-5",
            onPermissionRequest: approveAll,
            ...createOptions,
        });

        try {
            await beforeAgentStart(session);
            const { agentId } = await session.rpc.tasks.startAgent({
                agentType,
                prompt: "Reply with exactly SUBAGENT_TURN_COMPLETE.",
                name: "prompt-resume",
                description: "Exercise an idle subagent follow-up.",
                model: "claude-sonnet-5",
            });
            await waitForIdleAgent(session, agentId);

            await trigger(session);
            const sent = await session.rpc.tasks.sendMessage({
                id: agentId,
                message: "Reply again with exactly SUBAGENT_TURN_COMPLETE.",
            });
            expect(sent.sent).toBe(true);
            await waitForCondition(
                async () => {
                    const childRequests = requestHandler.inferenceRequests
                        .slice(requestBaseline)
                        .filter((request) => request.agentId === agentId && request.parentAgentId);
                    const task = (await session.rpc.tasks.list()).tasks.find(
                        (candidate) => candidate.id === agentId
                    );
                    return childRequests.length === 2 && task?.status === "idle";
                },
                { timeoutMessage: "Agent did not complete its follow-up turn." }
            );

            const childRequests = requestHandler.inferenceRequests
                .slice(requestBaseline)
                .filter((request) => request.agentId === agentId && request.parentAgentId);
            expect(childRequests).toHaveLength(2);
            return [childRequests[0]!, childRequests[1]!];
        } finally {
            const tasks = await session.rpc.tasks.list();
            for (const task of tasks.tasks) {
                if (
                    task.type === "agent" &&
                    (task.status === "running" || task.status === "idle")
                ) {
                    await session.rpc.tasks.cancel({ id: task.id });
                }
            }
            await session.disconnect();
        }
    }

    function expectAdaptedExplorePrompt(prompt: string): void {
        expect(prompt).toMatch(/^You are an exploration agent\./);
        expect(prompt).not.toContain("You are GitHub Copilot, an AI coding agent built by GitHub.");
        expect(prompt).not.toContain("<agent_instructions>");
    }

    function expectAdaptedGeneralPurposePrompt(prompt: string): void {
        expect(prompt).toMatch(
            /^As a sub-agent, complete your parent's task yourself; use another general-purpose agent only if the parent explicitly requests nested delegation\./
        );
        expect(prompt).toContain("You are running in non-interactive mode");
    }

    async function changePersistedSettings(): Promise<void> {
        await writeFile(
            join(env.COPILOT_HOME, "settings.json"),
            JSON.stringify({ subagents: { maxDepth: 4 } })
        );
    }

    it("preserves the adapted prompt on an ordinary idle-agent wake", async () => {
        const [initial, resumed] = await runResumeScenario(async () => {});

        expectAdaptedExplorePrompt(initial.systemPrompt);
        expect(resumed.systemPrompt).toBe(initial.systemPrompt);
    });

    it("preserves the adapted prompt after persisted settings rebuild the child context", async () => {
        const [initial, resumed] = await runResumeScenario(changePersistedSettings);

        expectAdaptedExplorePrompt(initial.systemPrompt);
        expect(resumed.systemPrompt).toBe(initial.systemPrompt);
    });

    it("preserves the general-purpose prompt after persisted settings rebuild the child context", async () => {
        const [initial, resumed] = await runResumeScenario(
            changePersistedSettings,
            {},
            async () => {},
            "general-purpose"
        );

        expectAdaptedGeneralPurposePrompt(initial.systemPrompt);
        expect(resumed.systemPrompt).toBe(initial.systemPrompt);
    });

    it("preserves a user-defined custom-agent prompt after persisted settings rebuild the child context", async () => {
        const customAgent: CustomAgentConfig = {
            name: "prompt-resume-custom",
            displayName: "Prompt Resume Custom",
            description: "Verifies user-defined subagent prompt resumption.",
            prompt: "You are the user-defined prompt resume agent.",
            tools: [],
        };
        const [initial, resumed] = await runResumeScenario(
            changePersistedSettings,
            { customAgents: [customAgent] },
            async () => {},
            customAgent.name
        );

        expect(initial.systemPrompt).toContain(customAgent.prompt);
        expect(initial.systemPrompt).not.toContain(
            "You are GitHub Copilot, an AI coding agent built by GitHub."
        );
        expect(resumed.systemPrompt).toBe(initial.systemPrompt);
    });

    it("rebuilds the adapted prompt after the inherited MCP catalog changes", async () => {
        const serverName = "prompt-resume-mcp";
        const config: MCPStdioServerConfig = {
            type: "local",
            command: process.execPath,
            args: [TEST_MCP_SERVER, "--server-name", serverName],
            workingDirectory: TEST_HARNESS_DIR,
            tools: ["*"],
        };
        const [initial, resumed] = await runResumeScenario(
            async (session) => {
                await session.rpc.mcp.stopServer({ serverName });
                await waitForCondition(
                    async () => !(await session.rpc.mcp.isServerRunning({ serverName })).running,
                    { timeoutMessage: `MCP server ${serverName} did not stop.` }
                );
            },
            {
                mcpServers: { [serverName]: config },
            },
            async (session) => {
                await waitForCondition(
                    async () =>
                        (await session.rpc.mcp.list()).servers.some(
                            (server) => server.name === serverName && server.status === "connected"
                        ),
                    { timeoutMessage: `MCP server ${serverName} did not connect.` }
                );
                await session.sendAndWait({
                    prompt: "Reply with exactly ROOT_TOOL_CONTEXT_READY.",
                });
            },
            "general-purpose"
        );

        expectAdaptedGeneralPurposePrompt(initial.systemPrompt);
        expectAdaptedGeneralPurposePrompt(resumed.systemPrompt);
        expect(initial.toolNames.some((name) => name.includes("get_env"))).toBe(true);
        expect(resumed.toolNames.some((name) => name.includes("get_env"))).toBe(false);
    });
});
