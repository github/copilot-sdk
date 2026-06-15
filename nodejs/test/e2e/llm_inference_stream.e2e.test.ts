/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import {
    approveAll,
    type LlmInferenceRequest,
    type LlmInferenceResponse,
    type LlmInferenceStreamSink,
    type LlmInferenceStreamStartResponse,
} from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

function stubNonStreaming(req: LlmInferenceRequest): LlmInferenceResponse {
    const url = req.url.toLowerCase();
    if (url.endsWith("/models")) {
        return {
            status: 200,
            headers: { "content-type": ["application/json"] },
            bodyText: JSON.stringify({
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
                            supports: { streaming: true, tool_calls: true, parallel_tool_calls: true, vision: true },
                        },
                    },
                ],
            }),
        };
    }
    if (url.includes("/models/session")) {
        return { status: 200, headers: {}, bodyText: "{}" };
    }
    if (url.includes("/policy")) {
        return { status: 200, headers: {}, bodyText: JSON.stringify({ state: "enabled" }) };
    }

    // Non-streaming chat completion — agent loop dispatches the inference
    // here when streaming is disabled. Return a minimal but well-formed
    // assistant response so the agent can complete the turn.
    if (url.includes("/chat/completions")) {
        return {
            status: 200,
            headers: { "content-type": ["application/json"] },
            bodyText: JSON.stringify({
                id: "chatcmpl-stub-1",
                object: "chat.completion",
                created: 1,
                model: "claude-sonnet-4.5",
                choices: [
                    {
                        index: 0,
                        message: {
                            role: "assistant",
                            content: "OK from the synthetic callback.",
                        },
                        finish_reason: "stop",
                    },
                ],
                usage: { prompt_tokens: 5, completion_tokens: 7, total_tokens: 12 },
            }),
        };
    }

    return { status: 200, headers: { "content-type": ["application/json"] }, bodyText: "{}" };
}

/**
 * Synthesizes a minimal but well-formed streaming response for the runtime's
 * streaming inference request. Emits SSE chunks for either the OpenAI
 * chat-completions or responses-API wire format depending on what the
 * runtime picks for this model.
 */
async function handleStreamRequest(
    req: LlmInferenceRequest,
    sink: LlmInferenceStreamSink,
): Promise<LlmInferenceStreamStartResponse> {
    const url = req.url.toLowerCase();
    const isResponsesApi = req.metadata.wireApi === "responses" || url.includes("/responses");

    queueMicrotask(async () => {
        try {
            const encoder = new TextEncoder();
            const send = (text: string) => sink.pushChunk(encoder.encode(text));

            if (isResponsesApi) {
                const id = "resp_stub_1";
                await send(
                    `event: response.created\n` +
                        `data: ${JSON.stringify({ type: "response.created", response: { id, object: "response", status: "in_progress", output: [] } })}\n\n`,
                );
                await send(
                    `event: response.output_item.added\n` +
                        `data: ${JSON.stringify({ type: "response.output_item.added", output_index: 0, item: { id: "msg_1", type: "message", role: "assistant", content: [] } })}\n\n`,
                );
                await send(
                    `event: response.content_part.added\n` +
                        `data: ${JSON.stringify({ type: "response.content_part.added", output_index: 0, content_index: 0, part: { type: "output_text", text: "" } })}\n\n`,
                );
                await send(
                    `event: response.output_text.delta\n` +
                        `data: ${JSON.stringify({ type: "response.output_text.delta", output_index: 0, content_index: 0, delta: "OK from the synthetic stream." })}\n\n`,
                );
                await send(
                    `event: response.output_text.done\n` +
                        `data: ${JSON.stringify({ type: "response.output_text.done", output_index: 0, content_index: 0, text: "OK from the synthetic stream." })}\n\n`,
                );
                await send(
                    `event: response.completed\n` +
                        `data: ${JSON.stringify({
                            type: "response.completed",
                            response: {
                                id,
                                object: "response",
                                status: "completed",
                                output: [
                                    {
                                        id: "msg_1",
                                        type: "message",
                                        role: "assistant",
                                        content: [{ type: "output_text", text: "OK from the synthetic stream." }],
                                    },
                                ],
                                usage: { input_tokens: 5, output_tokens: 7, total_tokens: 12 },
                            },
                        })}\n\n`,
                );
            } else {
                const base = {
                    id: "chatcmpl-stub-1",
                    object: "chat.completion.chunk",
                    created: 1,
                    model: "claude-sonnet-4.5",
                };
                await send(
                    `data: ${JSON.stringify({
                        ...base,
                        choices: [{ index: 0, delta: { role: "assistant", content: "" }, finish_reason: null }],
                    })}\n\n`,
                );
                await send(
                    `data: ${JSON.stringify({
                        ...base,
                        choices: [
                            {
                                index: 0,
                                delta: { content: "OK from the synthetic stream." },
                                finish_reason: null,
                            },
                        ],
                    })}\n\n`,
                );
                await send(
                    `data: ${JSON.stringify({
                        ...base,
                        choices: [{ index: 0, delta: {}, finish_reason: "stop" }],
                        usage: { prompt_tokens: 5, completion_tokens: 7, total_tokens: 12 },
                    })}\n\n`,
                );
                await send(`data: [DONE]\n\n`);
            }
            await sink.end();
        } catch (err) {
            const message = err instanceof Error ? err.message : String(err);
            await sink.end(message);
        }
    });

    return {
        status: 200,
        headers: { "content-type": ["text/event-stream"] },
    };
}

describe("LLM inference callback — fully mocked streaming", async () => {
    const received: LlmInferenceRequest[] = [];
    const streamed: LlmInferenceRequest[] = [];

    const { copilotClient: client } = await createSdkTestContext({
        copilotClientOptions: {
            llmInference: {
                createLlmInferenceProvider: () => ({
                    async onLlmRequest(req: LlmInferenceRequest): Promise<LlmInferenceResponse> {
                        received.push(req);
                        return stubNonStreaming(req);
                    },
                    async onLlmStreamRequest(req, sink) {
                        streamed.push(req);
                        return handleStreamRequest(req, sink);
                    },
                }),
            },
        },
    });

    it(
        "completes a full user→assistant turn entirely via the callback",
        async () => {
            await client.start();
            const session = await client.createSession({ onPermissionRequest: approveAll });
            let resultJson = "";
            try {
                const result = await session.sendAndWait({ prompt: "Say OK." });
                resultJson = JSON.stringify(result);
            } finally {
                await session.disconnect();
            }

            // The runtime intercepted at least one inference request — by
            // either the streaming or non-streaming codepath depending on
            // which the agent chose.
            const inferenceReqs = [...streamed, ...received].filter(
                (r) => r.metadata.endpointKind === "inference",
            );
            expect(inferenceReqs.length, "expected at least one inference request via the callback").toBeGreaterThan(
                0,
            );
            for (const r of inferenceReqs) {
                expect(r.metadata.transport).toBe("http");
            }

            // The synthetic content surfaced in the assistant response.
            expect(resultJson).toMatch(/OK from the synthetic/);
        },
        90_000,
    );
});
