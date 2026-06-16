/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll, type LlmInferenceRequest } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

const WS_TEXT = "OK from the synthetic ws.";

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

/**
 * The fake model catalog advertises both `/responses` and `ws:/responses`
 * so `pickModelProtocol` selects the Responses wire API and `ai-client.ts`
 * is allowed to pick the WebSocket transport (the feature flag is enabled
 * via the env var below). No `/v1/messages`, otherwise the model would be
 * routed to the Anthropic Messages API instead.
 */
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
                        supported_endpoints: ["/responses", "ws:/responses"],
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
 * Synthesizes the `/responses` SSE event stream for the HTTP code path
 * (single-shot inference requests — e.g. title generation — that don't
 * pick the WebSocket transport).
 */
async function handleHttpInference(req: LlmInferenceRequest): Promise<void> {
    await drainRequest(req);
    await req.responseBody.start({
        status: 200,
        headers: { "content-type": ["text/event-stream"] },
    });
    for (const event of buildResponsesEvents()) {
        await req.responseBody.write(`event: ${event.type}\ndata: ${JSON.stringify(event)}\n\n`);
    }
    await req.responseBody.end();
}

/**
 * Builds the ordered `/responses` event objects the reducer expects.
 * Used raw (one object = one WS message) for the WebSocket path and
 * SSE-framed for the HTTP path.
 */
function buildResponsesEvents(): Array<Record<string, unknown>> {
    const id = "resp_stub_ws_1";
    return [
        { type: "response.created", response: { id, object: "response", status: "in_progress", output: [] } },
        {
            type: "response.output_item.added",
            output_index: 0,
            item: { id: "msg_1", type: "message", role: "assistant", content: [] },
        },
        {
            type: "response.content_part.added",
            output_index: 0,
            content_index: 0,
            part: { type: "output_text", text: "" },
        },
        { type: "response.output_text.delta", output_index: 0, content_index: 0, delta: WS_TEXT },
        { type: "response.output_text.done", output_index: 0, content_index: 0, text: WS_TEXT },
        {
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
                        content: [{ type: "output_text", text: WS_TEXT }],
                    },
                ],
                usage: { input_tokens: 5, output_tokens: 7, total_tokens: 12 },
            },
        },
    ];
}

/**
 * Full-duplex WebSocket handler. The runtime opens the channel
 * (`transport === "websocket"`), the consumer acks the upgrade, then
 * pumps bidirectionally: every inbound `response.create` request the
 * runtime sends is answered with the ordered `/responses` event objects,
 * one event per outbound WS message (raw JSON, *not* SSE-framed). The
 * connection is reused across turns; it stays open until the runtime
 * closes it, at which point `req.requestBody` throws and we stop.
 */
async function handleWebSocket(req: LlmInferenceRequest, onRequest: () => void): Promise<void> {
    // Ack the upgrade (status 101-equivalent) before any message flows.
    await req.responseBody.start({ status: 101, headers: {} });
    try {
        for await (const _outbound of req.requestBody) {
            onRequest();
            for (const event of buildResponsesEvents()) {
                await req.responseBody.write(JSON.stringify(event));
            }
        }
    } catch {
        // Expected: the runtime cancels the request body when it closes the
        // socket at session teardown. Nothing more to do.
    }
}

describe("LLM inference callback — full-duplex WebSocket transport", async () => {
    const received: LlmInferenceRequest[] = [];
    let wsRequestCount = 0;

    const { copilotClient: client, env } = await createSdkTestContext({
        copilotClientOptions: {
            llmInference: {
                createLlmInferenceProvider: () => ({
                    async onLlmRequest(req: LlmInferenceRequest): Promise<void> {
                        received.push(req);
                        if (req.transport === "websocket") {
                            await handleWebSocket(req, () => {
                                wsRequestCount++;
                            });
                            return;
                        }
                        const url = req.url.toLowerCase();
                        const isInference =
                            url.includes("/chat/completions") ||
                            url.endsWith("/responses") ||
                            url.endsWith("/v1/messages") ||
                            url.endsWith("/messages");
                        if (isInference) {
                            await handleHttpInference(req);
                        } else {
                            await handleNonInferenceModelTraffic(req);
                        }
                    },
                }),
            },
        },
    });

    // Enable the WebSocket Responses transport in the spawned runtime. The
    // harness env object is the same one passed to the CLI subprocess, so
    // mutating it here flips the ExP flag for this test file's client.
    env.COPILOT_EXP_COPILOT_CLI_WEBSOCKET_RESPONSES = "true";

    it(
        "completes a user→assistant turn over the WebSocket transport via the callback",
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

            // The main agent turn (tools present, not single-shot) selected the
            // WebSocket transport and drove it through the callback.
            const wsReqs = received.filter((r) => r.transport === "websocket");
            expect(wsReqs.length, "expected at least one websocket request via the callback").toBeGreaterThan(0);
            expect(wsRequestCount, "expected the runtime to send at least one ws message").toBeGreaterThan(0);

            // The synthetic content surfaced in the assistant response.
            expect(resultJson).toMatch(/OK from the synthetic ws/);
        },
        90_000,
    );
});
