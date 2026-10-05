/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import {
    approveAll,
    CopilotRequestHandler,
    defineTool,
    type CopilotRequestContext,
} from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { waitForCondition } from "./harness/sdkTestHelper.js";

interface AnthropicRequest {
    system: unknown;
    tools: unknown;
    messages: Array<{ role: string; content: unknown }>;
}

function streamResponse(block: Record<string, unknown>, stopReason: string): Response {
    const events: Array<[string, Record<string, unknown>]> = [
        [
            "message_start",
            {
                type: "message_start",
                message: {
                    id: "msg_sdk_tool_update",
                    type: "message",
                    role: "assistant",
                    model: "claude-opus-5.5",
                    content: [],
                    stop_reason: null,
                    stop_sequence: null,
                    usage: { input_tokens: 5, output_tokens: 1 },
                },
            },
        ],
        ["content_block_start", { type: "content_block_start", index: 0, content_block: block }],
        ["content_block_stop", { type: "content_block_stop", index: 0 }],
        [
            "message_delta",
            {
                type: "message_delta",
                delta: { stop_reason: stopReason, stop_sequence: null },
                usage: { output_tokens: 7 },
            },
        ],
        ["message_stop", { type: "message_stop" }],
    ];
    return new Response(
        events
            .map(([event, data]) => `event: ${event}\ndata: ${JSON.stringify(data)}\n\n`)
            .join(""),
        { headers: { "content-type": "text/event-stream" } }
    );
}

function streamRejectionAfterOutput(): Response {
    const events = [
        [
            "message_start",
            {
                type: "message_start",
                message: {
                    id: "msg_sdk_tool_rejected",
                    type: "message",
                    role: "assistant",
                    model: "claude-opus-5.5",
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
                delta: { type: "text_delta", text: "already streamed" },
            },
        ],
        [
            "error",
            {
                type: "error",
                error: { type: "invalid_request_error", message: "invalid tool_addition" },
            },
        ],
    ] as const;
    return new Response(
        events
            .map(([event, data]) => `event: ${event}\ndata: ${JSON.stringify(data)}\n\n`)
            .join(""),
        { headers: { "content-type": "text/event-stream" } }
    );
}

class AnthropicToolUpdateHandler extends CopilotRequestHandler {
    readonly requests: Array<{ body: AnthropicRequest; beta: string | null }> = [];

    constructor(
        private readonly rejectPositionedUpdate: false | "before-output" | "after-output" = false
    ) {
        super();
    }

    protected override async sendRequest(
        request: Request,
        ctx: CopilotRequestContext
    ): Promise<Response> {
        if (new URL(request.url).hostname !== "api.anthropic.com") {
            return super.sendRequest(request, ctx);
        }
        const body = (await request.json()) as AnthropicRequest;
        const serialized = JSON.stringify(body.messages);
        if (
            !serialized.includes("SDK_ANTHROPIC_FIRST") &&
            !serialized.includes("SDK_ANTHROPIC_AFTER") &&
            !serialized.includes("SDK_ANTHROPIC_FOLLOW_UP")
        ) {
            return streamResponse({ type: "text", text: "Unrelated request" }, "end_turn");
        }
        this.requests.push({ body, beta: request.headers.get("anthropic-beta") });

        if (serialized.includes("SDK_ANTHROPIC_FOLLOW_UP")) {
            return streamResponse({ type: "text", text: "SDK_ANTHROPIC_LEGACY_OK" }, "end_turn");
        }
        if (
            body.messages.some(
                (message) =>
                    Array.isArray(message.content) &&
                    message.content.some((block: { type?: string }) => block.type === "tool_result")
            )
        ) {
            return streamResponse({ type: "text", text: "SDK_ANTHROPIC_TOOL_OK" }, "end_turn");
        }
        if (serialized.includes("SDK_ANTHROPIC_AFTER")) {
            if (this.rejectPositionedUpdate) {
                if (serialized.includes('"type":"tool_addition"')) {
                    if (this.rejectPositionedUpdate === "after-output") {
                        return streamRejectionAfterOutput();
                    }
                    return new Response(
                        JSON.stringify({
                            error: {
                                type: "invalid_request_error",
                                message: "invalid tool_addition",
                            },
                        }),
                        { status: 400, headers: { "content-type": "application/json" } }
                    );
                }
                return streamResponse(
                    { type: "text", text: "SDK_ANTHROPIC_RECOVERED" },
                    "end_turn"
                );
            }
            return streamResponse(
                { type: "tool_use", id: "toolu_sdk_added", name: "sdk_epoch_added", input: {} },
                "tool_use"
            );
        }
        return streamResponse({ type: "text", text: "SDK_ANTHROPIC_FIRST_OK" }, "end_turn");
    }
}

describe("SDK tool updates on direct Anthropic Messages", async () => {
    const handler = new AnthropicToolUpdateHandler();
    const { copilotClient: client } = await createSdkTestContext({
        copilotClientOptions: { requestHandler: handler },
    });

    it("keeps the top-level prefix and executes a newly added inline tool", async () => {
        let addedToolCalls = 0;
        const session = await client.createSession({
            model: "claude-opus-5.5",
            toolSearch: { enabled: false },
            onPermissionRequest: approveAll,
            provider: {
                type: "anthropic",
                baseUrl: "https://api.anthropic.com",
                apiKey: "synthetic-provider-key",
                modelId: "claude-opus-5.5",
                wireModel: "claude-opus-5.5",
            },
            tools: [
                defineTool("sdk_epoch_original", {
                    description: "Original SDK tool",
                    handler: () => "original",
                }),
                defineTool("sdk_epoch_added", {
                    description: "Added SDK tool",
                    handler: () => {
                        addedToolCalls++;
                        return "SDK_ADDED_TOOL_RESULT";
                    },
                }),
            ],
        });
        try {
            await session.rpc.tools.initializeAndValidate();
            expect(
                (await session.rpc.tools.getCurrentMetadata()).tools.map((tool) => tool.name)
            ).toEqual(expect.arrayContaining(["sdk_epoch_original", "sdk_epoch_added"]));
            const original = {
                name: "sdk_epoch_original",
                description: "Original SDK tool",
                parameters: { type: "object", properties: {} },
                defer: "never",
            } as const;
            const added = {
                name: "sdk_epoch_added",
                description: "Added SDK tool",
                parameters: { type: "object", properties: {} },
                defer: "never",
            } as const;
            await session.rpc.tools.set({ tools: [original] });

            const first = await session.sendAndWait({ prompt: "SDK_ANTHROPIC_FIRST" });
            expect(first?.data.content).toContain("SDK_ANTHROPIC_FIRST_OK");
            await session.rpc.tools.set({ tools: [original, added] });
            const second = await session.sendAndWait({
                prompt: "SDK_ANTHROPIC_AFTER: use sdk_epoch_added now.",
            });
            expect(second?.data.content).toContain("SDK_ANTHROPIC_TOOL_OK");
            expect(addedToolCalls).toBe(1);

            expect(handler.requests).toHaveLength(3);
            const [before, update, afterTool] = handler.requests;
            expect(update.body.system).toEqual(before.body.system);
            expect(update.body.tools).toEqual(before.body.tools);
            expect(afterTool.body.system).toEqual(before.body.system);
            expect(afterTool.body.tools).toEqual(before.body.tools);
            const blocks = update.body.messages.flatMap((message) =>
                Array.isArray(message.content) ? message.content : []
            );
            expect(blocks).toContainEqual(
                expect.objectContaining({
                    type: "tool_addition",
                    tool: expect.objectContaining({
                        type: "tool_definition",
                        definition: expect.objectContaining({ name: "sdk_epoch_added" }),
                    }),
                })
            );
            expect(update.beta).toContain("inline-tools-2026-09-15");
            expect(update.beta).not.toContain("mid-conversation-tool-changes-2026-07-01");
            expect(JSON.stringify(afterTool.body.messages)).toContain("SDK_ADDED_TOOL_RESULT");
        } finally {
            await session.disconnect();
        }
    });
});

describe("SDK tool updates on direct Anthropic Messages", async () => {
    const rejectingHandler = new AnthropicToolUpdateHandler("before-output");
    const { copilotClient: retryClient } = await createSdkTestContext({
        copilotClientOptions: { requestHandler: rejectingHandler },
    });

    it("retries a rejected inline update once with the full current tools", async () => {
        const original = {
            name: "sdk_epoch_original",
            description: "Original SDK tool",
            parameters: { type: "object", properties: {} },
            defer: "never",
        } as const;
        const added = {
            name: "sdk_epoch_added",
            description: "Added SDK tool",
            parameters: { type: "object", properties: {} },
            defer: "never",
        } as const;
        const session = await retryClient.createSession({
            model: "claude-opus-5.5",
            toolSearch: { enabled: false },
            onPermissionRequest: approveAll,
            provider: {
                type: "anthropic",
                baseUrl: "https://api.anthropic.com",
                apiKey: "synthetic-provider-key",
                modelId: "claude-opus-5.5",
                wireModel: "claude-opus-5.5",
            },
            tools: [
                defineTool(original.name, {
                    description: original.description,
                    handler: () => "original",
                }),
                defineTool(added.name, { description: added.description, handler: () => "added" }),
            ],
        });
        try {
            await session.rpc.tools.initializeAndValidate();
            await session.rpc.tools.set({ tools: [original] });
            const first = await session.sendAndWait({ prompt: "SDK_ANTHROPIC_FIRST" });
            expect(first?.data.content).toContain("SDK_ANTHROPIC_FIRST_OK");

            await session.rpc.tools.set({ tools: [original, added] });
            const recovered = await session.sendAndWait({
                prompt: "SDK_ANTHROPIC_AFTER: add a tool.",
            });
            expect(recovered?.data.content).toContain("SDK_ANTHROPIC_RECOVERED");
            expect(rejectingHandler.requests).toHaveLength(3);
            const [before, rejected, retry] = rejectingHandler.requests;
            expect(rejected.body.system).toEqual(before.body.system);
            expect(rejected.body.tools).toEqual(before.body.tools);
            expect(JSON.stringify(rejected.body.messages)).toContain('"type":"tool_addition"');
            expect(rejected.beta).toContain("inline-tools-2026-09-15");
            expect(retry.body.system).toEqual(before.body.system);
            expect((retry.body.tools as Array<{ name: string }>).map((tool) => tool.name)).toEqual(
                expect.arrayContaining([original.name, added.name])
            );
            expect(JSON.stringify(retry.body.messages)).not.toContain('"type":"tool_addition"');
            expect(retry.beta).not.toContain("inline-tools-2026-09-15");
        } finally {
            await session.disconnect();
        }
    });
});

describe("SDK tool updates after a streamed Anthropic rejection", async () => {
    const handler = new AnthropicToolUpdateHandler("after-output");
    const { copilotClient: client } = await createSdkTestContext({
        copilotClientOptions: { requestHandler: handler },
    });

    it("disables the rejected epoch without replaying output and uses full tools next turn", async () => {
        const original = {
            name: "sdk_epoch_original",
            description: "Original SDK tool",
            parameters: { type: "object", properties: {} },
            defer: "never",
        } as const;
        const added = {
            name: "sdk_epoch_added",
            description: "Added SDK tool",
            parameters: { type: "object", properties: {} },
            defer: "never",
        } as const;
        const session = await client.createSession({
            model: "claude-opus-5.5",
            toolSearch: { enabled: false },
            onPermissionRequest: approveAll,
            provider: {
                type: "anthropic",
                baseUrl: "https://api.anthropic.com",
                apiKey: "synthetic-provider-key",
                modelId: "claude-opus-5.5",
                wireModel: "claude-opus-5.5",
            },
            tools: [
                defineTool(original.name, {
                    description: original.description,
                    handler: () => "original",
                }),
                defineTool(added.name, { description: added.description, handler: () => "added" }),
            ],
        });
        let failedTurnIdle = false;
        const unsubscribe = session.on((event) => {
            if (event.type === "session.idle" && !event.agentId) {
                failedTurnIdle = true;
            }
        });
        try {
            await session.rpc.tools.initializeAndValidate();
            await session.rpc.tools.set({ tools: [original] });
            const first = await session.sendAndWait({ prompt: "SDK_ANTHROPIC_FIRST" });
            expect(first?.data.content).toContain("SDK_ANTHROPIC_FIRST_OK");
            failedTurnIdle = false;

            await session.rpc.tools.set({ tools: [original, added] });
            await expect(
                session.sendAndWait({ prompt: "SDK_ANTHROPIC_AFTER: add a tool." })
            ).rejects.toThrow(/invalid tool_addition/);
            await waitForCondition(() => failedTurnIdle, {
                timeoutMessage: "Rejected SDK tool update did not reach session.idle",
            });
            expect(handler.requests).toHaveLength(2);
            const [before, rejected] = handler.requests;
            expect(rejected.body.system).toEqual(before.body.system);
            expect(rejected.body.tools).toEqual(before.body.tools);
            expect(JSON.stringify(rejected.body.messages)).toContain('"type":"tool_addition"');

            const followUp = await session.sendAndWait({ prompt: "SDK_ANTHROPIC_FOLLOW_UP" });
            expect(followUp?.data.content).toContain("SDK_ANTHROPIC_LEGACY_OK");
            expect(handler.requests).toHaveLength(3);
            const request = handler.requests[2];
            expect(
                (request.body.tools as Array<{ name: string }>).map((tool) => tool.name)
            ).toEqual(expect.arrayContaining([original.name, added.name]));
            expect(JSON.stringify(request.body.messages)).not.toContain('"type":"tool_addition"');
            expect(request.beta).not.toContain("inline-tools-2026-09-15");
        } finally {
            unsubscribe();
            await session.disconnect();
        }
    });
});
