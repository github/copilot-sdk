/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { createServer, IncomingMessage, Server, ServerResponse } from "http";
import { AddressInfo } from "net";
import { afterAll, beforeEach, describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import type { ProviderConfig } from "../../src/index.js";
import { MockIdentityServer } from "./harness/MockIdentityServer.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { retry } from "./harness/sdkTestHelper.js";

/**
 * End-to-end coverage for Azure managed identity (MI) authentication on a BYOK
 * provider. Proves the full SDK → runtime → Rust credential chain wiring without
 * any real network:
 *
 *  - The shared **mock identity endpoint** (`test/harness/mockIdentityServer.ts`,
 *    spawned via {@link MockIdentityServer}) plays the App Service / Functions
 *    managed identity contract (`IDENTITY_ENDPOINT` + `IDENTITY_HEADER`). It
 *    returns a fixed fake AAD token and records the `resource` + identity query
 *    parameters the runtime asked for. Living in the shared harness lets every
 *    SDK language reuse it.
 *  - A local **mock model endpoint** is the BYOK provider's `baseUrl`. It records
 *    the `Authorization` header the runtime sent and replies with a minimal
 *    streamed chat completion so the turn finishes cleanly.
 *
 * The session is configured with `managedIdentity` (no apiKey/bearerToken), runs
 * one real turn, and we assert the model request carried
 * `Authorization: Bearer <fake-token>` and that the identity endpoint was asked
 * for the right resource + identity. Because the BYOK base URL is the mock model
 * server (not the replay proxy), the test needs no recorded snapshot and never
 * touches the network.
 */

interface ModelRequest {
    authorization: string | undefined;
    path: string;
}

/** Reads the full request body as a string. */
function readBody(req: IncomingMessage): Promise<string> {
    return new Promise((resolve, reject) => {
        const chunks: Buffer[] = [];
        req.on("data", (c: Buffer) => chunks.push(c));
        req.on("end", () => resolve(Buffer.concat(chunks).toString("utf8")));
        req.on("error", reject);
    });
}

function listen(server: Server): Promise<number> {
    return new Promise((resolve, reject) => {
        server.once("error", reject);
        server.listen(0, "127.0.0.1", () => resolve((server.address() as AddressInfo).port));
    });
}

function close(server: Server): Promise<void> {
    return new Promise((resolve) => server.close(() => resolve()));
}

describe("BYOK managed identity authentication", async () => {
    const { copilotClient: client, env } = await createSdkTestContext();

    // App Service / Functions managed identity endpoint, shared across SDKs.
    const identity = new MockIdentityServer();
    await identity.start();

    const modelRequests: ModelRequest[] = [];

    // BYOK model endpoint. Records the Authorization header the runtime injected
    // and returns a minimal streamed OpenAI chat completion so the turn ends.
    const modelServer = createServer(async (req: IncomingMessage, res: ServerResponse) => {
        const body = await readBody(req);
        modelRequests.push({
            authorization: (req.headers["authorization"] as string | undefined) ?? undefined,
            path: req.url ?? "",
        });
        let wantsStream = false;
        try {
            wantsStream = (JSON.parse(body) as { stream?: boolean }).stream === true;
        } catch {
            // Non-JSON body: fall back to a non-streaming reply.
        }

        if (wantsStream) {
            res.writeHead(200, { "content-type": "text/event-stream" });
            const base = {
                id: "mock-completion",
                object: "chat.completion.chunk",
                created: Math.floor(Date.now() / 1000),
                model: "mock-model",
            };
            res.write(
                `data: ${JSON.stringify({
                    ...base,
                    choices: [
                        {
                            index: 0,
                            delta: { role: "assistant", content: "OK" },
                            finish_reason: null,
                            logprobs: null,
                        },
                    ],
                })}\n\n`
            );
            res.write(
                `data: ${JSON.stringify({
                    ...base,
                    choices: [{ index: 0, delta: {}, finish_reason: "stop", logprobs: null }],
                })}\n\n`
            );
            res.write("data: [DONE]\n\n");
            res.end();
        } else {
            res.writeHead(200, { "content-type": "application/json" });
            res.end(
                JSON.stringify({
                    id: "mock-completion",
                    object: "chat.completion",
                    created: Math.floor(Date.now() / 1000),
                    model: "mock-model",
                    choices: [
                        {
                            index: 0,
                            message: { role: "assistant", content: "OK" },
                            finish_reason: "stop",
                            logprobs: null,
                        },
                    ],
                    usage: { prompt_tokens: 0, completion_tokens: 0, total_tokens: 0 },
                })
            );
        }
    });

    const modelPort = await listen(modelServer);
    const modelBaseUrl = `http://127.0.0.1:${modelPort}`;

    // The harness env object is the same one passed to the CLI subprocess, so
    // mutating it before the first createSession() configures managed identity
    // resolution inside the runtime. These are all standard Azure env vars.
    env.IDENTITY_ENDPOINT = identity.endpoint;
    env.IDENTITY_HEADER = identity.header;
    env.AZURE_TOKEN_CREDENTIALS = "ManagedIdentityCredential";
    // Ensure no ambient user-assigned id leaks in from the host environment.
    env.AZURE_CLIENT_ID = "";

    beforeEach(async () => {
        await identity.reset();
        modelRequests.length = 0;
    });

    afterAll(async () => {
        await identity.stop();
        await close(modelServer);
    });

    async function runTurn(provider: ProviderConfig): Promise<void> {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            provider,
        });
        try {
            await session.sendAndWait({ prompt: "What is 5+5?" });
        } finally {
            try {
                await session.disconnect();
            } catch {
                // disconnect may fail since the BYOK provider is a local mock
            }
        }
    }

    it("acquires a system-assigned managed identity token and injects it as a bearer", async () => {
        await runTurn({
            type: "openai",
            wireApi: "completions",
            baseUrl: modelBaseUrl,
            modelId: "claude-sonnet-4.5",
            managedIdentity: {},
        });

        await retry(
            "capture a model request",
            async () => expect(modelRequests.length).toBeGreaterThanOrEqual(1),
            1_200
        );

        // The runtime acquired the fake token from the identity endpoint and
        // injected it as the model request's bearer credential.
        expect(modelRequests[0].authorization).toBe(`Bearer ${identity.token}`);

        // The identity endpoint was hit with the App Service secret header, the
        // default cognitiveservices resource, and NO identity selector (system
        // assigned).
        const identityRequests = await identity.getRecordedRequests();
        expect(identityRequests.length).toBeGreaterThanOrEqual(1);
        const idReq = identityRequests[0];
        expect(idReq.identityHeader).toBe(identity.header);
        expect(idReq.resource).toBe("https://cognitiveservices.azure.com");
        expect(idReq.identityParams).toEqual({});
    });

    it("acquires a user-assigned managed identity (clientId) with a custom scope", async () => {
        // A custom scope keeps this turn's token cache key distinct from the
        // system-assigned test above (the runtime caches by scope + identity).
        await runTurn({
            type: "openai",
            wireApi: "completions",
            baseUrl: modelBaseUrl,
            modelId: "claude-sonnet-4.5",
            managedIdentity: {
                clientId: "11111111-2222-3333-4444-555555555555",
                scope: "https://gateway.example.test/.default",
            },
        });

        await retry(
            "capture a model request",
            async () => expect(modelRequests.length).toBeGreaterThanOrEqual(1),
            1_200
        );

        expect(modelRequests[0].authorization).toBe(`Bearer ${identity.token}`);

        const identityRequests = await identity.getRecordedRequests();
        expect(identityRequests.length).toBeGreaterThanOrEqual(1);
        const idReq = identityRequests[0];
        expect(idReq.identityHeader).toBe(identity.header);
        // The custom scope's resource (scope minus the /.default suffix).
        expect(idReq.resource).toBe("https://gateway.example.test");
        // The user-assigned client id was sent as the App Service client_id param.
        expect(idReq.identityParams).toEqual({
            client_id: "11111111-2222-3333-4444-555555555555",
        });
    });

    it("reuses a cached token across turns while it is still valid", async () => {
        // A unique scope keeps this turn's cache key isolated from the other
        // tests (the runtime caches process-wide by scope + identity).
        const provider: ProviderConfig = {
            type: "openai",
            wireApi: "completions",
            baseUrl: modelBaseUrl,
            modelId: "claude-sonnet-4.5",
            managedIdentity: { scope: "https://cache-test.example.test/.default" },
        };

        // Default lifetime (1h) is well outside the runtime's 5-minute refresh
        // buffer, so the token acquired on the first turn stays cached.
        await runTurn(provider);
        await runTurn(provider);

        // Two turns, but the identity endpoint was only hit once: the second
        // turn reused the cached token instead of re-acquiring one.
        const identityRequests = await identity.getRecordedRequests();
        expect(identityRequests.length).toBe(1);

        // Every model request across both turns carried that one cached token.
        expect(modelRequests.length).toBeGreaterThanOrEqual(2);
        for (const request of modelRequests) {
            expect(request.authorization).toBe(`Bearer ${identity.token}`);
        }
    });

    it("refreshes the token on the next turn once it is within the expiry buffer", async () => {
        // Mint short-lived, rotating tokens: a 1-second lifetime is inside the
        // runtime's 5-minute refresh buffer, so the cached token is treated as
        // stale immediately and re-acquired on the next turn. Rotation makes the
        // refreshed token observably different from the first one.
        await identity.configure({ expiresInSeconds: 1, rotateTokens: true });

        const provider: ProviderConfig = {
            type: "openai",
            wireApi: "completions",
            baseUrl: modelBaseUrl,
            modelId: "claude-sonnet-4.5",
            managedIdentity: { scope: "https://refresh-test.example.test/.default" },
        };

        await runTurn(provider);
        const firstTurnBearer = modelRequests.at(-1)?.authorization;

        await runTurn(provider);
        const secondTurnBearer = modelRequests.at(-1)?.authorization;

        // The endpoint was hit again for the second turn rather than serving a
        // cached token.
        const identityRequests = await identity.getRecordedRequests();
        expect(identityRequests.length).toBeGreaterThanOrEqual(2);

        // The second turn's model request carried a freshly minted token, not
        // the one from the first turn — proving automatic refresh.
        expect(secondTurnBearer).not.toBe(firstTurnBearer);
        expect(secondTurnBearer).toBe(`Bearer ${identityRequests.at(-1)?.issuedToken}`);
    });
});
