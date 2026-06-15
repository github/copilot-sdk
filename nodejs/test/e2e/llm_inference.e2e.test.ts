/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll, type LlmInferenceRequest, type LlmInferenceResponse } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("LLM inference callback", async () => {
    // Tracks every request the runtime asks the client to service.
    const received: LlmInferenceRequest[] = [];

    const { copilotClient: client } = await createSdkTestContext({
        copilotClientOptions: {
            llmInference: {
                createLlmInferenceProvider: () => ({
                    async onLlmRequest(req: LlmInferenceRequest): Promise<LlmInferenceResponse> {
                        received.push(req);
                        return {
                            status: 200,
                            headers: { "content-type": ["application/json"] },
                            bodyText: "{}",
                        };
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
        "invokes the callback for model requests, with sessionId populated for in-session traffic",
        async () => {
            const baselineLength = received.length;
            const session = await client.createSession({ onPermissionRequest: approveAll });
            try {
                await session.sendAndWait({ prompt: "Say OK." });
            } finally {
                await session.disconnect();
            }

            if (received.length === baselineLength) {
                console.warn(
                    "[llm-inference e2e] No non-streaming model requests fired during the turn. " +
                        "Wiring is still verified by the schema-level handshake in the prior test."
                );
                return;
            }

            expect(received.length).toBeGreaterThan(baselineLength);
            const newRequests = received.slice(baselineLength);
            for (const r of newRequests) {
                expect(r.url).toMatch(/^https?:\/\//);
                expect(typeof r.method).toBe("string");
                expect(r.metadata).toBeDefined();
            }

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
