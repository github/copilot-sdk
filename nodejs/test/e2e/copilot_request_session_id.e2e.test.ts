/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll, CopilotRequestHandler, type CopilotRequestContext } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

const SYNTHETIC_TEXT = "OK from the synthetic stream.";

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
 * A {@link CopilotRequestHandler} that records every intercepted request
 * (url + threaded session id) and fully replaces the upstream call with a
 * fabricated, well-formed response for every model-layer endpoint, so an
 * agent turn completes entirely off-network — no upstream server and no CAPI
 * proxy acting as the inference endpoint.
 *
 * This exercises the public extension surface end to end: a consumer
 * subclasses {@link CopilotRequestHandler} and overrides {@link sendRequest}
 * to short-circuit the upstream HTTP call with any {@link Response} it likes.
 * The base adapter streams that response back to the runtime.
 */
class RecordingRequestHandler extends CopilotRequestHandler {
    readonly records: InterceptedRequest[] = [];

    protected override async sendRequest(
        request: Request,
        ctx: CopilotRequestContext
    ): Promise<Response> {
        const url = request.url;
        this.records.push({ url, sessionId: ctx.sessionId });
        const bodyText = request.body ? await request.text() : "";
        return isInferenceUrl(url)
            ? buildInferenceResponse(url, bodyText)
            : buildNonInferenceResponse(url);
    }
}

function json(body: string): Response {
    return new Response(body, {
        status: 200,
        headers: { "content-type": "application/json" },
    });
}

function sse(body: string): Response {
    return new Response(body, {
        status: 200,
        headers: { "content-type": "text/event-stream", "cache-control": "no-cache" },
    });
}

/**
 * Synthesize a well-formed inference response so the agent turn completes.
 * The runtime selects `/responses` for both the CAPI and BYOK sessions here;
 * `/chat/completions` is handled too for robustness.
 */
function buildInferenceResponse(url: string, bodyText: string): Response {
    const wantsStream = /"stream"\s*:\s*true/.test(bodyText);
    const u = url.toLowerCase();

    if (u.includes("/responses")) {
        return wantsStream ? sse(RESPONSES_STREAM_EVENTS.join("")) : json(BUFFERED_RESPONSE_JSON);
    }

    if (u.includes("/chat/completions") && wantsStream) {
        return sse(CHAT_COMPLETION_STREAM_EVENTS.join(""));
    }

    // /chat/completions non-streaming (and any other inference url) — buffered JSON.
    return json(BUFFERED_CHAT_COMPLETION_JSON);
}

/**
 * Serve the non-inference model-layer GETs/POSTs the runtime issues (catalog,
 * model session, policy). These flow through the same handler but carry no
 * session id (they happen outside an agent turn).
 */
function buildNonInferenceResponse(url: string): Response {
    const u = url.toLowerCase();
    if (u.endsWith("/models")) {
        return json(MODEL_CATALOG_JSON);
    }
    if (u.includes("/models/session")) {
        return json("{}");
    }
    if (u.includes("/policy")) {
        return json(JSON.stringify({ state: "enabled" }));
    }
    return json("{}");
}

const RESPONSES_STREAM_EVENTS: string[] = [
    `event: response.created\ndata: ${JSON.stringify({
        type: "response.created",
        response: { id: "resp_stub_1", object: "response", status: "in_progress", output: [] },
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
        },
    })}\n\n`,
];

const CHAT_COMPLETION_STREAM_EVENTS: string[] = (() => {
    const base = {
        id: "chatcmpl-stub-1",
        object: "chat.completion.chunk",
        created: 1,
        model: "claude-sonnet-4.5",
    };
    return [
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
})();

const BUFFERED_RESPONSE_JSON = JSON.stringify({
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
});

const BUFFERED_CHAT_COMPLETION_JSON = JSON.stringify({
    id: "chatcmpl-stub-1",
    object: "chat.completion",
    created: 1,
    model: "claude-sonnet-4.5",
    choices: [
        {
            index: 0,
            message: { role: "assistant", content: SYNTHETIC_TEXT },
            finish_reason: "stop",
        },
    ],
    usage: { prompt_tokens: 5, completion_tokens: 7, total_tokens: 12 },
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

/**
 * Asserts the runtime threads its session id into the request handler for
 * BOTH a CAPI session and a BYOK session. The handler alone services every
 * model-layer request — no upstream server, no CAPI proxy acting as the
 * inference endpoint — so the only source of `ctx.sessionId` is the runtime's
 * own per-client threading.
 */
describe("CopilotRequestHandler threads the runtime session id (CAPI + BYOK)", async () => {
    const handler = new RecordingRequestHandler();

    const { copilotClient: client } = await createSdkTestContext({
        copilotClientOptions: {
            requestHandler: handler,
        },
    });

    let capiSessionId: string | undefined;

    it("threads the session id into a CAPI session's inference request", async () => {
        await client.start();
        const baseline = handler.records.length;
        const session = await client.createSession({ onPermissionRequest: approveAll });
        capiSessionId = session.sessionId;
        let resultJson = "";
        try {
            const result = await session.sendAndWait({ prompt: "Say OK." });
            resultJson = JSON.stringify(result);
        } finally {
            await session.disconnect();
        }

        const inference = handler.records.slice(baseline).filter((r) => isInferenceUrl(r.url));
        expect(
            inference.length,
            "expected at least one intercepted inference request"
        ).toBeGreaterThan(0);
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
        const baseline = handler.records.length;
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

        const inference = handler.records.slice(baseline).filter((r) => isInferenceUrl(r.url));
        expect(
            inference.length,
            "expected at least one intercepted BYOK inference request"
        ).toBeGreaterThan(0);
        for (const r of inference) {
            expect(r.sessionId, "BYOK inference request must carry the runtime session id").toBe(
                byokSessionId
            );
        }

        // Session ids are per-session, so the two turns must differ — proves
        // we assert against a real, request-specific id, not a constant.
        expect(byokSessionId).not.toBe(capiSessionId);

        // Validate the final assistant response arrived (guards against truncated captures)
        expect(resultJson).toMatch(/OK from the synthetic/);
    }, 90_000);
});
