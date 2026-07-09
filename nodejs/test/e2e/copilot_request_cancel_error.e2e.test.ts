/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll, CopilotRequestHandler, type CopilotRequestContext } from "../../src/index.js";
import { createSdkTestContext, isInProcessTransport } from "./harness/sdkTestContext.js";

/**
 * Cancellation and error coverage for {@link CopilotRequestHandler}. These two
 * scenarios exercise the handler's terminal paths that the happy-path session-id
 * and HTTP/WebSocket tests never reach:
 *
 * - **Error** — the handler throws from {@link CopilotRequestHandler.sendRequest}
 *   for an inference request. The base adapter reports a transport error back to
 *   the runtime (`errorResponse`) rather than hanging.
 * - **Runtime cancel** — the handler blocks an inference request indefinitely;
 *   when the consumer aborts the turn the runtime cancels the in-flight request,
 *   firing `ctx.signal`. The handler observes the abort (the `cancel`-frame
 *   path) instead of leaking a stuck request.
 *
 * Non-inference model-layer requests (catalog, policy, model session) are served
 * with minimal stubs so the turn reaches the inference step. The success-path
 * SSE body is intentionally omitted — neither scenario completes a turn.
 */

function isInferenceUrl(url: string): boolean {
    const u = url.toLowerCase();
    return (
        u.endsWith("/chat/completions") ||
        u.endsWith("/responses") ||
        u.endsWith("/v1/messages") ||
        u.endsWith("/messages")
    );
}

function json(body: string): Response {
    return new Response(body, { status: 200, headers: { "content-type": "application/json" } });
}

/** Serve the non-inference GETs/POSTs (catalog, policy, model session). */
function serveNonInference(url: string): Response {
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

async function waitFor(predicate: () => boolean, timeoutMs: number): Promise<void> {
    const deadline = Date.now() + timeoutMs;
    while (!predicate()) {
        if (Date.now() > deadline) {
            throw new Error("waitFor timed out");
        }
        await new Promise((resolve) => setTimeout(resolve, 50));
    }
}

/** Throws from every inference request to exercise the error-reporting path. */
class ThrowingRequestHandler extends CopilotRequestHandler {
    inferenceAttempts = 0;

    protected override async sendRequest(
        request: Request,
        _ctx: CopilotRequestContext
    ): Promise<Response> {
        if (!isInferenceUrl(request.url)) {
            return serveNonInference(request.url);
        }
        this.inferenceAttempts++;
        throw new Error("synthetic-callback-transport-failure");
    }
}

/** Blocks every inference request until the runtime cancels it. */
class CancellingRequestHandler extends CopilotRequestHandler {
    inferenceEntered = false;
    sawAbort = false;

    protected override async sendRequest(
        request: Request,
        ctx: CopilotRequestContext
    ): Promise<Response> {
        if (!isInferenceUrl(request.url)) {
            return serveNonInference(request.url);
        }
        this.inferenceEntered = true;
        await new Promise<void>((resolve) => {
            if (ctx.signal.aborted) {
                resolve();
                return;
            }
            ctx.signal.addEventListener("abort", () => resolve(), { once: true });
        });
        this.sawAbort = true;
        // The runtime already dropped the request; throwing simply propagates
        // the abort out of the (here, simulated) upstream call.
        throw new Error("cancelled by runtime");
    }
}

describe("CopilotRequestHandler surfaces inference errors", async () => {
    const handler = new ThrowingRequestHandler();
    const { copilotClient: client } = await createSdkTestContext({
        copilotClientOptions: { requestHandler: handler },
    });

    it("reports a thrown callback error instead of hanging the turn", async () => {
        await client.start();
        const session = await client.createSession({ onPermissionRequest: approveAll });
        try {
            // The callback throws on inference; the turn surfaces an error (or
            // completes without an assistant message) rather than hanging.
            await session.sendAndWait({ prompt: "Say OK." }).catch(() => undefined);
        } finally {
            await session.disconnect();
        }

        expect(
            handler.inferenceAttempts,
            "expected the inference callback to be reached and raise"
        ).toBeGreaterThan(0);
    }, 90_000);
});

describe("CopilotRequestHandler observes runtime cancellation", async () => {
    const handler = new CancellingRequestHandler();
    const { copilotClient: client } = await createSdkTestContext({
        copilotClientOptions: { requestHandler: handler },
    });

    // The runtime enforces a single, process-wide LLM inference provider: a second
    // client.start() with a requestHandler rejects llmInference.setProvider with
    // "Another client is already the LLM inference provider." The sibling error test
    // above already registers a provider and holds it for this file's lifetime, and
    // inproc runs share one runtime host, so this scenario can only run on the default
    // (stdio) cell, where each client owns its own runtime process.
    it.skipIf(isInProcessTransport)(
        "fires ctx.signal when the consumer aborts an in-flight inference request",
        async () => {
            await client.start();
            const session = await client.createSession({ onPermissionRequest: approveAll });
            try {
                await session.send("Say OK.");
                await waitFor(() => handler.inferenceEntered, 60_000);
                await session.abort();
                await waitFor(() => handler.sawAbort, 30_000);
            } finally {
                await session.disconnect();
            }

            expect(handler.inferenceEntered, "expected the inference callback to be entered").toBe(
                true
            );
            expect(handler.sawAbort, "expected the callback to observe runtime cancellation").toBe(
                true
            );
        },
        90_000
    );
});
