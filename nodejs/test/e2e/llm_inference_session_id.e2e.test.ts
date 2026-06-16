/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll, LlmRequestHandler, type LlmInferenceRequest } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

const SYNTHETIC_TEXT = "OK from the synthetic stream.";

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
    body: string
): Promise<void> {
    await drainRequest(req);
    await req.responseBody.start(init);
    if (body.length > 0) {
        await req.responseBody.write(body);
    }
    await req.responseBody.end();
}

/**
 * Serve the model-layer GETs/POSTs the runtime issues that are not
 * inference (catalog, model session, policy). These flow through the same
 * callback but carry no session id (they happen outside an agent turn).
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
                        capabilities: {
                            type: "chat",
                            family: "claude-sonnet-4.5",
                            tokenizer: "o200k_base",
                            limits: { max_context_window_tokens: 200000, max_output_tokens: 8192 },
                            supports: {
                                streaming: true,
                                tool_calls: true,
                                parallel_tool_calls: true,
                                vision: true,
                            },
                        },
                    },
                ],
            })
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
 * Synthesize a well-formed inference response so the agent turn completes.
 * The runtime selects `/responses` for both the CAPI and BYOK sessions
 * here; `/chat/completions` is handled too for robustness. The consumer
 * fabricates the response directly — there is no upstream server and the
 * CAPI record/replay proxy is never the inference endpoint.
 */
async function handleInference(req: LlmInferenceRequest): Promise<void> {
    const bodyText = await drainRequest(req);
    const wantsStream = /"stream"\s*:\s*true/.test(bodyText);
    const url = req.url.toLowerCase();

    // `/responses` streams via SSE only when the request asked for it
    // (`stream: true`). BYOK turns whose config-derived model doesn't
    // advertise streaming issue a buffered request expecting a single
    // JSON `response` object, so branch on the flag exactly as a real
    // upstream would.
    if (url.includes("/responses")) {
        if (!wantsStream) {
            await req.responseBody.start({
                status: 200,
                headers: { "content-type": ["application/json"] },
            });
            await req.responseBody.write(
                JSON.stringify({
                    id: "resp_stub_1",
                    object: "response",
                    status: "completed",
                    output: [
                        {
                            id: "msg_1",
                            type: "message",
                            role: "assistant",
                            content: [{ type: "output_text", text: SYNTHETIC_TEXT }],
                        },
                    ],
                    usage: { input_tokens: 5, output_tokens: 7, total_tokens: 12 },
                })
            );
            await req.responseBody.end();
            return;
        }
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
                delta: SYNTHETIC_TEXT,
            })}\n\n`,
            `event: response.output_text.done\ndata: ${JSON.stringify({
                type: "response.output_text.done",
                output_index: 0,
                content_index: 0,
                text: SYNTHETIC_TEXT,
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
                            content: [{ type: "output_text", text: SYNTHETIC_TEXT }],
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
        const base = { id: "chatcmpl-stub-1", object: "chat.completion.chunk", created: 1, model: "claude-sonnet-4.5" };
        const events: string[] = [
            `data: ${JSON.stringify({
                ...base,
                choices: [{ index: 0, delta: { role: "assistant", content: "" }, finish_reason: null }],
            })}\n\n`,
            `data: ${JSON.stringify({
                ...base,
                choices: [{ index: 0, delta: { content: SYNTHETIC_TEXT }, finish_reason: null }],
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

    // /chat/completions non-streaming — buffered JSON.
    await req.responseBody.start({ status: 200, headers: { "content-type": ["application/json"] } });
    await req.responseBody.write(
        JSON.stringify({
            id: "chatcmpl-stub-1",
            object: "chat.completion",
            created: 1,
            model: "claude-sonnet-4.5",
            choices: [
                { index: 0, message: { role: "assistant", content: SYNTHETIC_TEXT }, finish_reason: "stop" },
            ],
            usage: { prompt_tokens: 5, completion_tokens: 7, total_tokens: 12 },
        })
    );
    await req.responseBody.end();
}

interface InterceptedRequest {
    url: string;
    sessionId?: string;
}

function isInferenceUrl(url: string): boolean {
    const u = url.toLowerCase();
    return (
        u.endsWith("/chat/completions") ||
        u.endsWith("/responses") ||
        u.endsWith("/v1/messages") ||
        u.endsWith("/messages")
    );
}

/**
 * Asserts the runtime threads its session id into the LLM inference
 * callback for BOTH a CAPI session and a BYOK session. The callback alone
 * services every model-layer request — no upstream server, no CAPI proxy
 * acting as the inference endpoint — so the only source of `req.sessionId`
 * is the runtime's own per-client threading.
 */
describe("LLM inference callback threads the runtime session id (CAPI + BYOK)", async () => {
    const records: InterceptedRequest[] = [];

    const { copilotClient: client } = await createSdkTestContext({
        copilotClientOptions: {
            llmInference: {
                handler: new (class extends LlmRequestHandler {
                    override async onLlmRequest(req: LlmInferenceRequest): Promise<void> {
                        records.push({ url: req.url, sessionId: req.sessionId });
                        if (isInferenceUrl(req.url)) {
                            await handleInference(req);
                        } else {
                            await handleNonInferenceModelTraffic(req);
                        }
                    }
                })(),
            },
        },
    });

    let capiSessionId: string | undefined;

    it("threads the session id into a CAPI session's inference request", async () => {
        await client.start();
        const baseline = records.length;
        const session = await client.createSession({ onPermissionRequest: approveAll });
        capiSessionId = session.sessionId;
        let resultJson = "";
        try {
            const result = await session.sendAndWait({ prompt: "Say OK." });
            resultJson = JSON.stringify(result);
        } finally {
            await session.disconnect();
        }

        const inference = records.slice(baseline).filter((r) => isInferenceUrl(r.url));
        expect(inference.length, "expected at least one intercepted inference request").toBeGreaterThan(0);
        for (const r of inference) {
            expect(r.sessionId, "CAPI inference request must carry the runtime session id").toBe(
                session.sessionId
            );
        }

        // Validate the final assistant response arrived (guards against truncated captures)
        expect(resultJson).toMatch(/OK from the synthetic/);
    }, 90_000);

    it("threads the session id into a BYOK session's inference request", async () => {
        await client.start();
        const baseline = records.length;
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            // BYOK providers require an explicit model id.
            model: "claude-sonnet-4.5",
            provider: {
                type: "openai",
                wireApi: "responses",
                baseUrl: "https://byok.invalid/v1",
                apiKey: "byok-secret",
                modelId: "claude-sonnet-4.5",
                wireModel: "claude-sonnet-4.5",
            },
        });
        const byokSessionId = session.sessionId;
        let resultJson = "";
        try {
            const result = await session.sendAndWait({ prompt: "Say OK." });
            resultJson = JSON.stringify(result);
        } finally {
            await session.disconnect();
        }

        const inference = records.slice(baseline).filter((r) => isInferenceUrl(r.url));
        expect(inference.length, "expected at least one intercepted BYOK inference request").toBeGreaterThan(0);
        for (const r of inference) {
            expect(r.sessionId, "BYOK inference request must carry the runtime session id").toBe(byokSessionId);
        }

        // Session ids are per-session, so the two turns must differ — proves
        // we assert against a real, request-specific id, not a constant.
        expect(byokSessionId).not.toBe(capiSessionId);

        // Validate the final assistant response arrived (guards against truncated captures)
        expect(resultJson).toMatch(/OK from the synthetic/);
    }, 90_000);
});
