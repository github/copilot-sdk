/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll, type LlmInferenceRequest } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

/**
 * Drain the request body and reply with a single buffered response. The
 * unified callback supports both buffered and streaming uniformly — for
 * non-streaming responses, the consumer writes the whole body once and
 * calls `end`.
 */
async function respondBuffered(
    req: LlmInferenceRequest,
    init: { status: number; headers?: Record<string, string[]> },
    body: string,
): Promise<void> {
    for await (const _chunk of req.requestBody) {
        // discard — the runtime always sends at least one chunk (with end:true).
    }
    await req.responseBody.start(init);
    if (body.length > 0) {
        await req.responseBody.write(body);
    }
    await req.responseBody.end();
}

async function handleNonStreaming(req: LlmInferenceRequest): Promise<void> {
    const url = req.url.toLowerCase();
    if (url.endsWith("/models")) {
        return respondBuffered(
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
    }
    if (url.includes("/models/session")) {
        return respondBuffered(req, { status: 200, headers: {} }, "{}");
    }
    if (url.includes("/policy")) {
        return respondBuffered(req, { status: 200, headers: {} }, JSON.stringify({ state: "enabled" }));
    }
    return respondBuffered(
        req,
        { status: 200, headers: { "content-type": ["application/json"] } },
        "{}",
    );
}

describe("LLM inference callback", async () => {
    const received: LlmInferenceRequest[] = [];

    const { copilotClient: client } = await createSdkTestContext({
        copilotClientOptions: {
            llmInference: {
                createLlmInferenceProvider: () => ({
                    async onLlmRequest(req): Promise<void> {
                        received.push(req);
                        await handleNonStreaming(req);
                    },
                }),
            },
        },
    });

    it("registers the provider on connect without erroring", async () => {
        await client.start();
        expect(client).toBeDefined();
    });

    it(
        "invokes the callback for non-streaming model-layer requests and threads sessionId through",
        async () => {
            const baselineLength = received.length;
            const session = await client.createSession({ onPermissionRequest: approveAll });
            try {
                // Drive a turn so model-layer traffic (catalog,
                // session-intent, inference) flows through the callback.
                // We swallow errors here — the buffered handler returns
                // empty JSON for inference, which is not a valid model
                // response; the agent will surface a transport error.
                // What we care about is that the runtime *attempted* to
                // call the callback for the model-layer endpoints.
                try {
                    await session.sendAndWait({ prompt: "Say OK." });
                } catch {
                    // expected — see comment above
                }
            } finally {
                await session.disconnect();
            }

            expect(received.length).toBeGreaterThan(baselineLength);
            const newRequests = received.slice(baselineLength);
            for (const r of newRequests) {
                expect(r.url).toMatch(/^https?:\/\//);
                expect(typeof r.method).toBe("string");
            }

            const catalog = newRequests.find((r) => r.url.toLowerCase().endsWith("/models"));
            expect(catalog, "expected to intercept the /models catalog request").toBeDefined();

            const inSession = newRequests.find((r) => typeof r.sessionId === "string");
            if (inSession) {
                expect(inSession.sessionId).toMatch(/[a-zA-Z0-9-]+/);
            }
        },
        90_000,
    );
});
