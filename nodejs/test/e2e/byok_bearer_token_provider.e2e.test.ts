/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { beforeEach, describe, expect, it } from "vitest";
import { approveAll, CopilotRequestHandler } from "../../src/index.js";
import type {
    CopilotRequestContext,
    BearerTokenProvider,
    NamedProviderConfig,
    ProviderModelConfig,
} from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

/**
 * A captured outbound HTTP request the runtime aimed at a fake BYOK provider
 * endpoint: just the host and the `Authorization` header, which is all these
 * tests need to assert on.
 */
interface CapturedRequest {
    host: string;
    authorization?: string;
}

// Fake BYOK provider base URLs. These hosts are never actually dialed: the
// client-global request interceptor fully answers any request aimed at a
// `.invalid` host, so they only need to be syntactically valid, non-resolving
// URLs. Distinct hosts let the per-provider test assert routing by host.
const PRIMARY_HOST = "byok-endpoint.invalid";
const PRIMARY_BASE_URL = `https://${PRIMARY_HOST}/v1`;
const RED_HOST = "byok-red.invalid";
const RED_BASE_URL = `https://${RED_HOST}/v1`;
const BLUE_HOST = "byok-blue.invalid";
const BLUE_BASE_URL = `https://${BLUE_HOST}/v1`;

/**
 * Client-global HTTP request interceptor (from the SDK's `CopilotRequestHandler`
 * surface) used in place of a real HTTP listener.
 *
 * The runtime invokes {@link sendRequest} for every model-layer HTTP request it
 * would otherwise issue. We capture the ones aimed at a fake BYOK host —
 * recording the `Authorization` header the runtime applied after calling the
 * provider's `bearerTokenProvider` callback over the session-scoped
 * `providerToken.getToken` RPC — and answer them with a synthetic `404` (a
 * non-retryable status, so each outbound model request yields exactly one
 * capture). Every other request (CAPI bootstrap: model catalog, policy, …) is
 * passed straight through to the real network via `super.sendRequest`.
 *
 * Because the handler is client-global (one per CLI process), it is installed
 * once for the whole fixture and {@link reset} between tests.
 */
class CapturingRequestHandler extends CopilotRequestHandler {
    public readonly captures: CapturedRequest[] = [];

    protected override async sendRequest(
        request: Request,
        ctx: CopilotRequestContext
    ): Promise<Response> {
        const url = new URL(request.url);
        if (url.hostname.endsWith(".invalid")) {
            this.captures.push({
                host: url.host,
                authorization: request.headers.get("authorization") ?? undefined,
            });
            return new Response(JSON.stringify({ error: { message: "fake byok endpoint" } }), {
                status: 404,
                headers: { "content-type": "application/json" },
            });
        }
        return super.sendRequest(request, ctx);
    }

    reset(): void {
        this.captures.length = 0;
    }

    /** The `Authorization` headers captured across BYOK requests, in arrival order. */
    authHeaders(): string[] {
        return this.captures
            .map((c) => c.authorization)
            .filter((v): v is string => typeof v === "string");
    }

    /** The `Authorization` header captured for requests aimed at `host`, if any. */
    authHeaderForHost(host: string): string | undefined {
        return this.captures.find((c) => c.host === host)?.authorization;
    }
}

/**
 * End-to-end coverage for the experimental BYOK bearer-token-provider surface
 * (`bearerTokenProvider` on a provider config). The callback stays entirely on the
 * SDK/client side: the SDK strips it from the wire config, sets the
 * `hasBearerTokenProvider` flag, and the runtime calls back over the session-scoped
 * `providerToken.getToken` RPC before each outbound model request, applying the
 * returned token as the `Authorization` header.
 *
 * Rather than standing up a real HTTP listener, these tests install a
 * client-global {@link CapturingRequestHandler} that intercepts the runtime's
 * outbound model request in-process, captures the `Authorization` header, and
 * returns a synthetic response. They validate, against a real runtime:
 *  1. the callback's token reaches the model request as `Authorization: Bearer <token>`;
 *  2. the runtime re-acquires a token per request (no runtime-side caching);
 *  3. per-provider dispatch routes each provider's turn to its own callback,
 *     and the resulting token reaches that provider's endpoint.
 */
describe("BYOK bearer-token provider", async () => {
    const handler = new CapturingRequestHandler();
    const { copilotClient: client } = await createSdkTestContext({
        copilotClientOptions: { requestHandler: handler },
    });

    beforeEach(() => {
        handler.reset();
    });

    /** Drive one BYOK turn; the synthetic 404 errors the turn, which is expected. */
    async function runTurn(
        providers: NamedProviderConfig[],
        models: ProviderModelConfig[],
        selectionId: string,
        prompt: string
    ): Promise<void> {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            model: selectionId,
            providers,
            models,
        });
        try {
            // The interceptor always 404s, so the turn errors after the runtime
            // has already sent the (token-bearing) request — which is all we
            // assert on. Swallow the resulting error.
            await session.sendAndWait({ prompt }).catch(() => undefined);
        } finally {
            try {
                await session.disconnect();
            } catch {
                // ignore disconnect errors for the fake BYOK endpoint
            }
        }
    }

    it("applies the callback's token as the Authorization header", async () => {
        const SENTINEL = "sentinel-bearer-token-abc123";
        let calls = 0;
        const getBearerToken: BearerTokenProvider = async () => {
            calls += 1;
            return SENTINEL;
        };

        const providers: NamedProviderConfig[] = [
            {
                name: "mi",
                type: "openai",
                wireApi: "completions",
                baseUrl: PRIMARY_BASE_URL,
                bearerTokenProvider: getBearerToken,
            },
        ];
        const models: ProviderModelConfig[] = [
            { id: "default", provider: "mi", wireModel: "byok-gpt-4o" },
        ];

        await runTurn(providers, models, "mi/default", "What is 5+5?");

        // The runtime acquired a token via the callback and applied it verbatim as
        // the bearer credential on the outbound model request.
        expect(handler.authHeaders()).toContain(`Bearer ${SENTINEL}`);
        expect(calls).toBeGreaterThanOrEqual(1);
    });

    it("re-acquires a fresh token for each request (no runtime caching)", async () => {
        let calls = 0;
        const getBearerToken: BearerTokenProvider = async () => {
            calls += 1;
            // A distinct token per acquisition proves the runtime re-invokes the
            // callback per request rather than caching a previous token.
            return `rotating-token-${calls}`;
        };

        const providers: NamedProviderConfig[] = [
            {
                name: "mi",
                type: "openai",
                wireApi: "completions",
                baseUrl: PRIMARY_BASE_URL,
                bearerTokenProvider: getBearerToken,
            },
        ];
        const models: ProviderModelConfig[] = [
            { id: "default", provider: "mi", wireModel: "byok-gpt-4o" },
        ];

        await runTurn(providers, models, "mi/default", "What is 1+1?");
        await runTurn(providers, models, "mi/default", "What is 2+2?");

        // Each outbound request carries a freshly-acquired, distinct token.
        const auths = handler.authHeaders();
        expect(auths.length).toBeGreaterThanOrEqual(2);
        expect(auths[0]).toMatch(/^Bearer rotating-token-\d+$/);
        expect(auths[1]).toMatch(/^Bearer rotating-token-\d+$/);
        expect(auths[0]).not.toBe(auths[1]);
        expect(calls).toBeGreaterThanOrEqual(2);
    });

    it("dispatches token acquisition per provider", async () => {
        const tokenByProvider: Record<string, string> = {
            red: "token-for-red",
            blue: "token-for-blue",
        };
        const acquiredFor: string[] = [];
        const makeCallback =
            (providerName: string): BearerTokenProvider =>
            async (args) => {
                // The runtime forwards the requesting provider's name so the client
                // can dispatch to the right credential.
                expect(args.providerName).toBe(providerName);
                // The runtime also forwards the owning session id so a
                // client-level shared callback can resolve the session.
                expect(typeof args.sessionId).toBe("string");
                expect(args.sessionId.length).toBeGreaterThan(0);
                acquiredFor.push(providerName);
                return tokenByProvider[providerName];
            };

        const providers: NamedProviderConfig[] = [
            {
                name: "red",
                type: "openai",
                wireApi: "completions",
                baseUrl: RED_BASE_URL,
                bearerTokenProvider: makeCallback("red"),
            },
            {
                name: "blue",
                type: "openai",
                wireApi: "completions",
                baseUrl: BLUE_BASE_URL,
                bearerTokenProvider: makeCallback("blue"),
            },
        ];
        const models: ProviderModelConfig[] = [
            { id: "default", provider: "red", wireModel: "byok-gpt-4o" },
            { id: "default", provider: "blue", wireModel: "byok-gpt-4o" },
        ];

        await runTurn(providers, models, "red/default", "What is 3+3?");
        await runTurn(providers, models, "blue/default", "What is 4+4?");

        // Each provider's turn was authenticated with its own token AND that token
        // was delivered to that provider's endpoint, proving per-provider dispatch
        // (not a single session-global credential).
        expect(handler.authHeaderForHost(RED_HOST)).toBe(`Bearer ${tokenByProvider.red}`);
        expect(handler.authHeaderForHost(BLUE_HOST)).toBe(`Bearer ${tokenByProvider.blue}`);
        expect(acquiredFor).toContain("red");
        expect(acquiredFor).toContain("blue");
    });
});
