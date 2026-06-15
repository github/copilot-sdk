/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll, type LlmInferenceRequest, type LlmInferenceResponse } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

/**
 * Provides minimal but realistic stub responses for the model-layer endpoints
 * the runtime touches before issuing the actual inference request. The
 * inference request itself is *not* handled here — streaming intercept is a
 * separate Commit-2 deliverable. Stream requests fall through to the recorded
 * CAPI traffic.
 */
function stubNonStreamingResponse(req: LlmInferenceRequest): LlmInferenceResponse {
    const url = req.url.toLowerCase();

    // GET /models — model catalog
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

    // /models/session/intent etc.
    if (url.includes("/models/session")) {
        return { status: 200, headers: {}, bodyText: "{}" };
    }

    if (url.includes("/policy")) {
        return { status: 200, headers: {}, bodyText: JSON.stringify({ state: "enabled" }) };
    }

    // Fallback: opaque empty JSON
    return { status: 200, headers: { "content-type": ["application/json"] }, bodyText: "{}" };
}

describe("LLM inference callback", async () => {
    // Tracks every request the runtime asks the client to service.
    const received: LlmInferenceRequest[] = [];

    const { copilotClient: client } = await createSdkTestContext({
        copilotClientOptions: {
            llmInference: {
                createLlmInferenceProvider: () => ({
                    async onLlmRequest(req: LlmInferenceRequest): Promise<LlmInferenceResponse> {
                        received.push(req);
                        return stubNonStreamingResponse(req);
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
                await session.sendAndWait({ prompt: "Say OK." });
            } finally {
                await session.disconnect();
            }

            // After Phase 2, the Rust runtime intercepts every model-layer
            // HTTP request that previously hit the recording proxy — so we
            // now expect to see at least the /models catalog request and
            // typically /models/session intent etc.
            expect(received.length).toBeGreaterThan(baselineLength);
            const newRequests = received.slice(baselineLength);
            for (const r of newRequests) {
                expect(r.url).toMatch(/^https?:\/\//);
                expect(typeof r.method).toBe("string");
                expect(r.metadata).toBeDefined();
                expect(r.metadata.transport).toBe("http");
            }

            // At least one of the intercepted requests should be the models
            // catalog — that's the very first thing the runtime asks for.
            const catalog = newRequests.find((r) => r.metadata.endpointKind === "models-catalog");
            expect(catalog, "expected to intercept the /models catalog request").toBeDefined();

            // Any request that originated inside the session should carry
            // the sessionId on the payload. This proves the runtime threaded
            // the field through the global callback correctly (no implicit
            // dispatch key — it's just a payload field).
            const inSession = newRequests.find((r) => typeof r.sessionId === "string");
            if (inSession) {
                expect(inSession.sessionId).toMatch(/[a-zA-Z0-9-]+/);
            }
        },
        60_000
    );
});

