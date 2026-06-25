/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("session.provider.getEndpoint RPC", async () => {
    const { copilotClient: client, env } = await createSdkTestContext();

    // The provider endpoint API is gated behind an opt-in env var; the harness
    // env object is the same one passed to the CLI subprocess, so mutating it
    // here enables the API for this test file's client.
    env.COPILOT_ALLOW_GET_PROVIDER_ENDPOINT = "true";

    it("returns the BYOK provider endpoint when a custom provider is configured", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            provider: {
                type: "openai",
                wireApi: "completions",
                baseUrl: "https://api.example.test/v1",
                apiKey: "byok-secret",
                headers: { "X-Custom-Header": "byok-yes" },
            },
        });

        try {
            const endpoint = await session.rpc.provider.getEndpoint({});

            expect(endpoint.type).toBe("openai");
            expect(endpoint.wireApi).toBe("completions");
            expect(endpoint.baseUrl).toBe("https://api.example.test/v1");
            expect(endpoint.apiKey).toBe("byok-secret");
            expect(endpoint.headers).toMatchObject({ "X-Custom-Header": "byok-yes" });
            // BYOK sessions never issue a CAPI session token.
            expect(endpoint.sessionToken).toBeUndefined();
        } finally {
            try {
                await session.disconnect();
            } catch {
                // disconnect may fail since the BYOK provider URL is fake
            }
        }
    });

    it("returns the CAPI provider endpoint for an OAuth-authenticated session", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
        });

        try {
            const endpoint = await session.rpc.provider.getEndpoint({});

            expect(["openai", "azure", "anthropic"]).toContain(endpoint.type);
            // wireApi is omitted for anthropic; otherwise one of the OpenAI shapes.
            if (endpoint.type !== "anthropic") {
                expect(["completions", "responses"]).toContain(endpoint.wireApi);
            }

            // CAPI baseUrl is the (proxy) Copilot API URL injected by the harness.
            expect(endpoint.baseUrl).toMatch(/^https?:\/\//);

            // For CAPI OAuth sessions the apiKey is the resolved GitHub bearer.
            expect(endpoint.apiKey).toBeTypeOf("string");
            expect(endpoint.apiKey!.length).toBeGreaterThan(0);

            // Standard CAPI headers should be present, and Authorization is
            // surfaced as the runtime sends it (`Bearer <apiKey>`).
            expect(endpoint.headers["Copilot-Integration-Id"]).toBeTypeOf("string");
            expect(endpoint.headers["User-Agent"]).toMatch(/Copilot/i);
            expect(endpoint.headers["X-GitHub-Api-Version"]).toBeTypeOf("string");
            expect(endpoint.headers["X-Interaction-Id"]).toMatch(/[0-9a-f-]{8,}/);
            expect(endpoint.headers.Authorization).toBe(`Bearer ${endpoint.apiKey}`);

            // When the omit-modelId path returned an auto-mode session token, it
            // must use the documented header name and an ISO 8601 expiry. The
            // harness may have a non-auto model selected, in which case the
            // field is simply omitted.
            if (endpoint.sessionToken) {
                expect(endpoint.sessionToken.header).toBe("Copilot-Session-Token");
                expect(endpoint.sessionToken.token.length).toBeGreaterThan(0);
                if (endpoint.sessionToken.expiresAt !== undefined) {
                    expect(Date.parse(endpoint.sessionToken.expiresAt)).not.toBeNaN();
                }
            }
        } finally {
            await session.disconnect();
        }
    });
});
