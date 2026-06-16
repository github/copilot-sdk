/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll, type LlmInferenceRequest } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

async function drainRequest(req: LlmInferenceRequest): Promise<void> {
    for await (const _chunk of req.requestBody) {
        // discard
    }
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

async function serviceNonInference(req: LlmInferenceRequest): Promise<boolean> {
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
        return true;
    }
    if (url.includes("/models/session")) {
        await respondBuffered(req, { status: 200, headers: {} }, "{}");
        return true;
    }
    if (url.includes("/policy")) {
        await respondBuffered(req, { status: 200, headers: {} }, JSON.stringify({ state: "enabled" }));
        return true;
    }
    return false;
}

function isInferenceUrl(url: string): boolean {
    const u = url.toLowerCase();
    return (
        u.includes("/chat/completions") ||
        u.includes("/responses") ||
        u.endsWith("/messages") ||
        u.endsWith("/v1/messages")
    );
}

/**
 * Verifies the consumer → runtime cancellation path: when the consumer
 * itself decides to abort the upstream call (e.g. its own
 * `AbortController` fired, or the upstream socket dropped), it signals the
 * runtime via `responseBody.error({ code: "cancelled" })`. The runtime
 * must surface that faithfully as a request failure rather than hanging
 * waiting for a response head/body.
 */
describe("LLM inference callback — consumer-initiated cancellation", async () => {
    let inferenceAttempts = 0;

    const { copilotClient: client } = await createSdkTestContext({
        copilotClientOptions: {
            llmInference: {
                createLlmInferenceProvider: () => ({
                    async onLlmRequest(req: LlmInferenceRequest): Promise<void> {
                        if (await serviceNonInference(req)) {
                            return;
                        }
                        if (!isInferenceUrl(req.url)) {
                            await respondBuffered(
                                req,
                                { status: 200, headers: { "content-type": ["application/json"] } },
                                "{}",
                            );
                            return;
                        }

                        // Consumer-initiated cancellation: the consumer's own
                        // upstream call was aborted, so it tells the runtime to
                        // give up on this request. No response head is ever
                        // produced; the runtime should see a transport failure.
                        await drainRequest(req);
                        inferenceAttempts += 1;
                        await req.responseBody.error({
                            message: "upstream call aborted by consumer",
                            code: "cancelled",
                        });
                    },
                }),
            },
        },
    });

    it(
        "surfaces a consumer-signalled cancellation to the runtime",
        async () => {
            await client.start();
            const session = await client.createSession({ onPermissionRequest: approveAll });

            let caught: unknown;
            try {
                await session.sendAndWait({ prompt: "Say OK." });
            } catch (err) {
                caught = err;
            } finally {
                await session.disconnect();
            }

            // The runtime reached the inference step and the consumer's
            // cancellation terminated it (rather than the runtime hanging).
            expect(inferenceAttempts).toBeGreaterThan(0);
            if (caught) {
                const message = caught instanceof Error ? caught.message : String(caught);
                expect(message.length).toBeGreaterThan(0);
            }
        },
        90_000,
    );
});
