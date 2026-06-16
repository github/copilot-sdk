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

async function waitFor(predicate: () => boolean, timeoutMs: number): Promise<void> {
    const start = Date.now();
    while (!predicate()) {
        if (Date.now() - start > timeoutMs) {
            throw new Error("waitFor timed out");
        }
        await new Promise((resolve) => setTimeout(resolve, 50));
    }
}

/**
 * Verifies the runtime → consumer cancellation path: when an in-flight
 * turn is aborted via `session.abort()`, the runtime cancels the
 * callback-served inference request and the consumer observes
 * `req.signal.aborted` so it can tear down its upstream call.
 */
describe("LLM inference callback — cancellation", async () => {
    let inferenceEntered = false;
    let sawAbort = false;
    let resolveAbortSeen: (() => void) | undefined;
    const abortSeen = new Promise<void>((resolve) => {
        resolveAbortSeen = resolve;
    });

    const { copilotClient: client } = await createSdkTestContext({
        copilotClientOptions: {
            llmInference: {
                createLlmInferenceProvider: () => ({
                    async onLlmRequest(req: LlmInferenceRequest): Promise<void> {
                        if (await serviceNonInference(req)) {
                            return;
                        }
                        const url = req.url.toLowerCase();
                        const isInference =
                            url.includes("/chat/completions") ||
                            url.includes("/responses") ||
                            url.endsWith("/messages") ||
                            url.endsWith("/v1/messages");
                        if (!isInference) {
                            await respondBuffered(
                                req,
                                { status: 200, headers: { "content-type": ["application/json"] } },
                                "{}",
                            );
                            return;
                        }

                        // Inference: never produce a response. Wait for the
                        // runtime to cancel us, recording the abort.
                        await drainRequest(req);
                        inferenceEntered = true;
                        await new Promise<void>((resolve) => {
                            if (req.signal.aborted) {
                                resolve();
                                return;
                            }
                            req.signal.addEventListener("abort", () => resolve(), { once: true });
                        });
                        sawAbort = true;
                        resolveAbortSeen?.();
                        try {
                            await req.responseBody.error({ message: "cancelled by upstream", code: "cancelled" });
                        } catch {
                            // Runtime already dropped the request on cancel.
                        }
                    },
                }),
            },
        },
    });

    it(
        "propagates runtime cancellation to the consumer's req.signal",
        async () => {
            await client.start();
            const session = await client.createSession({ onPermissionRequest: approveAll });
            try {
                await session.send({ prompt: "Say OK." });
                await waitFor(() => inferenceEntered, 60_000);
                await session.abort();
                await Promise.race([
                    abortSeen,
                    new Promise((_resolve, reject) =>
                        setTimeout(() => reject(new Error("timed out waiting for abort")), 30_000),
                    ),
                ]);
            } finally {
                await session.disconnect();
            }

            // The consumer observed the runtime-driven cancellation.
            expect(inferenceEntered).toBe(true);
            expect(sawAbort).toBe(true);
        },
        120_000,
    );
});
