/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { randomUUID } from "node:crypto";
import { once } from "node:events";
import { createServer, type Socket } from "node:net";
import { setTimeout } from "node:timers/promises";
import { describe, expect, expectTypeOf, it, onTestFinished, vi } from "vitest";
import {
    CancellationTokenSource,
    createMessageConnection,
    ErrorCodes,
    ResponseError,
    StreamMessageReader,
    StreamMessageWriter,
    type CancellationToken,
    type MessageConnection,
} from "vscode-jsonrpc/node.js";
import {
    CopilotClient,
    RuntimeConnection,
    type CopilotClientOptions,
    type ExtensionLaunchProfile,
    type ExtensionLaunchProviderHandler,
    type ExtensionLaunchProviderRegistrationResult,
    type ExtensionLaunchProviderResolveRequest,
    type ExtensionLaunchProviderResolveResult,
    type ExtensionSource,
    type PermissionRequestedEvent,
    type ResumeSessionConfig,
    type RetainedEvent,
    type SessionConfig,
    type SessionEvent,
    type SessionRetainRequest,
} from "../src/index.js";
import { ExtensionLaunchProviderConnection } from "../src/extensionLaunchProvider.js";
import type { PermissionDecisionRequest, SessionOpenOptions } from "../src/generated/rpc.js";

function deferred<T>() {
    let resolve!: (value: T | PromiseLike<T>) => void;
    const promise = new Promise<T>((complete) => {
        resolve = complete;
    });
    return { promise, resolve };
}

// A synthetic loopback runtime peer, not an injected client connection or handler table.
async function runtimePeer(configure: (connection: MessageConnection) => void = () => {}) {
    const peers: { connection: MessageConnection; socket: Socket }[] = [];
    const clients: CopilotClient[] = [];
    const server = createServer((socket) => {
        const connection = createMessageConnection(
            new StreamMessageReader(socket),
            new StreamMessageWriter(socket)
        );
        peers.push({ connection, socket });
        connection.onRequest("connect", () => ({ protocolVersion: 3 }));
        connection.onRequest("registerExtensionLaunchProvider", () => ({ contractVersion: 1 }));
        connection.onRequest("session.create", (params: { sessionId: string }) => ({
            sessionId: params.sessionId,
        }));
        connection.onRequest("session.resume", (params: { sessionId: string }) => ({
            sessionId: params.sessionId,
        }));
        connection.onRequest("session.detach", () => ({ success: true }));
        connection.onClose(() => connection.dispose());
        configure(connection);
        connection.listen();
    });
    onTestFinished(async () => {
        const errors: Error[] = [];
        try {
            for (const client of clients) {
                errors.push(...(await client.stop()));
            }
        } finally {
            for (const peer of peers) {
                peer.connection.dispose();
                peer.socket.destroy();
            }
            await new Promise<void>((resolve, reject) => {
                server.close((error) => (error ? reject(error) : resolve()));
            });
        }
        expect(errors).toEqual([]);
    });
    server.listen(0, "127.0.0.1");
    await once(server, "listening");
    const address = server.address();
    if (!address || typeof address === "string") {
        throw new Error("Expected a loopback TCP listener");
    }
    return {
        peers,
        client(options: Omit<CopilotClientOptions, "connection"> = {}) {
            const client = new CopilotClient({
                ...options,
                connection: RuntimeConnection.forUri(`127.0.0.1:${address.port}`),
            });
            clients.push(client);
            return client;
        },
    };
}

const profile: ExtensionLaunchProfile = {
    executable: "/synthetic/bin/node",
    args: ["--import", "/original directory/bootstrap.mjs", "/original directory/extension.mjs"],
    env: { SYNTHETIC_LITERAL: "literal value", COPILOT_SDK_PATH: "/runtime-selected/sdk" },
};
const candidate: ExtensionLaunchProviderResolveRequest = {
    id: "project:fixture",
    name: "fixture",
    modulePath: "/original directory/extension.mjs",
    source: "project",
    sessionId: "unit-session",
    defaultLaunch: profile,
};
const grant: ExtensionLaunchProviderHandler = {
    resolve: async (request) => ({ launch: request.defaultLaunch }),
};

describe("public script safety lifecycle configuration", () => {
    it("uses the canonical optional setting for both public configs", () => {
        expectTypeOf<SessionConfig["enableScriptSafety"]>().toEqualTypeOf<
            SessionOpenOptions["enableScriptSafety"]
        >();
        expectTypeOf<ResumeSessionConfig["enableScriptSafety"]>().toEqualTypeOf<
            boolean | undefined
        >();
    });

    describe.each(["create", "resume"])("%s", (operation) => {
        it.each([undefined, false, true])(
            "forwards %j before the resolver and pre-return permission handling",
            async (enableScriptSafety) => {
                let returned = false;
                const order: string[] = [];
                const events: SessionEvent[] = [];
                const responded = deferred<void>();
                const runtime = await runtimePeer((connection) => {
                    connection.onRequest(
                        "session.permissions.handlePendingPermissionRequest",
                        (params: PermissionDecisionRequest & SessionRetainRequest) => {
                            expect(params).toEqual({
                                sessionId: "script-safety-session",
                                requestId: "early-permission",
                                result: { kind: "reject" },
                            });
                            order.push("permission-response");
                            responded.resolve();
                            return { success: true };
                        }
                    );
                    connection.onRequest(
                        `session.${operation}`,
                        async (
                            params: SessionRetainRequest &
                                Pick<SessionOpenOptions, "enableScriptSafety">
                        ) => {
                            expect(params.enableScriptSafety).toBe(enableScriptSafety);
                            expect(Object.hasOwn(params, "enableScriptSafety")).toBe(
                                enableScriptSafety !== undefined
                            );
                            order.push("initial-request");
                            await expect(
                                connection.sendRequest("extensionLaunchProvider.resolve", {
                                    ...candidate,
                                    sessionId: params.sessionId,
                                })
                            ).resolves.toEqual({ launch: profile });
                            const event: PermissionRequestedEvent = {
                                type: "permission.requested",
                                id: randomUUID(),
                                timestamp: new Date().toISOString(),
                                parentId: null,
                                data: {
                                    requestId: "early-permission",
                                    permissionRequest: {
                                        kind: "shell",
                                        canOfferSessionApproval: false,
                                        commands: [],
                                        fullCommandText: "pwd",
                                        hasWriteFileRedirection: false,
                                        intention: "Synthetic early permission routing",
                                        possiblePaths: [],
                                        possibleUrls: [],
                                    },
                                },
                            };
                            await connection.sendNotification("session.event", {
                                sessionId: params.sessionId,
                                event,
                            });
                            await responded.promise;
                            expect(returned).toBe(false);
                            return { sessionId: params.sessionId };
                        }
                    );
                });
                const client = runtime.client({
                    extensionLaunchProvider: {
                        resolve: async () => {
                            expect(returned).toBe(false);
                            order.push("resolver");
                            return { launch: profile };
                        },
                    },
                });
                const config: SessionConfig = {
                    ...(enableScriptSafety === undefined ? {} : { enableScriptSafety }),
                    requestExtensions: true,
                    onEvent: (event) => events.push(event),
                    onPermissionRequest: (_, context) => {
                        expect(returned).toBe(false);
                        expect(context.sessionId).toBe("script-safety-session");
                        order.push("permission-handler");
                        return { kind: "reject" };
                    },
                };
                const session =
                    operation === "create"
                        ? await client.createSession({
                              ...config,
                              sessionId: "script-safety-session",
                          })
                        : await client.resumeSession("script-safety-session", config);
                returned = true;
                expect(session.sessionId).toBe("script-safety-session");
                expect(order).toEqual([
                    "initial-request",
                    "resolver",
                    "permission-handler",
                    "permission-response",
                ]);
                expect(events.map((event) => event.type)).toEqual(["permission.requested"]);
            }
        );
    });
});

describe("public extension launch provider attachment", () => {
    it("does not register a provider when the option is omitted", async () => {
        const register = vi.fn(() => ({ contractVersion: 1 }));
        const runtime = await runtimePeer((connection) => {
            connection.onRequest("registerExtensionLaunchProvider", register);
        });
        const client = runtime.client();
        const session = await client.createSession({});
        await client.resumeSession(session.sessionId, {});
        expect(register).not.toHaveBeenCalled();
    });

    describe.each(["start", "create", "resume"])("%s negotiation", (operation) => {
        it.each([
            null,
            undefined,
            {},
            { contractVersion: 0 },
            { contractVersion: 2 },
            { contractVersion: "1" },
        ])(
            "rejects an old or invalid acknowledgement %j before creating/resuming",
            async (acknowledgement) => {
                const create = vi.fn();
                const resume = vi.fn();
                const runtime = await runtimePeer((connection) => {
                    connection.onRequest("registerExtensionLaunchProvider", () => acknowledgement);
                    connection.onRequest("session.create", create);
                    connection.onRequest("session.resume", resume);
                });
                const client = runtime.client({ extensionLaunchProvider: grant });
                const result =
                    operation === "start"
                        ? client.start()
                        : operation === "create"
                          ? client.createSession({})
                          : client.resumeSession("unit-session", {});
                await expect(result).rejects.toThrow("requires contract version 1");
                expect(create).not.toHaveBeenCalled();
                expect(resume).not.toHaveBeenCalled();
                expect(() => client.rpc).toThrow("not connected");
            }
        );
    });

    it.each([ErrorCodes.MethodNotFound, -32001])(
        "preserves registration error %s without a fallback",
        async (code) => {
            const runtime = await runtimePeer((connection) => {
                connection.onRequest(
                    "registerExtensionLaunchProvider",
                    () =>
                        new ResponseError(code, "registration refused", { owner: "another-client" })
                );
            });
            const client = runtime.client({ extensionLaunchProvider: grant });
            await expect(client.start()).rejects.toMatchObject({
                code,
                message: "registration refused",
                data: { owner: "another-client" },
            });
            expect(() => client.rpc).toThrow("not connected");
        }
    );

    it("gates overlapping start/create/resume calls and registers once per connection", async () => {
        const entered = deferred<void>();
        const acknowledgement = deferred<ExtensionLaunchProviderRegistrationResult>();
        const register = vi.fn(() => {
            entered.resolve();
            return acknowledgement.promise;
        });
        const create = vi.fn((params: { sessionId: string }) => ({ sessionId: params.sessionId }));
        const resume = vi.fn((params: { sessionId: string }) => ({ sessionId: params.sessionId }));
        const runtime = await runtimePeer((connection) => {
            connection.onRequest("registerExtensionLaunchProvider", register);
            connection.onRequest("session.create", create);
            connection.onRequest("session.resume", resume);
        });
        const client = runtime.client({ extensionLaunchProvider: grant });
        const start = client.start();
        await entered.promise;
        const creating = client.createSession({ sessionId: "created" });
        const resuming = client.resumeSession("resumed", {});
        await setTimeout(20);
        expect(create).not.toHaveBeenCalled();
        expect(resume).not.toHaveBeenCalled();
        acknowledgement.resolve({ contractVersion: 1 });
        await Promise.all([start, client.start(), creating, resuming]);
        await expect(client.rpc.registerExtensionLaunchProvider()).resolves.toEqual({
            contractVersion: 1,
        });
        await expect(client.rpc.registerExtensionLaunchProvider()).resolves.toEqual({
            contractVersion: 1,
        });
        expect(register).toHaveBeenCalledTimes(1);
        expect(create).toHaveBeenCalledTimes(1);
        expect(resume).toHaveBeenCalledTimes(1);
    });

    it("attaches before registration but does not approve before acknowledgement", async () => {
        const resolve = vi.fn(grant.resolve);
        const runtime = await runtimePeer((connection) => {
            connection.onRequest("registerExtensionLaunchProvider", async () => {
                await expect(
                    connection.sendRequest("extensionLaunchProvider.resolve", candidate)
                ).rejects.toThrow("has not been acknowledged");
                return { contractVersion: 1 };
            });
        });
        const client = runtime.client({ extensionLaunchProvider: { resolve } });
        await client.start();
        expect(resolve).not.toHaveBeenCalled();
        await expect(
            runtime.peers[0].connection.sendRequest("extensionLaunchProvider.resolve", candidate)
        ).resolves.toEqual({ launch: profile });
        expect(resolve).toHaveBeenCalledTimes(1);
    });

    it.each<ExtensionSource>(["project", "user", "plugin", "session"])(
        "preserves %s source, path, identity, context and opaque launch recipe",
        async (source) => {
            const request: ExtensionLaunchProviderResolveRequest = {
                ...candidate,
                id: `${source}:fixture`,
                source,
            };
            const resolve = vi.fn(grant.resolve);
            const runtime = await runtimePeer();
            const client = runtime.client({ extensionLaunchProvider: { resolve } });
            await client.start();
            await expect(
                runtime.peers[0].connection.sendRequest("extensionLaunchProvider.resolve", request)
            ).resolves.toEqual({ launch: profile });
            expect(resolve.mock.calls[0][0]).toEqual(request);
            expect(resolve.mock.calls[0][1]?.isCancellationRequested).toBe(false);
        }
    );

    it("does not invent optional session or bootstrap context", async () => {
        const request: ExtensionLaunchProviderResolveRequest = {
            id: candidate.id,
            name: candidate.name,
            modulePath: candidate.modulePath,
            source: candidate.source,
        };
        const resolve = vi.fn(async () => ({}));
        const runtime = await runtimePeer();
        const client = runtime.client({ extensionLaunchProvider: { resolve } });
        await client.start();
        await expect(
            runtime.peers[0].connection.sendRequest("extensionLaunchProvider.resolve", request)
        ).resolves.toEqual({});
        expect(resolve).toHaveBeenCalledWith(request, expect.anything());
    });

    it.each<ExtensionLaunchProviderResolveResult>([{}, { launch: null }])(
        "preserves an explicit denial %j",
        async (denial) => {
            const runtime = await runtimePeer();
            const client = runtime.client({
                extensionLaunchProvider: { resolve: async () => denial },
            });
            await client.start();
            await expect(
                runtime.peers[0].connection.sendRequest(
                    "extensionLaunchProvider.resolve",
                    candidate
                )
            ).resolves.toEqual(denial);
        }
    );

    it("preserves callback error codes and data, with no success-shaped fallback", async () => {
        const runtime = await runtimePeer();
        const client = runtime.client({
            extensionLaunchProvider: {
                resolve: () => {
                    throw new ResponseError(-32005, "source approval failed", {
                        stage: "revision",
                    });
                },
            },
        });
        await client.start();
        await expect(
            runtime.peers[0].connection.sendRequest("extensionLaunchProvider.resolve", candidate)
        ).rejects.toMatchObject({
            code: -32005,
            message: "source approval failed",
            data: { stage: "revision" },
        });
    });

    it.each<"stop" | "forceStop">(["stop", "forceStop"])(
        "observes cancellation when a resolver synchronously calls %s and throws",
        async (operation) => {
            const runtime = await runtimePeer();
            const failure = new Error("synchronous provider failure");
            let stopping: Promise<void | Error[]> | undefined;
            let cancelledSynchronously: boolean | undefined;
            const resolve = vi.fn<ExtensionLaunchProviderHandler["resolve"]>((_request, token) => {
                stopping = client[operation]();
                cancelledSynchronously = token?.isCancellationRequested;
                throw failure;
            });
            const client = runtime.client({ extensionLaunchProvider: { resolve } });
            await client.start();
            await expect(
                runtime.peers[0].connection.sendRequest(
                    "extensionLaunchProvider.resolve",
                    candidate
                )
            ).rejects.toBeInstanceOf(Error);
            expect(resolve).toHaveBeenCalledTimes(1);
            if (!stopping) throw new Error("The resolver did not initiate shutdown");
            expect(await stopping).toEqual(operation === "stop" ? [] : undefined);
            expect(cancelledSynchronously).toBe(true);
            await setTimeout(0);
        }
    );

    it("cancels in-flight resolution and never reuses a late grant", async () => {
        const entered = deferred<void>();
        const lateGrant = deferred<ExtensionLaunchProviderResolveResult>();
        let observedToken: CancellationToken | undefined;
        const resolve = vi.fn<ExtensionLaunchProviderHandler["resolve"]>(
            async (_request, token) => {
                observedToken = token;
                entered.resolve();
                return lateGrant.promise;
            }
        );
        const runtime = await runtimePeer();
        const client = runtime.client({ extensionLaunchProvider: { resolve } });
        await client.start();
        const cancellation = new CancellationTokenSource();
        onTestFinished(() => cancellation.dispose());
        const request = runtime.peers[0].connection.sendRequest(
            "extensionLaunchProvider.resolve",
            candidate,
            cancellation.token
        );
        await entered.promise;
        cancellation.cancel();
        await expect(request).rejects.toMatchObject({ code: -32800 });
        expect(observedToken?.isCancellationRequested).toBe(true);
        lateGrant.resolve({ launch: profile });
        await expect(
            runtime.peers[0].connection.sendRequest("extensionLaunchProvider.resolve", candidate)
        ).resolves.toEqual({ launch: profile });
        expect(resolve).toHaveBeenCalledTimes(2);
    });

    it.each(["stop", "forceStop", "disconnect"])(
        "%s cancels outstanding grants and reconnects with a fresh registration",
        async (operation) => {
            const entered = deferred<void>();
            const lateGrant = deferred<ExtensionLaunchProviderResolveResult>();
            const register = vi.fn(() => ({ contractVersion: 1 }));
            let observedToken: CancellationToken | undefined;
            const resolve = vi.fn<ExtensionLaunchProviderHandler["resolve"]>(
                async (_request, token) => {
                    observedToken = token;
                    entered.resolve();
                    return lateGrant.promise;
                }
            );
            const runtime = await runtimePeer((connection) => {
                connection.onRequest("registerExtensionLaunchProvider", register);
            });
            const client = runtime.client({ extensionLaunchProvider: { resolve } });
            await client.start();
            const originalRpc = client.rpc;
            const pending = runtime.peers[0].connection.sendRequest(
                "extensionLaunchProvider.resolve",
                candidate
            );
            const rejected = expect(pending).rejects.toBeInstanceOf(Error);
            await entered.promise;
            if (operation === "stop") {
                expect(await client.stop()).toEqual([]);
            } else if (operation === "forceStop") {
                await client.forceStop();
            } else {
                runtime.peers[0].socket.destroy();
            }
            await rejected;
            await expect.poll(() => observedToken?.isCancellationRequested).toBe(true);
            lateGrant.resolve({ launch: profile });
            await expect(originalRpc.registerExtensionLaunchProvider()).rejects.toMatchObject({
                code: -32800,
            });
            await client.start();
            expect(register).toHaveBeenCalledTimes(2);
            expect(resolve).toHaveBeenCalledTimes(1);
            await expect(
                runtime.peers[1].connection.sendRequest(
                    "extensionLaunchProvider.resolve",
                    candidate
                )
            ).resolves.toEqual({ launch: profile });
            expect(resolve).toHaveBeenCalledTimes(2);
        }
    );

    it("surfaces a shared runtime's refusal to replace a disconnected provider", async () => {
        let registrations = 0;
        const create = vi.fn();
        const runtime = await runtimePeer((connection) => {
            connection.onRequest("registerExtensionLaunchProvider", () => {
                if (++registrations === 1) return { contractVersion: 1 };
                return new ResponseError(
                    -32603,
                    "Another client is already the extension launch provider."
                );
            });
            connection.onRequest("session.create", create);
        });
        const client = runtime.client({ extensionLaunchProvider: grant });
        await client.start();
        expect(await client.stop()).toEqual([]);
        await expect(client.createSession({})).rejects.toThrow(
            "already the extension launch provider"
        );
        expect(registrations).toBe(2);
        expect(create).not.toHaveBeenCalled();
        expect(() => client.rpc).toThrow("not connected");
    });

    it("stopping during negotiation rejects startup instead of accepting a late acknowledgement", async () => {
        const entered = deferred<void>();
        const acknowledgement = deferred<ExtensionLaunchProviderRegistrationResult>();
        const runtime = await runtimePeer((connection) => {
            connection.onRequest("registerExtensionLaunchProvider", () => {
                entered.resolve();
                return acknowledgement.promise;
            });
        });
        const client = runtime.client({ extensionLaunchProvider: grant });
        const starting = client.start();
        const rejected = expect(starting).rejects.toBeInstanceOf(Error);
        await entered.promise;
        expect(await client.stop()).toEqual([]);
        await rejected;
        acknowledgement.resolve({ contractVersion: 1 });
        expect(() => client.rpc).toThrow("not connected");
    });
});

describe("extension launch cancellation adapter", () => {
    it("disposes an unused connection repeatedly and refuses later registration", async () => {
        const register = vi.fn<() => Promise<ExtensionLaunchProviderRegistrationResult>>(
            async () => ({ contractVersion: 1 })
        );
        const connection = new ExtensionLaunchProviderConnection(grant, register);
        onTestFinished(() => connection.dispose());
        connection.dispose();
        connection.dispose();
        await expect(connection.register()).rejects.toMatchObject({ code: -32800 });
        await expect(connection.register()).rejects.toMatchObject({ code: -32800 });
        expect(register).not.toHaveBeenCalled();
    });

    it("handles synchronous overlapping wire and lifetime cancellation before callback entry", async () => {
        const resolve = vi.fn(grant.resolve);
        const connection = new ExtensionLaunchProviderConnection({ resolve }, async () => ({
            contractVersion: 1,
        }));
        onTestFinished(() => connection.dispose());
        await connection.register();
        const subscriptionDisposed = vi.fn();
        const token: CancellationToken = {
            isCancellationRequested: true,
            onCancellationRequested(listener) {
                listener(undefined);
                connection.dispose();
                listener(undefined);
                return { dispose: subscriptionDisposed };
            },
        };
        await expect(connection.handler.resolve(candidate, token)).rejects.toMatchObject({
            code: -32800,
        });
        expect(resolve).not.toHaveBeenCalled();
        expect(subscriptionDisposed).toHaveBeenCalledTimes(1);
        await expect(connection.register()).rejects.toMatchObject({ code: -32800 });
    });

    it("preserves synchronous invocation and the original error when the resolver disposes then throws", async () => {
        const failure = new Error("synchronous provider failure");
        let entered = false;
        const connection = new ExtensionLaunchProviderConnection(
            {
                resolve() {
                    entered = true;
                    connection.dispose();
                    throw failure;
                },
            },
            async () => ({ contractVersion: 1 })
        );
        onTestFinished(() => connection.dispose());
        await connection.register();
        const resolving = connection.handler.resolve(candidate);
        expect(entered).toBe(true);
        await expect(resolving).rejects.toBe(failure);
        await setTimeout(0);
    });
});

describe("public launch provider cancellation lifecycle", () => {
    it.each(["start", "create", "resume"])(
        "%s preserves handshake failures before provider registration and supports repeated cleanup",
        async (operation) => {
            const diagnostics = vi.spyOn(console, "error");
            onTestFinished(() => diagnostics.mockRestore());
            let rejectHandshake = true;
            const registration = vi.fn(() => ({ contractVersion: 1 }));
            const resolve = vi.fn(grant.resolve);
            const runtime = await runtimePeer((connection) => {
                connection.onRequest("connect", () =>
                    rejectHandshake
                        ? new ResponseError(-32041, "synthetic handshake failure", {
                              phase: "before-registration",
                          })
                        : { protocolVersion: 3 }
                );
                connection.onRequest("registerExtensionLaunchProvider", registration);
            });
            const client = runtime.client({ extensionLaunchProvider: { resolve } });
            const operationResult =
                operation === "start"
                    ? client.start()
                    : operation === "create"
                      ? client.createSession({})
                      : client.resumeSession("unit-session", {});
            await expect(operationResult).rejects.toMatchObject({
                code: -32041,
                message: "synthetic handshake failure",
                data: { phase: "before-registration" },
            });
            expect(registration).not.toHaveBeenCalled();
            expect(resolve).not.toHaveBeenCalled();
            expect(() => client.rpc).toThrow("not connected");
            await client.forceStop();
            expect(await client.stop()).toEqual([]);
            await client.forceStop();
            expect(diagnostics).not.toHaveBeenCalled();

            rejectHandshake = false;
            await client.start();
            expect(registration).toHaveBeenCalledTimes(1);
            expect(resolve).not.toHaveBeenCalled();
            expect(await client.stop()).toEqual([]);
            expect(await client.stop()).toEqual([]);
            await client.forceStop();
            expect(diagnostics).not.toHaveBeenCalled();
        }
    );

    it.each(["stop", "forceStop", "disconnect"])(
        "%s before handshake completion safely cancels an unused provider lifetime",
        async (operation) => {
            const diagnostics = vi.spyOn(console, "error");
            onTestFinished(() => diagnostics.mockRestore());
            const entered = deferred<void>();
            const handshake = deferred<{ protocolVersion: number }>();
            const registration = vi.fn(() => ({ contractVersion: 1 }));
            const runtime = await runtimePeer((connection) => {
                connection.onRequest("connect", () => {
                    entered.resolve();
                    return handshake.promise;
                });
                connection.onRequest("registerExtensionLaunchProvider", registration);
            });
            const client = runtime.client({ extensionLaunchProvider: grant });
            const starting = client.start();
            const failure = starting.catch((error: unknown) => error);
            await entered.promise;
            const originalRpc = client.rpc;
            if (operation === "disconnect") {
                runtime.peers[0].socket.destroy();
                await expect
                    .poll(() => {
                        try {
                            return client.rpc;
                        } catch (error) {
                            return error;
                        }
                    })
                    .toBeInstanceOf(Error);
                await client.forceStop();
            } else if (operation === "forceStop") {
                await client.forceStop();
            } else {
                expect(await client.stop()).toEqual([]);
            }
            await expect(failure).resolves.toMatchObject({
                code: ErrorCodes.PendingResponseRejected,
            });
            handshake.resolve({ protocolVersion: 3 });
            await expect(originalRpc.registerExtensionLaunchProvider()).rejects.toMatchObject({
                code: -32800,
            });
            expect(registration).not.toHaveBeenCalled();
            expect(await client.stop()).toEqual([]);
            await client.forceStop();
            expect(diagnostics).not.toHaveBeenCalled();
        }
    );

    it.each(["stop", "forceStop", "disconnect"])(
        "overlapping wire cancellation and %s notify once and cannot replay a late grant",
        async (operation) => {
            const entered = deferred<void>();
            const late = deferred<ExtensionLaunchProviderResolveResult>();
            const notified = deferred<void>();
            const diagnostics = vi.spyOn(console, "error");
            onTestFinished(() => diagnostics.mockRestore());
            const registration = vi.fn(() => ({ contractVersion: 1 }));
            const runtime = await runtimePeer((connection) => {
                connection.onRequest("registerExtensionLaunchProvider", registration);
            });
            const freshProfile: ExtensionLaunchProfile = {
                ...profile,
                args: [...profile.args, "--fresh-resolution"],
            };
            let notifications = 0;
            let stopping: Promise<PromiseSettledResult<void | Error[]>[]> | undefined;
            const resolve = vi
                .fn<ExtensionLaunchProviderHandler["resolve"]>()
                .mockImplementationOnce(async (_request, token) => {
                    if (!token) throw new Error("Expected the public cancellation token");
                    token.onCancellationRequested(() => {
                        notifications++;
                        if (operation === "disconnect") {
                            runtime.peers[0].socket.destroy();
                            stopping = Promise.resolve([]);
                        } else {
                            stopping = Promise.allSettled([
                                operation === "stop" ? client.stop() : client.forceStop(),
                            ]);
                        }
                        notified.resolve();
                    });
                    entered.resolve();
                    return late.promise;
                })
                .mockResolvedValue({ launch: freshProfile });
            const client = runtime.client({ extensionLaunchProvider: { resolve } });
            await client.start();
            const oldRpc = client.rpc;
            const wireCancellation = new CancellationTokenSource();
            onTestFinished(() => wireCancellation.dispose());
            const pending = runtime.peers[0].connection.sendRequest(
                "extensionLaunchProvider.resolve",
                candidate,
                wireCancellation.token
            );
            const failed = expect(pending).rejects.toBeInstanceOf(Error);
            await entered.promise;
            wireCancellation.cancel();
            wireCancellation.cancel();
            await notified.promise;
            if (!stopping) throw new Error("Cancellation did not initiate connection teardown");
            for (const result of await stopping) {
                expect(result.status).toBe("fulfilled");
                if (result.status === "fulfilled") {
                    expect(result.value).toEqual(operation === "stop" ? [] : undefined);
                }
            }
            await failed;
            await client.forceStop();
            expect(await client.stop()).toEqual([]);
            expect(notifications).toBe(1);
            late.resolve({ launch: profile });
            await expect(oldRpc.registerExtensionLaunchProvider()).rejects.toMatchObject({
                code: -32800,
            });
            await client.start();
            expect(registration).toHaveBeenCalledTimes(2);
            expect(resolve).toHaveBeenCalledTimes(1);
            await expect(
                runtime.peers[1].connection.sendRequest(
                    "extensionLaunchProvider.resolve",
                    candidate
                )
            ).resolves.toEqual({ launch: freshProfile });
            expect(resolve).toHaveBeenCalledTimes(2);
            expect(diagnostics).not.toHaveBeenCalled();
        }
    );
});

describe("public no-turn retention bindings", () => {
    it("retains reentrantly before create returns and delivers the early retained event", async () => {
        let createReturned = false;
        const events: SessionEvent[] = [];
        const retained: SessionRetainRequest[] = [];
        const runtime = await runtimePeer((connection) => {
            connection.onRequest("session.retain", async (params: SessionRetainRequest) => {
                retained.push(params);
                const event: RetainedEvent = {
                    type: "session.retained",
                    id: randomUUID(),
                    timestamp: new Date().toISOString(),
                    parentId: null,
                    data: {},
                };
                await connection.sendNotification("session.event", {
                    sessionId: params.sessionId,
                    event,
                });
                return null;
            });
            connection.onRequest("session.create", async (params: { sessionId: string }) => {
                await expect(
                    connection.sendRequest("extensionLaunchProvider.resolve", {
                        ...candidate,
                        sessionId: params.sessionId,
                    })
                ).resolves.toEqual({ launch: profile });
                return { sessionId: params.sessionId };
            });
        });
        const client = runtime.client({
            extensionLaunchProvider: {
                async resolve(request) {
                    expect(createReturned).toBe(false);
                    if (!request.sessionId)
                        throw new Error("Expected actual runtime session correlation");
                    const result = await client.rpc.session.retain({
                        sessionId: request.sessionId,
                    });
                    expectTypeOf(result).toEqualTypeOf<null>();
                    expect(result).toBeNull();
                    return { launch: request.defaultLaunch };
                },
            },
        });
        const session = await client.createSession({ onEvent: (event) => events.push(event) });
        createReturned = true;
        expectTypeOf<Awaited<ReturnType<typeof session.rpc.retain>>>().toEqualTypeOf<null>();
        expectTypeOf<
            Parameters<typeof client.rpc.session.retain>[0]
        >().toEqualTypeOf<SessionRetainRequest>();
        expect(events.map((event) => event.type)).toEqual(["session.retained"]);
        expect(retained).toEqual([{ sessionId: session.sessionId }]);
        await expect(session.rpc.retain()).resolves.toBeNull();
        expect(retained).toEqual([
            { sessionId: session.sessionId },
            { sessionId: session.sessionId },
        ]);
    });

    it.each([
        [ErrorCodes.MethodNotFound, "unsupported"],
        [-32001, "persistence unavailable"],
        [-32002, "writer flush failed"],
        [-32800, "retention cancelled"],
    ])("propagates %s (%s) from both bindings", async (code, message) => {
        const retain = vi.fn(() => new ResponseError(code, message, { operation: "retain" }));
        const runtime = await runtimePeer((connection) => {
            connection.onRequest("session.retain", retain);
        });
        const client = runtime.client();
        const session = await client.createSession({});
        await expect(
            client.rpc.session.retain({ sessionId: session.sessionId })
        ).rejects.toMatchObject({
            code,
            message,
            data: { operation: "retain" },
        });
        await expect(session.rpc.retain()).rejects.toMatchObject({
            code,
            message,
            data: { operation: "retain" },
        });
        expect(retain).toHaveBeenCalledTimes(2);
    });

    it("rejects connection loss during retention and never retries the effect", async () => {
        const entered = deferred<void>();
        const flush = deferred<null>();
        const retain = vi.fn(() => {
            entered.resolve();
            return flush.promise;
        });
        const runtime = await runtimePeer((connection) => {
            connection.onRequest("session.retain", retain);
        });
        const client = runtime.client();
        await client.start();
        const pending = client.rpc.session.retain({ sessionId: "unit-session" });
        const rejected = expect(pending).rejects.toBeInstanceOf(Error);
        await entered.promise;
        await client.forceStop();
        await rejected;
        flush.resolve(null);
        await client.start();
        expect(retain).toHaveBeenCalledTimes(1);
    });
});
