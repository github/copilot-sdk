/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll, CopilotRequestHandler, type CopilotRequestContext } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

const MODEL = "claude-sonnet-5";
const FIRST_PROMPT = "Use the sql tool to insert the Anthropic replay todo.";
const FOLLOWUP_PROMPT = "Confirm the Anthropic replay todo was inserted.";
const TOOL_ID = "toolu_sdk_replay";
const TOOL_INPUT = {
    description: "Insert Anthropic replay todo",
    query: "INSERT INTO todos (id, title, status) VALUES ('sdk-replay', 'Anthropic replay', 'done')",
};

type Block =
    | { type: "thinking"; thinking: string; signature: string }
    | { type: "text"; text: string }
    | { type: "tool_use"; id: string; name: string; input: typeof TOOL_INPUT };

function messageStream(blocks: Block[], stopReason: "tool_use" | "end_turn"): Response {
    const events: Array<[string, unknown]> = [
        [
            "message_start",
            {
                type: "message_start",
                message: {
                    id: "msg_sdk_replay",
                    type: "message",
                    role: "assistant",
                    model: MODEL,
                    content: [],
                    stop_reason: null,
                    stop_sequence: null,
                    usage: { input_tokens: 5, output_tokens: 1 },
                },
            },
        ],
    ];
    for (const [index, block] of blocks.entries()) {
        events.push([
            "content_block_start",
            {
                type: "content_block_start",
                index,
                content_block:
                    block.type === "tool_use"
                        ? { type: "tool_use", id: block.id, name: block.name, input: {} }
                        : { type: block.type, [block.type === "text" ? "text" : "thinking"]: "" },
            },
        ]);
        events.push([
            "content_block_delta",
            {
                type: "content_block_delta",
                index,
                delta:
                    block.type === "thinking"
                        ? { type: "thinking_delta", thinking: block.thinking }
                        : block.type === "text"
                          ? { type: "text_delta", text: block.text }
                          : { type: "input_json_delta", partial_json: JSON.stringify(block.input) },
            },
        ]);
        if (block.type === "thinking") {
            events.push([
                "content_block_delta",
                {
                    type: "content_block_delta",
                    index,
                    delta: { type: "signature_delta", signature: block.signature },
                },
            ]);
        }
        events.push(["content_block_stop", { type: "content_block_stop", index }]);
    }
    events.push(
        [
            "message_delta",
            {
                type: "message_delta",
                delta: { stop_reason: stopReason, stop_sequence: null },
                usage: { output_tokens: 10 },
            },
        ],
        ["message_stop", { type: "message_stop" }]
    );
    return new Response(
        events
            .map(([event, data]) => `event: ${event}\ndata: ${JSON.stringify(data)}\n\n`)
            .join(""),
        { status: 200, headers: { "content-type": "text/event-stream" } }
    );
}

class AnthropicReplayHandler extends CopilotRequestHandler {
    readonly requests: Array<{ messages: Array<{ role: string; content: unknown }> }> = [];

    constructor(private readonly blocks: Block[]) {
        super();
    }

    protected override async sendRequest(
        request: Request,
        _ctx: CopilotRequestContext
    ): Promise<Response> {
        if (!request.url.endsWith("/messages")) {
            throw new Error(`Unexpected SDK replay request: ${request.url}`);
        }
        const body = (await request.json()) as {
            messages: Array<{ role: string; content: unknown }>;
        };
        this.requests.push(body);
        const messages = JSON.stringify(body.messages);
        if (messages.includes(FOLLOWUP_PROMPT)) {
            return messageStream([{ type: "text", text: "SDK_REPLAY_FOLLOWUP_DONE" }], "end_turn");
        }
        if (messages.includes(TOOL_ID)) {
            return messageStream([{ type: "text", text: "SDK_REPLAY_TOOL_DONE" }], "end_turn");
        }
        return messageStream(this.blocks, "tool_use");
    }
}

function replayedAssistantContent(
    request: { messages: Array<{ role: string; content: unknown }> } | undefined
): unknown[] {
    const assistant = request?.messages.find((message) => message.role === "assistant");
    if (!assistant || !Array.isArray(assistant.content)) {
        throw new Error("Resumed Anthropic request has no assistant block array");
    }
    return assistant.content;
}

describe("Anthropic replay through SDK sessions", async () => {
    const { createClient } = await createSdkTestContext();
    const thinkingOne = {
        type: "thinking",
        thinking: "First thought.",
        signature: "sig-sdk-one",
    } as const;
    const thinkingTwo = {
        type: "thinking",
        thinking: "Second thought.",
        signature: "sig-sdk-two",
    } as const;
    const tool = { type: "tool_use", id: TOOL_ID, name: "sql", input: TOOL_INPUT } as const;

    it.each([
        [
            "interleaved",
            [
                thinkingOne,
                { type: "text", text: "First narration." },
                thinkingTwo,
                { type: "text", text: "Second narration." },
                tool,
            ],
        ],
        ["adjacent legacy", [thinkingOne, thinkingTwo, tool]],
    ] as const)("preserves %s thinking blocks across a cold SDK resume", async (_name, blocks) => {
        const handler = new AnthropicReplayHandler([...blocks]);
        const provider = {
            type: "anthropic" as const,
            baseUrl: "https://anthropic-replay.invalid/v1",
            apiKey: "test-provider-key",
            modelId: MODEL,
            wireModel: MODEL,
        };
        const firstClient = createClient({ requestHandler: handler });
        let sessionId: string;
        try {
            const session = await firstClient.createSession({
                onPermissionRequest: approveAll,
                model: MODEL,
                provider,
            });
            sessionId = session.sessionId;
            const result = await session.sendAndWait({ prompt: FIRST_PROMPT });
            expect(result?.data.content).toContain("SDK_REPLAY_TOOL_DONE");
            await session.disconnect();
        } finally {
            await firstClient.stop();
        }

        const resumedClient = createClient({ requestHandler: handler });
        try {
            const resumed = await resumedClient.resumeSession(sessionId, {
                onPermissionRequest: approveAll,
                model: MODEL,
                provider,
            });
            const result = await resumed.sendAndWait({ prompt: FOLLOWUP_PROMPT });
            expect(result?.data.content).toContain("SDK_REPLAY_FOLLOWUP_DONE");
            const followup = handler.requests.findLast((request) =>
                JSON.stringify(request.messages).includes(FOLLOWUP_PROMPT)
            );
            expect(replayedAssistantContent(followup)).toEqual(blocks);
            await resumed.disconnect();
        } finally {
            await resumedClient.stop();
        }
    });
});
