/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import type {
    GetBearerToken,
    NamedProviderConfig,
    ProviderModelConfig,
} from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { retry } from "./harness/sdkTestHelper.js";
import type { ParsedHttpExchange } from "../../../test/harness/replayingCapiProxy";

/**
 * End-to-end coverage for the experimental BYOK bearer-token-provider surface
 * (`getBearerToken` on a provider config). The callback stays entirely on the
 * SDK/client side: the SDK strips it from the wire config, sets the
 * `bearerTokenProvider` flag, and the runtime calls back over the session-scoped
 * `providerToken.acquire` RPC before each outbound model request, applying the
 * returned token as the `Authorization` header.
 *
 * These tests validate, against a real runtime + replaying model proxy:
 *  1. the callback's token reaches the model as `Authorization: Bearer <token>`;
 *  2. tokens are refreshed (re-acquired) when they expire;
 *  3. per-provider dispatch routes each provider's turn to its own callback.
 */
describe("BYOK bearer-token provider", async () => {
    const { copilotClient: client, openAiEndpoint } = await createSdkTestContext();

    async function waitForExchanges(minimumCount = 1): Promise<ParsedHttpExchange[]> {
        await retry(
            `capture ${minimumCount} chat completion request(s)`,
            async () => {
                const exchanges = await openAiEndpoint.getExchanges();
                expect(exchanges.length).toBeGreaterThanOrEqual(minimumCount);
            },
            1_200
        );
        return openAiEndpoint.getExchanges();
    }

    function getHeader(exchange: ParsedHttpExchange, name: string): string | undefined {
        const headers = exchange.requestHeaders ?? {};
        const key = Object.keys(headers).find((k) => k.toLowerCase() === name.toLowerCase());
        if (key === undefined) {
            return undefined;
        }
        const value = headers[key];
        return Array.isArray(value) ? value[0] : value;
    }

    it("applies the callback's token as the Authorization header", async () => {
        const SENTINEL = "sentinel-bearer-token-abc123";
        let calls = 0;
        const getBearerToken: GetBearerToken = async () => {
            calls += 1;
            // Far-future expiry: the runtime caches it, so a single turn needs
            // only one acquisition.
            return { token: SENTINEL, expiresOnTimestamp: Date.now() + 60 * 60 * 1000 };
        };

        const providers: NamedProviderConfig[] = [
            {
                name: "mi",
                type: "openai",
                wireApi: "completions",
                baseUrl: openAiEndpoint.url,
                getBearerToken,
            },
        ];
        const models: ProviderModelConfig[] = [
            { id: "default", provider: "mi", wireModel: "byok-gpt-4o" },
        ];

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            model: "mi/default",
            providers,
            models,
        });

        try {
            const reply = await session.sendAndWait({ prompt: "What is 5+5?" });
            const exchanges = await waitForExchanges();
            expect(exchanges.length).toBe(1);

            // The runtime acquired a token via the callback and applied it
            // verbatim as the bearer credential on the outbound model request.
            expect(getHeader(exchanges[0], "Authorization")).toBe(`Bearer ${SENTINEL}`);
            // The far-future expiry means the token is cached, so the single
            // turn needs only one acquisition (it is never re-fetched mid-turn).
            expect(calls).toBe(1);

            // Validate the final assistant response arrived (guards against
            // truncated captures).
            expect(reply?.data.content).toContain("10");
        } finally {
            try {
                await session.disconnect();
            } catch {
                // ignore disconnect errors for the fake BYOK endpoint
            }
        }
    });

    it("re-acquires a fresh token when the previous one has expired", async () => {
        let calls = 0;
        const getBearerToken: GetBearerToken = async () => {
            calls += 1;
            // Already-expired expiry forces the runtime to re-acquire on the next
            // request rather than reuse the cached token.
            return { token: `rotating-token-${calls}`, expiresOnTimestamp: Date.now() - 1 };
        };

        const providers: NamedProviderConfig[] = [
            {
                name: "mi",
                type: "openai",
                wireApi: "completions",
                baseUrl: openAiEndpoint.url,
                getBearerToken,
            },
        ];
        const models: ProviderModelConfig[] = [
            { id: "default", provider: "mi", wireModel: "byok-gpt-4o" },
        ];

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            model: "mi/default",
            providers,
            models,
        });

        try {
            const reply1 = await session.sendAndWait({ prompt: "What is 1+1?" });
            const afterTurn1 = await waitForExchanges(1);
            const callsAfterTurn1 = calls;

            const reply2 = await session.sendAndWait({ prompt: "What is 2+2?" });
            const exchanges = await waitForExchanges(2);

            // Each outbound request carries a freshly-acquired, distinct token,
            // proving the runtime refreshed rather than reusing the expired one.
            const auth1 = getHeader(exchanges[0], "Authorization");
            const auth2 = getHeader(exchanges[1], "Authorization");
            expect(auth1).toMatch(/^Bearer rotating-token-\d+$/);
            expect(auth2).toMatch(/^Bearer rotating-token-\d+$/);
            expect(auth1).not.toBe(auth2);

            // The second turn triggered at least one additional acquisition.
            expect(calls).toBeGreaterThan(callsAfterTurn1);

            // Validate the final assistant responses arrived (guards against
            // truncated captures).
            expect(reply1?.data.content).toContain("2");
            expect(reply2?.data.content).toContain("4");
            void afterTurn1;
        } finally {
            try {
                await session.disconnect();
            } catch {
                // ignore disconnect errors for the fake BYOK endpoint
            }
        }
    });

    it("dispatches token acquisition per provider", async () => {
        const tokenByProvider: Record<string, string> = {
            red: "token-for-red",
            blue: "token-for-blue",
        };
        const acquiredFor: string[] = [];
        const makeCallback =
            (providerName: string): GetBearerToken =>
            async (args) => {
                // The runtime forwards the requesting provider's name so the
                // client can dispatch to the right credential.
                expect(args.providerName).toBe(providerName);
                acquiredFor.push(providerName);
                return {
                    token: tokenByProvider[providerName],
                    expiresOnTimestamp: Date.now() + 60 * 60 * 1000,
                };
            };

        const providers: NamedProviderConfig[] = [
            {
                name: "red",
                type: "openai",
                wireApi: "completions",
                baseUrl: openAiEndpoint.url,
                getBearerToken: makeCallback("red"),
            },
            {
                name: "blue",
                type: "openai",
                wireApi: "completions",
                baseUrl: openAiEndpoint.url,
                getBearerToken: makeCallback("blue"),
            },
        ];
        const models: ProviderModelConfig[] = [
            { id: "default", provider: "red", wireModel: "byok-gpt-4o" },
            { id: "default", provider: "blue", wireModel: "byok-gpt-4o" },
        ];

        async function runTurn(selectionId: string, prompt: string): Promise<string | undefined> {
            const session = await client.createSession({
                onPermissionRequest: approveAll,
                model: selectionId,
                providers,
                models,
            });
            try {
                const reply = await session.sendAndWait({ prompt });
                return reply?.data.content;
            } finally {
                try {
                    await session.disconnect();
                } catch {
                    // ignore disconnect errors for the fake BYOK endpoint
                }
            }
        }

        const replyRed = await runTurn("red/default", "What is 3+3?");
        const afterRed = await waitForExchanges(1);
        expect(getHeader(afterRed[0], "Authorization")).toBe(`Bearer ${tokenByProvider.red}`);

        const replyBlue = await runTurn("blue/default", "What is 4+4?");
        const exchanges = await waitForExchanges(2);

        // The two turns were authenticated with their respective providers'
        // tokens, proving per-provider dispatch (not a single session-global
        // credential).
        const authValues = exchanges.map((e) => getHeader(e, "Authorization"));
        expect(authValues).toContain(`Bearer ${tokenByProvider.red}`);
        expect(authValues).toContain(`Bearer ${tokenByProvider.blue}`);
        expect(acquiredFor).toContain("red");
        expect(acquiredFor).toContain("blue");

        // Validate the final assistant responses arrived (guards against
        // truncated captures).
        expect(replyRed).toContain("6");
        expect(replyBlue).toContain("8");
    });
});
