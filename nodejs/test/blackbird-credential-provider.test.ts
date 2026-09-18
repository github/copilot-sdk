/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { once } from "node:events";
import { createServer, type Socket } from "node:net";
import { describe, expect, it, onTestFinished, vi } from "vitest";
import {
    createMessageConnection,
    StreamMessageReader,
    StreamMessageWriter,
    type MessageConnection,
} from "vscode-jsonrpc/node.js";
import {
    CopilotClient,
    RuntimeConnection,
    type BlackbirdCredentialAcquireResult,
    type BlackbirdCredentialProvider,
} from "../src/index.js";
import type { BlackbirdTokenGetTokenRequest } from "../src/generated/rpc.js";
import { getSdkProtocolVersion } from "../src/sdkProtocolVersion.js";

function deferred<T>() {
    let resolve!: (value: T) => void;
    let reject!: (error: Error) => void;
    const promise = new Promise<T>((yes, no) => {
        resolve = yes;
        reject = no;
    });
    return { promise, resolve, reject };
}

// A framed transport peer, not a model/search server or credential resolver.
// CopilotClient installs its real generated reverse-RPC dispatch on the socket.
async function transportFixture() {
    const connected = deferred<MessageConnection>();
    const sockets: Socket[] = [];
    const requests: { method: string; params: Record<string, unknown> }[] = [];
    const registrations: BlackbirdTokenGetTokenRequest[] = [];
    const bind = vi.fn(async (_params: BlackbirdTokenGetTokenRequest) => {});
    const server = createServer((socket) => {
        sockets.push(socket);
        const peer = createMessageConnection(
            new StreamMessageReader(socket),
            new StreamMessageWriter(socket)
        );
        peer.onRequest(async (method: string, params: Record<string, unknown>) => {
            requests.push({ method, params });
            switch (method) {
                case "connect":
                    return { protocolVersion: getSdkProtocolVersion() };
                case "session.create":
                case "session.resume":
                    return { sessionId: params.sessionId };
                case "session.detach":
                case "session.delete":
                    return { success: true };
                case "session.options.update":
                    return {};
                default:
                    throw new Error(`Unexpected method: ${method}`);
            }
        });
        peer.onRequest(
            "session.blackbird.setCredentialProvider",
            async (params: BlackbirdTokenGetTokenRequest) => {
                registrations.push(params);
                await bind(params);
                return null;
            }
        );
        peer.listen();
        connected.resolve(peer);
    });
    server.listen(0, "127.0.0.1");
    await once(server, "listening");
    const address = server.address();
    if (!address || typeof address === "string") throw new Error("Missing test TCP address");
    const client = new CopilotClient({
        connection: RuntimeConnection.forUri(`127.0.0.1:${address.port}`),
    });
    onTestFinished(async () => {
        await client.forceStop();
        for (const socket of sockets) socket.destroy();
        (await connected.promise).dispose();
        await new Promise<void>((resolve, reject) =>
            server.close((error) => (error ? reject(error) : resolve()))
        );
    });
    await client.start();
    const peer = await connected.promise;
    return {
        client,
        peer,
        requests,
        registrations,
        bind,
        closeTransport: () => sockets.forEach((socket) => socket.destroy()),
        getToken: (params: BlackbirdTokenGetTokenRequest) =>
            peer.sendRequest<BlackbirdCredentialAcquireResult>("blackbirdToken.getToken", params),
    };
}

function provider(accessToken = "ops-sentinel"): BlackbirdCredentialProvider {
    return { host: "github.com", getToken: vi.fn(() => ({ accessToken })) };
}

describe("Blackbird operation credential providers over SDK transport", () => {
    it("keeps normal sessions default-off without changing model authentication", async () => {
        const { client, registrations, getToken } = await transportFixture();
        const session = await client.createSession({ gitHubToken: "model-only-sentinel" });
        expect(registrations).toEqual([]);
        await expect(
            getToken({ host: "github.com", sessionId: session.sessionId, registrationId: "absent" })
        ).rejects.toThrow("No blackbirdToken handler registered");
    });

    it("dispatches distinct model and operational providers without caching or invented expiry", async () => {
        const { client, peer, requests, registrations, getToken } = await transportFixture();
        const modelProvider = vi.fn(() => ({
            kind: "token" as const,
            accessToken: "model-sentinel",
            expiresIn: 900,
        }));
        const session = await client.createSession({ gitHubTokenProvider: modelProvider });
        const ops = provider();
        await session.registerBlackbirdCredentialProvider(ops);
        const [registration] = registrations;
        expect(registration).toEqual({
            sessionId: session.sessionId,
            host: "github.com",
            registrationId: expect.stringMatching(/^[0-9a-f-]{36}$/),
        });
        expect(JSON.stringify(registrations)).not.toContain("sentinel");
        expect(ops.getToken).not.toHaveBeenCalled();
        await expect(getToken(registration)).resolves.toEqual({ accessToken: "ops-sentinel" });
        await expect(getToken(registration)).resolves.toEqual({ accessToken: "ops-sentinel" });
        expect(ops.getToken).toHaveBeenCalledTimes(2);
        expect(ops.getToken).toHaveBeenCalledWith({
            host: "github.com",
            sessionId: session.sessionId,
        });
        expect(modelProvider).not.toHaveBeenCalled();
        const create = requests.find((request) => request.method === "session.create")!;
        await expect(
            peer.sendRequest("gitHubToken.getToken", {
                host: "github.com",
                sessionId: session.sessionId,
                registrationId: create.params.gitHubTokenProviderRegistrationId,
                reason: "initial",
            })
        ).resolves.toEqual({
            kind: "token",
            accessToken: "model-sentinel",
            expiresIn: 900,
        });
        expect(modelProvider).toHaveBeenCalledOnce();
    });

    it("routes two isolated sessions and rejects wrong-session registrations and hosts", async () => {
        const { client, registrations, getToken } = await transportFixture();
        const first = await client.createSession({});
        const second = await client.createSession({});
        const firstProvider = provider("first-ops");
        const secondProvider = provider("second-ops");
        await first.registerBlackbirdCredentialProvider(firstProvider);
        await second.registerBlackbirdCredentialProvider(secondProvider);
        expect(registrations[0].registrationId).not.toBe(registrations[1].registrationId);
        await expect(getToken(registrations[0])).resolves.toEqual({ accessToken: "first-ops" });
        await expect(getToken(registrations[1])).resolves.toEqual({ accessToken: "second-ops" });
        await expect(
            getToken({ ...registrations[0], sessionId: second.sessionId })
        ).rejects.toThrow("unavailable");
        await expect(
            // @ts-expect-error Runtime messages still require host validation.
            getToken({ ...registrations[0], host: "github.example.com" })
        ).rejects.toThrow("unavailable");
        expect(firstProvider.getToken).toHaveBeenCalledOnce();
        expect(secondProvider.getToken).toHaveBeenCalledOnce();
    });

    it("registers the callback before native admission can request it", async () => {
        const { client, registrations, bind, getToken } = await transportFixture();
        bind.mockImplementation(async (registration) => {
            await expect(getToken(registration)).resolves.toEqual({ accessToken: "ops-sentinel" });
        });
        const session = await client.createSession({});
        await session.registerBlackbirdCredentialProvider(provider());
        expect(registrations).toHaveLength(1);
    });

    it("leaves the existing BYOK bearer provider independent of operational credentials", async () => {
        const { client, peer, registrations, getToken } = await transportFixture();
        const modelProvider = vi.fn(async () => "byok-model-sentinel");
        const session = await client.createSession({
            provider: {
                type: "openai",
                baseUrl: "https://model.invalid/v1",
                bearerTokenProvider: modelProvider,
            },
        });
        const ops = provider();
        await session.registerBlackbirdCredentialProvider(ops);
        await expect(getToken(registrations[0])).resolves.toEqual({ accessToken: "ops-sentinel" });
        expect(modelProvider).not.toHaveBeenCalled();
        await expect(
            peer.sendRequest("providerToken.getToken", {
                sessionId: session.sessionId,
                providerName: "default",
            })
        ).resolves.toEqual({ token: "byok-model-sentinel" });
        expect(modelProvider).toHaveBeenCalledWith({
            providerName: "default",
            sessionId: session.sessionId,
        });
        expect(ops.getToken).toHaveBeenCalledOnce();
    });

    it("rejects unsupported configuration before sending a binding request", async () => {
        const { client, registrations } = await transportFixture();
        const session = await client.createSession({});
        await expect(
            session.registerBlackbirdCredentialProvider({
                ...provider(),
                // @ts-expect-error Only github.com is supported.
                host: "github.example.com",
            })
        ).rejects.toThrow("requires github.com");
        await expect(
            // @ts-expect-error JavaScript callers may omit the callback.
            session.registerBlackbirdCredentialProvider({ host: "github.com" })
        ).rejects.toThrow("requires github.com and getToken");
        expect(registrations).toEqual([]);
    });

    it("preserves positive integer expiry and strips unrelated callback properties", async () => {
        const { client, registrations, getToken } = await transportFixture();
        const session = await client.createSession({});
        await session.registerBlackbirdCredentialProvider({
            host: "github.com",
            getToken: () => ({ accessToken: "ops", expiresIn: 123, privateDetail: "not-on-wire" }),
        });
        await expect(getToken(registrations[0])).resolves.toEqual({
            accessToken: "ops",
            expiresIn: 123,
        });
    });

    it.each<unknown>([
        null,
        undefined,
        {},
        { accessToken: "" },
        { accessToken: 7 },
        { accessToken: "ops", expiresIn: 0 },
        { accessToken: "ops", expiresIn: -1 },
        { accessToken: "ops", expiresIn: 1.5 },
        { accessToken: "ops", expiresIn: NaN },
        { accessToken: "ops", expiresIn: Infinity },
        { accessToken: "ops", expiresIn: "60" },
        { accessToken: "ops", expiresIn: null },
    ])("rejects malformed host results without echoing credentials: %j", async (result) => {
        const { client, registrations, getToken } = await transportFixture();
        const session = await client.createSession({});
        await session.registerBlackbirdCredentialProvider({
            host: "github.com",
            // @ts-expect-error Deliberately model an untyped/malformed host callback.
            getToken: () => result,
        });
        await expect(getToken(registrations[0])).rejects.toThrow(
            "Blackbird credential provider returned an invalid credential"
        );
    });

    it("bounds rejected provider errors without forwarding broker secrets", async () => {
        const { client, registrations, getToken } = await transportFixture();
        const session = await client.createSession({});
        await session.registerBlackbirdCredentialProvider({
            host: "github.com",
            getToken: async () => {
                throw new Error("SENSITIVE_BROKER_SENTINEL".repeat(500));
            },
        });
        const error = await getToken(registrations[0]).catch((error: unknown) => error);
        expect(error).toBeInstanceOf(Error);
        expect(String(error)).toContain("Blackbird credential provider failed");
        expect(String(error)).not.toContain("SENSITIVE_BROKER_SENTINEL");
        expect(String(error).length).toBeLessThan(200);
    });

    it("preserves the old provider after rejected registration and removes the rejected callback", async () => {
        const { client, registrations, bind, getToken } = await transportFixture();
        const session = await client.createSession({});
        await session.registerBlackbirdCredentialProvider(provider("old"));
        bind.mockRejectedValueOnce(new Error("Native binding rejected"));
        await expect(session.registerBlackbirdCredentialProvider(provider("new"))).rejects.toThrow(
            "Native binding rejected"
        );
        await expect(getToken(registrations[0])).resolves.toEqual({ accessToken: "old" });
        await expect(getToken(registrations[1])).rejects.toThrow("unavailable");
    });

    it("removes a rejected initial binding without changing session authentication", async () => {
        const { client, registrations, bind, getToken, requests } = await transportFixture();
        const session = await client.createSession({ gitHubToken: "unchanged-model-sentinel" });
        bind.mockRejectedValueOnce(new Error("Unsupported runtime method"));
        const ops = provider();
        await expect(session.registerBlackbirdCredentialProvider(ops)).rejects.toThrow(
            "Unsupported runtime method"
        );
        await expect(getToken(registrations[0])).rejects.toThrow("No blackbirdToken handler");
        expect(ops.getToken).not.toHaveBeenCalled();
        expect(requests.map((request) => request.method)).toEqual(["connect", "session.create"]);
        await session.registerBlackbirdCredentialProvider(ops);
        await expect(getToken(registrations[1])).resolves.toEqual({ accessToken: "ops-sentinel" });
    });

    it("invalidates replaced in-flight callbacks and makes old cleanup harmless", async () => {
        const { client, registrations, getToken } = await transportFixture();
        const session = await client.createSession({});
        const result = deferred<BlackbirdCredentialAcquireResult>();
        const entered = deferred<void>();
        const unregisterOld = await session.registerBlackbirdCredentialProvider({
            host: "github.com",
            getToken: () => {
                entered.resolve();
                return result.promise;
            },
        });
        const pending = getToken(registrations[0]);
        const rejected = expect(pending).rejects.toThrow("unavailable");
        await entered.promise;
        const unregisterNew = await session.registerBlackbirdCredentialProvider(provider("new"));
        unregisterOld();
        unregisterOld();
        result.resolve({ accessToken: "stale-token" });
        await rejected;
        await expect(getToken(registrations[0])).rejects.toThrow("unavailable");
        await expect(getToken(registrations[1])).resolves.toEqual({ accessToken: "new" });
        unregisterNew();
        await expect(getToken(registrations[1])).rejects.toThrow("No blackbirdToken handler");
        expect(registrations).toHaveLength(2); // No native clear/fallback request.
    });

    it("rejects overlapping binds rather than committing callbacks out of native order", async () => {
        const { client, registrations, bind, getToken } = await transportFixture();
        const session = await client.createSession({});
        const admitted = deferred<void>();
        const finish = deferred<void>();
        bind.mockImplementationOnce(async () => {
            admitted.resolve();
            await finish.promise;
        });
        const first = session.registerBlackbirdCredentialProvider(provider("first"));
        await admitted.promise;
        await expect(
            session.registerBlackbirdCredentialProvider(provider("second"))
        ).rejects.toThrow("already in progress");
        finish.resolve();
        await first;
        expect(registrations).toHaveLength(1);
        await expect(getToken(registrations[0])).resolves.toEqual({ accessToken: "first" });
    });

    it("requires explicit rebind after resume even with the old connection alive", async () => {
        const { client, registrations, getToken } = await transportFixture();
        const original = await client.createSession({});
        const result = deferred<BlackbirdCredentialAcquireResult>();
        const entered = deferred<void>();
        const unregisterOld = await original.registerBlackbirdCredentialProvider({
            host: "github.com",
            getToken: () => {
                entered.resolve();
                return result.promise;
            },
        });
        const rejected = expect(getToken(registrations[0])).rejects.toThrow("unavailable");
        await entered.promise;
        const resumed = await client.resumeSession(original.sessionId, {});
        result.resolve({ accessToken: "old" });
        await rejected;
        await expect(getToken(registrations[0])).rejects.toThrow("No blackbirdToken handler");
        await expect(original.registerBlackbirdCredentialProvider(provider())).rejects.toThrow(
            "no longer available"
        );
        await resumed.registerBlackbirdCredentialProvider(provider("resumed"));
        unregisterOld();
        await expect(getToken(registrations[0])).rejects.toThrow("unavailable");
        await expect(getToken(registrations[1])).resolves.toEqual({ accessToken: "resumed" });
    });

    it("retires the old callback when a failed resume replaces its dispatch target", async () => {
        const { client, peer, registrations, getToken } = await transportFixture();
        const original = await client.createSession({});
        await original.registerBlackbirdCredentialProvider(provider());
        peer.onRequest("session.resume", () => {
            throw new Error("Resume failed");
        });
        await expect(client.resumeSession(original.sessionId, {})).rejects.toThrow("Resume failed");
        await expect(getToken(registrations[0])).rejects.toThrow("No session found");
        await expect(original.registerBlackbirdCredentialProvider(provider())).rejects.toThrow(
            "no longer available"
        );
    });

    it.each(["unregister", "disconnect", "delete"] as const)(
        "invalidates in-flight callbacks on %s",
        async (cleanup) => {
            const { client, registrations, getToken } = await transportFixture();
            const session = await client.createSession({});
            const result = deferred<BlackbirdCredentialAcquireResult>();
            const entered = deferred<void>();
            const unregister = await session.registerBlackbirdCredentialProvider({
                host: "github.com",
                getToken: () => {
                    entered.resolve();
                    return result.promise;
                },
            });
            const rejected = expect(getToken(registrations[0])).rejects.toThrow("unavailable");
            await entered.promise;
            if (cleanup === "unregister") unregister();
            if (cleanup === "disconnect") await session.disconnect();
            if (cleanup === "delete") await client.deleteSession(session.sessionId);
            result.resolve({ accessToken: "obsolete" });
            await rejected;
            await expect(getToken(registrations[0])).rejects.toThrow();
            if (cleanup !== "unregister") {
                await expect(
                    session.registerBlackbirdCredentialProvider(provider())
                ).rejects.toThrow("no longer available");
            }
        }
    );

    it("does not restore a pending registration after disconnect", async () => {
        const { client, registrations, bind, getToken } = await transportFixture();
        const session = await client.createSession({});
        const entered = deferred<void>();
        const finish = deferred<void>();
        bind.mockImplementationOnce(async () => {
            entered.resolve();
            await finish.promise;
        });
        const rejected = expect(
            session.registerBlackbirdCredentialProvider(provider())
        ).rejects.toThrow("no longer available");
        await entered.promise;
        await session.disconnect();
        finish.resolve();
        await rejected;
        await expect(getToken(registrations[0])).rejects.toThrow("No blackbirdToken handler");
    });

    it.each(["close", "stop"] as const)("retires callbacks after transport %s", async (action) => {
        const { client, peer, closeTransport } = await transportFixture();
        const session = await client.createSession({});
        await session.registerBlackbirdCredentialProvider(provider());
        if (action === "close") {
            const waiting = deferred<void>();
            peer.onRequest("session.send", () => {
                waiting.resolve();
                return { messageId: "pending" };
            });
            // A structured wait observes the SDK's actual disconnect notification.
            const response = session.sendAndWait({
                prompt: "transport-only",
                responseSchema: { type: "object" },
            });
            const rejected = expect(response).rejects.toThrow("disconnected");
            await waiting.promise;
            closeTransport();
            await rejected;
        } else {
            await client.forceStop();
        }
        await expect(session.registerBlackbirdCredentialProvider(provider())).rejects.toThrow(
            "no longer available"
        );
    });
});
