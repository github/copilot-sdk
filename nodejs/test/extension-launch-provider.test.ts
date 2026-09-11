/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { PassThrough } from "node:stream";
import { createServer, type Server, type Socket } from "node:net";
import {
    createMessageConnection,
    ErrorCodes,
    type MessageConnection,
    StreamMessageReader,
    StreamMessageWriter,
} from "vscode-jsonrpc/node.js";
import { afterEach, describe, expect, it, onTestFinished, vi } from "vitest";
import {
    approveAll,
    CopilotClient,
    RuntimeConnection,
    type ExtensionLaunchProvider,
    type ExtensionLaunchProviderResolveRequest,
} from "../src/index.js";
import { registerClientGlobalApiHandlers } from "../src/generated/rpc.js";

// This file is for unit tests. Where relevant, prefer to add e2e tests in e2e/*.test.ts instead.
//
// These tests exercise the public `extensionLaunchProvider` client option end to end over a
// real TCP socket (no CLI subprocess involved): a hand-scripted fake server plays the runtime
// side of the wire protocol so we can assert exact request ordering, dispatch, and error
// semantics without depending on a real Copilot CLI binary being present.

const sampleRequest: ExtensionLaunchProviderResolveRequest = {
    id: "project:legacy-extension",
    name: "Legacy extension",
    modulePath: "/extensions/legacy/index.js",
    source: "project",
    sessionId: "session-1",
    defaultLaunch: {
        executable: "/usr/bin/node",
        args: ["/runtime/extension-bootstrap.mjs"],
        env: { EXTENSION_PATH: "/extensions/legacy/index.js" },
    },
};

function deferred<T>() {
    let resolve!: (value: T) => void;
    const promise = new Promise<T>((complete) => {
        resolve = complete;
    });
    return { promise, resolve };
}

/** Minimal fake runtime server: answers `connect` and lets a test script the rest. */
class FakeRuntimeServer {
    private server: Server;
    private connections: MessageConnection[] = [];
    private sockets: Socket[] = [];
    private port = 0;

    private constructor(server: Server) {
        this.server = server;
    }

    static async start(): Promise<FakeRuntimeServer> {
        const server = createServer();
        await new Promise<void>((resolve) => server.listen(0, "127.0.0.1", resolve));
        const fake = new FakeRuntimeServer(server);
        const address = server.address();
        if (address && typeof address === "object") {
            fake.port = address.port;
        }
        return fake;
    }

    get url(): string {
        return `localhost:${this.port}`;
    }

    /** Accepts the next incoming connection and wires it as a JSON-RPC peer, replying to `connect`. */
    async acceptOne(): Promise<MessageConnection> {
        const socket = await new Promise<Socket>((resolve) => {
            this.server.once("connection", resolve);
        });
        this.sockets.push(socket);
        const connection = createMessageConnection(
            new StreamMessageReader(socket),
            new StreamMessageWriter(socket)
        );
        this.connections.push(connection);
        connection.onRequest("connect", async () => ({
            ok: true,
            protocolVersion: 3,
            version: "test",
        }));
        connection.listen();
        return connection;
    }

    async close(): Promise<void> {
        for (const connection of this.connections) {
            connection.dispose();
        }
        for (const socket of this.sockets) {
            socket.destroy();
        }
        await new Promise<void>((resolve) => this.server.close(() => resolve()));
    }
}

describe("extensionLaunchProvider client option", () => {
    let servers: FakeRuntimeServer[] = [];

    afterEach(async () => {
        await Promise.all(servers.map((s) => s.close()));
        servers = [];
    });

    it("registers no handler and sends no registration request when omitted (backward compatible)", async () => {
        const server = await FakeRuntimeServer.start();
        servers.push(server);
        const client = new CopilotClient({ connection: RuntimeConnection.forUri(server.url) });
        onTestFinished(() => client.forceStop());

        const serverConnectionPromise = server.acceptOne();
        const startPromise = client.start();
        const accepted = await serverConnectionPromise;
        const registerSpy = vi.fn();
        accepted.onRequest("registerExtensionLaunchProvider", async () => {
            registerSpy();
            return null;
        });

        await startPromise;

        expect(registerSpy).not.toHaveBeenCalled();
        await expect(
            accepted.sendRequest("extensionLaunchProvider.resolve", sampleRequest)
        ).rejects.toThrow("No extensionLaunchProvider client-global handler registered");
    });

    it("registers the provider before start() resolves and dispatches a resolve request", async () => {
        const server = await FakeRuntimeServer.start();
        servers.push(server);
        const calls: ExtensionLaunchProviderResolveRequest[] = [];
        const provider: ExtensionLaunchProvider = (request) => {
            calls.push(request);
            return { launch: { executable: "/app/copilot", args: ["run"], env: {} } };
        };
        const client = new CopilotClient({
            connection: RuntimeConnection.forUri(server.url),
            extensionLaunchProvider: provider,
        });
        onTestFinished(() => client.forceStop());

        const serverConnectionPromise = server.acceptOne();
        const startPromise = client.start();
        const serverConnection = await serverConnectionPromise;

        let registered = false;
        serverConnection.onRequest("registerExtensionLaunchProvider", async () => {
            registered = true;
            return { contractVersion: 1 };
        });

        // `start()` only resolves once the registration round trip completes, so by
        // the time it resolves the provider is guaranteed to be wired up.
        await startPromise;
        expect(registered).toBe(true);

        const resolveResult = await serverConnection.sendRequest(
            "extensionLaunchProvider.resolve",
            sampleRequest
        );
        expect(resolveResult).toEqual({
            launch: { executable: "/app/copilot", args: ["run"], env: {} },
        });
        expect(calls).toEqual([sampleRequest]);
    });

    it.each(["synchronous", "asynchronous"])(
        "propagates a %s callback failure as a JSON-RPC error",
        async (kind) => {
            const server = await FakeRuntimeServer.start();
            servers.push(server);
            const failure = new Error("extension profile lookup failed");
            const client = new CopilotClient({
                connection: RuntimeConnection.forUri(server.url),
                extensionLaunchProvider:
                    kind === "synchronous"
                        ? () => {
                              throw failure;
                          }
                        : async () => {
                              throw failure;
                          },
            });
            onTestFinished(() => client.forceStop());

            const serverConnectionPromise = server.acceptOne();
            const startPromise = client.start();
            const serverConnection = await serverConnectionPromise;
            serverConnection.onRequest("registerExtensionLaunchProvider", async () => ({
                contractVersion: 1,
            }));
            await startPromise;

            await expect(
                serverConnection.sendRequest("extensionLaunchProvider.resolve", sampleRequest)
            ).rejects.toMatchObject({
                code: ErrorCodes.InternalError,
                message: expect.stringContaining("extension profile lookup failed"),
            });
        }
    );

    it.each([{}, { launch: null }])(
        "forwards explicit denial %j without substituting a default launch",
        async (denial) => {
            const server = await FakeRuntimeServer.start();
            servers.push(server);
            const client = new CopilotClient({
                connection: RuntimeConnection.forUri(server.url),
                extensionLaunchProvider: () => denial,
            });
            onTestFinished(() => client.forceStop());

            const serverConnectionPromise = server.acceptOne();
            const startPromise = client.start();
            const serverConnection = await serverConnectionPromise;
            serverConnection.onRequest("registerExtensionLaunchProvider", async () => ({
                contractVersion: 1,
            }));
            await startPromise;

            const result = await serverConnection.sendRequest(
                "extensionLaunchProvider.resolve",
                sampleRequest
            );
            expect(result).toEqual(denial);
        }
    );

    it("re-registers when the same client reconnects instead of reusing prior acknowledgement", async () => {
        const server = await FakeRuntimeServer.start();
        servers.push(server);
        const seen: string[][] = [];

        const client = new CopilotClient({
            connection: RuntimeConnection.forUri(server.url),
            extensionLaunchProvider: () => ({
                launch: { executable: "/copilot", args: [], env: {} },
            }),
        });
        onTestFinished(() => client.forceStop());

        for (let attempt = 0; attempt < 2; attempt++) {
            const serverConnectionPromise = server.acceptOne();
            const startPromise = client.start();
            const serverConnection = await serverConnectionPromise;
            const registrations: string[] = [];
            serverConnection.onRequest("registerExtensionLaunchProvider", async () => {
                registrations.push("registered");
                return { contractVersion: 1 };
            });
            await startPromise;
            seen.push(registrations);
            await client.stop();
        }

        // Each connection independently re-sends its own registration request; the
        // second client's registration is not skipped or merged with the first's.
        expect(seen).toEqual([["registered"], ["registered"]]);
    });

    it("waits for registration before concurrent start, create, and resume calls complete", async () => {
        const server = await FakeRuntimeServer.start();
        servers.push(server);
        const acknowledgement = deferred<{ contractVersion: 1 }>();
        const registering = deferred<void>();
        const calls: string[] = [];
        const client = new CopilotClient({
            connection: RuntimeConnection.forUri(server.url),
            extensionLaunchProvider: (request) => ({ launch: request.defaultLaunch }),
        });
        onTestFinished(() => client.forceStop());

        const accepted = server.acceptOne();
        const starting = client.start();
        const connection = await accepted;
        connection.onRequest("registerExtensionLaunchProvider", async () => {
            calls.push("register");
            expect(
                await connection.sendRequest("extensionLaunchProvider.resolve", sampleRequest)
            ).toEqual({ launch: sampleRequest.defaultLaunch });
            registering.resolve();
            await acknowledgement.promise;
            calls.push("acknowledge");
            return { contractVersion: 1 };
        });
        for (const method of ["session.create", "session.resume"]) {
            connection.onRequest(method, (params: { sessionId: string }) => {
                calls.push(method);
                return { sessionId: params.sessionId };
            });
        }
        await registering.promise;
        const restarting = client.start();
        const creating = client.createSession({
            sessionId: "created",
            onPermissionRequest: approveAll,
        });
        const resuming = client.resumeSession("resumed", {
            onPermissionRequest: approveAll,
        });
        expect(() => client.rpc).toThrow("Call start() first");
        await expect(client.retainSession("not-yet-admitted")).rejects.toThrow(
            "Call start() first"
        );
        await new Promise((resolve) => setImmediate(resolve));
        expect(calls).toEqual(["register"]);
        acknowledgement.resolve({ contractVersion: 1 });

        await Promise.all([starting, restarting, creating, resuming]);
        expect(calls).toEqual(["register", "acknowledge", "session.create", "session.resume"]);
    });

    it("retains by ID over the live connection while create is blocked on the resolver", async () => {
        const server = await FakeRuntimeServer.start();
        servers.push(server);
        const calls: string[] = [];
        let createResolved = false;
        const client = new CopilotClient({
            connection: RuntimeConnection.forUri(server.url),
            extensionLaunchProvider: async (request) => {
                if (!request.sessionId) {
                    return { launch: null };
                }
                expect(createResolved).toBe(false);
                await client.retainSession(request.sessionId);
                calls.push("approve");
                return { launch: request.defaultLaunch };
            },
        });
        onTestFinished(() => client.forceStop());
        const accepted = server.acceptOne();
        const starting = client.start();
        const connection = await accepted;
        connection.onRequest("registerExtensionLaunchProvider", () => ({ contractVersion: 1 }));
        connection.onRequest("session.retain", (params: { sessionId: string }) => {
            calls.push(`retain:${params.sessionId}`);
            return null;
        });
        connection.onRequest("session.create", async (params: { sessionId: string }) => {
            calls.push("create");
            const result = await connection.sendRequest("extensionLaunchProvider.resolve", {
                ...sampleRequest,
                sessionId: params.sessionId,
            });
            expect(result).toEqual({ launch: sampleRequest.defaultLaunch });
            calls.push("created");
            return { sessionId: params.sessionId };
        });
        await starting;
        // No local CopilotSession is needed, even for an ID unknown to this SDK.
        await expect(client.retainSession("existing-runtime-session")).resolves.toBeUndefined();
        const created = await client.createSession({
            sessionId: "new-session",
            onPermissionRequest: approveAll,
        });
        createResolved = true;
        expect(created.sessionId).toBe("new-session");
        expect(calls).toEqual([
            "retain:existing-runtime-session",
            "create",
            "retain:new-session",
            "approve",
            "created",
        ]);
    });

    it("rejects empty IDs and disconnected retention without starting the client", async () => {
        const client = new CopilotClient({
            connection: RuntimeConnection.forUri("localhost:1"),
        });
        onTestFinished(() => client.forceStop());
        const start = vi.spyOn(client, "start");
        await expect(client.retainSession("")).rejects.toThrow("non-empty string");
        await expect(client.retainSession("session-1")).rejects.toThrow("Call start() first");
        expect(start).not.toHaveBeenCalled();
    });

    it("propagates retain-by-ID errors and rejects pending retention on shutdown", async () => {
        const server = await FakeRuntimeServer.start();
        servers.push(server);
        const client = new CopilotClient({
            connection: RuntimeConnection.forUri(server.url),
            extensionLaunchProvider: () => ({ launch: null }),
        });
        onTestFinished(() => client.forceStop());
        const accepted = server.acceptOne();
        const starting = client.start();
        const connection = await accepted;
        const retaining = deferred<void>();
        connection.onRequest("registerExtensionLaunchProvider", () => ({ contractVersion: 1 }));
        connection.onRequest("session.retain", (params: { sessionId: string }) => {
            if (params.sessionId === "missing") {
                throw new Error("Session persistence is unavailable");
            }
            retaining.resolve();
            return new Promise<never>(() => {});
        });
        await starting;
        await expect(client.retainSession("missing")).rejects.toThrow(
            "Session persistence is unavailable"
        );
        const pending = expect(client.retainSession("pending")).rejects.toThrow();
        await retaining.promise;
        await client.forceStop();
        await pending;
        await expect(client.retainSession("pending")).rejects.toThrow("Call start() first");
    });

    it.each([
        null,
        {},
        { contractVersion: 0 },
        { contractVersion: 2 },
        { contractVersion: "1" },
        1,
    ])("rejects unsupported registration %j without any session request", async (response) => {
        const server = await FakeRuntimeServer.start();
        servers.push(server);
        const client = new CopilotClient({
            connection: RuntimeConnection.forUri(server.url),
            extensionLaunchProvider: () => ({ launch: null }),
        });
        onTestFinished(() => client.forceStop());
        const accepted = server.acceptOne();
        const starting = client.start();
        const creating = client.createSession({ onPermissionRequest: approveAll });
        const resuming = client.resumeSession("existing", {
            onPermissionRequest: approveAll,
        });
        const settled = Promise.allSettled([starting, creating, resuming]);
        const connection = await accepted;
        const calls: string[] = [];
        connection.onRequest((method) => {
            calls.push(method);
            return response;
        });

        const results = await settled;
        expect(results).toEqual(
            Array.from({ length: 3 }, () => ({
                status: "rejected",
                reason: expect.objectContaining({
                    message: "Extension launch provider requires runtime contractVersion 1.",
                }),
            }))
        );
        expect(calls).toEqual(["registerExtensionLaunchProvider"]);
        expect(() => client.rpc).toThrow("Call start() first");
    });

    it("propagates registration errors and retries only with the provider still configured", async () => {
        const server = await FakeRuntimeServer.start();
        servers.push(server);
        const provider = vi.fn(() => ({ launch: sampleRequest.defaultLaunch }));
        const client = new CopilotClient({
            connection: RuntimeConnection.forUri(server.url),
            extensionLaunchProvider: provider,
        });
        onTestFinished(() => client.forceStop());
        const calls: string[] = [];

        const firstConnection = server.acceptOne();
        const firstStart = client.start();
        const rejected = expect(firstStart).rejects.toThrow("registration unavailable");
        (await firstConnection).onRequest("registerExtensionLaunchProvider", () => {
            calls.push("failed registration");
            throw new Error("registration unavailable");
        });
        await rejected;

        const secondConnection = server.acceptOne();
        const secondStart = client.start();
        const connection = await secondConnection;
        connection.onRequest("registerExtensionLaunchProvider", () => {
            calls.push("new registration");
            return { contractVersion: 1 };
        });
        await secondStart;
        await connection.sendRequest("extensionLaunchProvider.resolve", sampleRequest);
        expect(calls).toEqual(["failed registration", "new registration"]);
        expect(provider).toHaveBeenCalledExactlyOnceWith(sampleRequest);
    });

    it("rejects a replacement runtime that acknowledges an older contract", async () => {
        const server = await FakeRuntimeServer.start();
        servers.push(server);
        const client = new CopilotClient({
            connection: RuntimeConnection.forUri(server.url),
            extensionLaunchProvider: () => ({ launch: null }),
        });
        onTestFinished(() => client.forceStop());
        for (const response of [{ contractVersion: 1 }, null]) {
            const accepted = server.acceptOne();
            const starting = client.start();
            const assertion =
                response === null
                    ? expect(starting).rejects.toThrow("contractVersion 1")
                    : expect(starting).resolves.toBeUndefined();
            (await accepted).onRequest("registerExtensionLaunchProvider", () => response);
            await assertion;
            await client.stop();
        }
    });

    it.each(["stop", "forceStop"] as const)(
        "%s cancels registration instead of admitting a session",
        async (method) => {
            const server = await FakeRuntimeServer.start();
            servers.push(server);
            const registering = deferred<void>();
            const acknowledgement = deferred<{ contractVersion: 1 }>();
            const client = new CopilotClient({
                connection: RuntimeConnection.forUri(server.url),
                extensionLaunchProvider: () => ({ launch: null }),
            });
            onTestFinished(() => client.forceStop());
            const accepted = server.acceptOne();
            const creating = client.createSession({ onPermissionRequest: approveAll });
            const assertion = expect(creating).rejects.toThrow();
            const connection = await accepted;
            connection.onRequest("registerExtensionLaunchProvider", () => {
                registering.resolve();
                return acknowledgement.promise;
            });
            const create = vi.fn();
            connection.onRequest("session.create", create);
            await registering.promise;
            await client[method]();
            await assertion;
            acknowledgement.resolve({ contractVersion: 1 });
            expect(create).not.toHaveBeenCalled();
            expect(() => client.rpc).toThrow("Call start() first");
        }
    );

    it("surfaces the generic client-global-handler error when no provider is registered on a bare connection", async () => {
        const clientToServer = new PassThrough();
        const serverToClient = new PassThrough();
        const clientConn = createMessageConnection(
            new StreamMessageReader(serverToClient),
            new StreamMessageWriter(clientToServer)
        );
        const serverConn = createMessageConnection(
            new StreamMessageReader(clientToServer),
            new StreamMessageWriter(serverToClient)
        );
        onTestFinished(() => {
            clientConn.dispose();
            serverConn.dispose();
        });

        registerClientGlobalApiHandlers(clientConn, {});
        clientConn.listen();
        serverConn.listen();

        await expect(
            serverConn.sendRequest("extensionLaunchProvider.resolve", sampleRequest)
        ).rejects.toThrow("No extensionLaunchProvider client-global handler registered");
    });
});

describe("requestCanvasRenderer does not imply extension launch provider authority", () => {
    it("session-level requestCanvasRenderer leaves the client-global provider unset", async () => {
        const server = await FakeRuntimeServer.start();
        onTestFinished(() => server.close());
        const client = new CopilotClient({
            connection: RuntimeConnection.forUri(server.url),
        });
        onTestFinished(() => client.forceStop());
        const accepted = server.acceptOne();
        const creating = client.createSession({
            requestCanvasRenderer: true,
            onPermissionRequest: approveAll,
        });
        const connection = await accepted;
        const requests: Array<{ method: string; params: object }> = [];
        connection.onRequest((method: string, params: { sessionId: string }) => {
            requests.push({ method, params });
            return { sessionId: params.sessionId };
        });
        await creating;
        expect(requests).toEqual([
            {
                method: "session.create",
                params: expect.objectContaining({ requestCanvasRenderer: true }),
            },
        ]);
        await expect(
            connection.sendRequest("extensionLaunchProvider.resolve", sampleRequest)
        ).rejects.toThrow("No extensionLaunchProvider client-global handler registered");
    });
});
