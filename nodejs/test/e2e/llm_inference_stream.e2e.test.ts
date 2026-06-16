/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll, LlmRequestHandler, type LlmInferenceRequest } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

async function drainRequest(req: LlmInferenceRequest): Promise<string> {
    const parts: Buffer[] = [];
    for await (const chunk of req.requestBody) {
        parts.push(Buffer.from(chunk));
    }
    return Buffer.concat(parts).toString("utf-8");
}

async function respondBuffered(
    req: LlmInferenceRequest,
    init: { status: number; headers?: Record<string, string[]> },
    body: string,
): Promise<void> {
    await drainRequest(req);
    await req.responseBody.start(init);
    if (body.length > 0) {
        await req.responseBody.write(body);
    }
    await req.responseBody.end();
}

async function handleNonInferenceModelTraffic(req: LlmInferenceRequest): Promise<void> {
    const url = req.url.toLowerCase();
    if (url.endsWith("/models")) {
        await respondBuffered(
            req,
            { status: 200, headers: { "content-type": ["application/json"] } },
            JSON.stringify({
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
        );
        return;
    }
    if (url.includes("/models/session")) {
        await respondBuffered(req, { status: 200, headers: {} }, "{}");
        return;
    }
    if (url.includes("/policy")) {
        await respondBuffered(req, { status: 200, headers: {} }, JSON.stringify({ state: "enabled" }));
        return;
    }
    await respondBuffered(req, { status: 200, headers: { "content-type": ["application/json"] } }, "{}");
}

/**
 * Synthesizes a minimal but well-formed response for the runtime's
 * inference request. The runtime calls the buffered code path for
 * `/chat/completions` and the streaming code path for `/responses`, but
 * the unified callback has no field telling the consumer which — the
 * consumer dispatches by URL.
 */
async function handleInference(req: LlmInferenceRequest): Promise<void> {
    const bodyText = await drainRequest(req);
    const wantsStream = /"stream"\s*:\s*true/.test(bodyText);
    const url = req.url.toLowerCase();

    if (url.includes("/responses")) {
        await req.responseBody.start({
            status: 200,
            headers: { "content-type": ["text/event-stream"] },
        });
        const id = "resp_stub_1";
        const events: string[] = [
            `event: response.created\ndata: ${JSON.stringify({
                type: "response.created",
                response: { id, object: "response", status: "in_progress", output: [] },
            })}\n\n`,
            `event: response.output_item.added\ndata: ${JSON.stringify({
                type: "response.output_item.added",
                output_index: 0,
                item: { id: "msg_1", type: "message", role: "assistant", content: [] },
            })}\n\n`,
            `event: response.content_part.added\ndata: ${JSON.stringify({
                type: "response.content_part.added",
                output_index: 0,
                content_index: 0,
                part: { type: "output_text", text: "" },
            })}\n\n`,
            `event: response.output_text.delta\ndata: ${JSON.stringify({
                type: "response.output_text.delta",
                output_index: 0,
                content_index: 0,
                delta: "OK from the synthetic stream.",
            })}\n\n`,
            `event: response.output_text.done\ndata: ${JSON.stringify({
                type: "response.output_text.done",
                output_index: 0,
                content_index: 0,
                text: "OK from the synthetic stream.",
            })}\n\n`,
            `event: response.completed\ndata: ${JSON.stringify({
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
        ];
        for (const event of events) {
            await req.responseBody.write(event);
        }
        await req.responseBody.end();
        return;
    }

    if (url.includes("/chat/completions") && wantsStream) {
        await req.responseBody.start({
            status: 200,
            headers: { "content-type": ["text/event-stream"] },
        });
        const base = {
            id: "chatcmpl-stub-1",
            object: "chat.completion.chunk",
            created: 1,
            model: "claude-sonnet-4.5",
        };
        const events: string[] = [
            `data: ${JSON.stringify({
                ...base,
                choices: [{ index: 0, delta: { role: "assistant", content: "" }, finish_reason: null }],
            })}\n\n`,
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
            `data: ${JSON.stringify({
                ...base,
                choices: [{ index: 0, delta: {}, finish_reason: "stop" }],
                usage: { prompt_tokens: 5, completion_tokens: 7, total_tokens: 12 },
            })}\n\n`,
            `data: [DONE]\n\n`,
        ];
        for (const event of events) {
            await req.responseBody.write(event);
        }
        await req.responseBody.end();
        return;
    }

    // /chat/completions non-streaming — buffered JSON. (body already drained above)
    await req.responseBody.start({ status: 200, headers: { "content-type": ["application/json"] } });
    await req.responseBody.write(
        JSON.stringify({
            id: "chatcmpl-stub-1",
            object: "chat.completion",
            created: 1,
            model: "claude-sonnet-4.5",
            choices: [
                {
                    index: 0,
                    message: { role: "assistant", content: "OK from the synthetic stream." },
                    finish_reason: "stop",
                },
            ],
            usage: { prompt_tokens: 5, completion_tokens: 7, total_tokens: 12 },
        }),
    );
    await req.responseBody.end();
}

describe("LLM inference callback — fully mocked streaming", async () => {
    const received: LlmInferenceRequest[] = [];

    const { copilotClient: client } = await createSdkTestContext({
        copilotClientOptions: {
            llmInference: {
                handler: new (class extends LlmRequestHandler {
                    override async onLlmRequest(req: LlmInferenceRequest): Promise<void> {
                        received.push(req);
                        const url = req.url.toLowerCase();
                        const isInference =
                            url.includes("/chat/completions") ||
                            url.endsWith("/responses") ||
                            url.endsWith("/v1/messages") ||
                            url.endsWith("/messages");
                        if (isInference) {
                            await handleInference(req);
                        } else {
                            await handleNonInferenceModelTraffic(req);
                        }
                    }
                })(),
            },
        },
    });

    it(
        "completes a full user→assistant turn entirely via the callback (chunked SSE response)",
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

            // At least one inference request flowed through the callback.
            const inferenceReqs = received.filter((r) => {
                const u = r.url.toLowerCase();
                return (
                    u.endsWith("/chat/completions") ||
                    u.endsWith("/responses") ||
                    u.endsWith("/v1/messages") ||
                    u.endsWith("/messages")
                );
            });
            expect(inferenceReqs.length, "expected at least one inference request via the callback").toBeGreaterThan(
                0,
            );

            // The synthetic content surfaced in the assistant response.
            expect(resultJson).toMatch(/OK from the synthetic/);
        },
        90_000,
    );
});
