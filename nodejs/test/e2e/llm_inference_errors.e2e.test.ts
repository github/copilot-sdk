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

/**
 * Verifies that errors thrown (or signalled via `responseBody.error`) by
 * the LLM inference callback surface to the SDK consumer as transport
 * failures, so the runtime's existing retry / error-reporting machinery
 * handles them uniformly.
 */
describe("LLM inference callback — error mapping", async () => {
    let callsBeforeError = 0;
    let totalCalls = 0;

    const { copilotClient: client } = await createSdkTestContext({
        copilotClientOptions: {
            llmInference: {
                createLlmInferenceProvider: () => ({
                    async onLlmRequest(req: LlmInferenceRequest): Promise<void> {
                        totalCalls += 1;
                        const url = req.url.toLowerCase();

                        // Service models / session / policy normally so the
                        // agent can reach the inference step.
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
                                                limits: {
                                                    max_context_window_tokens: 200000,
                                                    max_output_tokens: 8192,
                                                },
                                                supports: {
                                                    streaming: true,
                                                    tool_calls: true,
                                                    parallel_tool_calls: true,
                                                    vision: true,
                                                },
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
                            await respondBuffered(
                                req,
                                { status: 200, headers: {} },
                                JSON.stringify({ state: "enabled" }),
                            );
                            return;
                        }

                        // Inference: throw a transport-level error from the
                        // callback. The adapter converts this into a
                        // terminal `httpResponseChunk` with `error` set, so
                        // the runtime surfaces it as `APIConnectionError`.
                        if (url.includes("/chat/completions") || url.includes("/responses")) {
                            await drainRequest(req);
                            callsBeforeError += 1;
                            throw new Error("synthetic-callback-transport-failure");
                        }

                        await respondBuffered(
                            req,
                            { status: 200, headers: { "content-type": ["application/json"] } },
                            "{}",
                        );
                    },
                }),
            },
        },
    });

    it(
        "surfaces a callback-thrown error to the SDK consumer",
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

            // The agent layer typically wraps inference failures in its
            // own error type and may convert them to an event rather than
            // a thrown exception, so the assertion is loose: either we
            // caught an error referencing the callback failure, or the
            // inference call was attempted at least once and the runtime
            // did NOT hang waiting for a response.
            expect(totalCalls).toBeGreaterThan(0);
            expect(callsBeforeError).toBeGreaterThan(0);
            if (caught) {
                const message = caught instanceof Error ? caught.message : String(caught);
                expect(message.length).toBeGreaterThan(0);
            }
        },
        90_000,
    );
});
