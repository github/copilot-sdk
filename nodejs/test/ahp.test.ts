/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/* eslint-disable @typescript-eslint/no-explicit-any */
import { PassThrough } from "node:stream";
import { describe, expect, it, onTestFinished, vi } from "vitest";
import {
    CancellationToken,
    CancellationTokenSource,
    createMessageConnection,
    ResponseError,
    StreamMessageReader,
    StreamMessageWriter,
} from "vscode-jsonrpc/node.js";
import { CopilotClient, RuntimeConnection, type AhpEndpointOptions } from "../src/index.js";

function deferred<T = void>() {
    let resolve!: (value: T) => void;
    let reject!: (error: Error) => void;
    const promise = new Promise<T>((yes, no) => {
        resolve = yes;
        reject = no;
    });
    return { promise, resolve, reject };
}

function harness() {
    const toServer = new PassThrough();
    const toClient = new PassThrough();
    const sdk = createMessageConnection(
        new StreamMessageReader(toClient),
        new StreamMessageWriter(toServer)
    );
    const server = createMessageConnection(
        new StreamMessageReader(toServer),
        new StreamMessageWriter(toClient)
    );
    const client = new CopilotClient({
        connection: RuntimeConnection.forUri("localhost:1234"),
    });
    (client as any).connection = sdk;
    (client as any).attachConnectionHandlers();
    const register = vi.fn((_params: unknown) => ({ endpointId: "endpoint-1" }));
    const close = vi.fn(() => null);
    const dispose = vi.fn(() => null);
    const send = vi.fn(() => null);
    let nextConnection = 0;
    server.onRequest("ahp.registerEndpoint", register);
    server.onRequest("ahp.openConnection", () => ({
        connectionId: `connection-${++nextConnection}`,
    }));
    server.onRequest("ahp.closeConnection", close);
    server.onRequest("ahp.disposeEndpoint", dispose);
    server.onRequest("ahp.send", send);
    server.onRequest("ahp.refreshExposure", () => null);
    server.onRequest("ahp.setCapabilities", () => null);
    server.onRequest("ping", () => ({ message: "pong" }));
    sdk.listen();
    server.listen();
    onTestFinished(async () => {
        await client.forceStop();
        server.dispose();
        toClient.destroy();
        toServer.destroy();
    });
    const message = (connectionId: string, text: string, token = CancellationToken.None) =>
        server.sendRequest<null>(
            "ahp.message",
            {
                endpointId: "endpoint-1",
                connectionId,
                message: text,
            },
            token
        );
    return { client, server, sdk, register, close, dispose, send, message, toClient };
}

describe("native AHP endpoint opaque bridge", () => {
    it("reinstalls manual handlers after generated connection setup on reconnect", async () => {
        const h = harness();
        const old = await h.client.createAhpEndpoint({ onListSessions: () => [] });
        const onClose = vi.fn();
        await old.openConnection({ onMessage: () => undefined, onClose });
        (h.client as any).attachConnectionHandlers();
        expect(onClose).toHaveBeenCalledWith(
            expect.objectContaining({ message: "Runtime connection replaced" })
        );
        // Stand in for regenerated wrappers that do not forward CancellationToken.
        h.sdk.onRequest("ahp.listSessions", () => {
            throw new Error("No generated AHP callback handler");
        });
        const next = await h.client.createAhpEndpoint({
            onListSessions: ({ signal }) => {
                expect(signal).toBeInstanceOf(AbortSignal);
                return [{ sessionId: "after-reconnect" }];
            },
        });
        await expect(
            h.server.sendRequest("ahp.listSessions", { endpointId: next.id })
        ).resolves.toEqual({ sessionIds: ["after-reconnect"] });
        await old.dispose();
        await next.dispose();
        expect(h.dispose).toHaveBeenCalledOnce();
    });

    it("registers only flags and wire options; callbacks can call ordinary SDK without deadlock", async () => {
        const h = harness();
        const callback = vi.fn(async () => {
            expect(await h.client.ping()).toMatchObject({ message: "pong" });
            return { sessionId: "chosen" };
        });
        const endpoint = await h.client.createAhpEndpoint({
            onCreateSession: callback,
            allowSessionCreation: false,
            capabilities: { opaque: ["value"] },
        });
        await endpoint.openConnection({ onMessage: () => undefined });
        expect(h.register).toHaveBeenCalledWith(
            {
                callbacks: {
                    createSession: true,
                    resumeSession: false,
                    listSessions: false,
                    sessionControl: false,
                },
                allowSessionCreation: false,
                capabilities: { opaque: ["value"] },
            },
            expect.anything()
        );
        await expect(
            h.server.sendRequest("ahp.createSession", {
                endpointId: "endpoint-1",
                connectionId: "connection-1",
                requestedSessionId: "requested",
                config: { opaque: true },
            })
        ).resolves.toEqual({ sessionId: "chosen" });
        expect(callback).toHaveBeenCalledWith(
            {
                connectionId: "connection-1",
                requestedSessionId: "requested",
                config: { opaque: true },
            },
            { signal: expect.any(AbortSignal) }
        );
    });

    it("keeps local session configuration and tools on the ordinary SDK path", async () => {
        const h = harness();
        const tool = vi.fn(() => "safe result");
        const permission = vi.fn(() => ({ kind: "denied-interactively-by-user" as const }));
        const create = vi.fn((params) => ({ sessionId: params.sessionId }));
        h.server.onRequest("session.create", create);
        const config = {
            sessionId: "local",
            name: "Persisted application session",
            systemMessage: { mode: "replace" as const, content: "Distinct application prompt" },
            tools: [{ name: "local_tool", description: "Safe", handler: tool }],
            onPermissionRequest: permission,
        };
        const endpoint = await h.client.createAhpEndpoint({
            onCreateSession: () => h.client.createSession(config),
        });
        await endpoint.openConnection({ onMessage: () => undefined });
        await expect(
            h.server.sendRequest("ahp.createSession", {
                endpointId: "endpoint-1",
                connectionId: "connection-1",
                requestedSessionId: "requested",
            })
        ).resolves.toEqual({ sessionId: "local" });
        expect(create).toHaveBeenCalledTimes(1);
        const wire = create.mock.calls[0][0];
        expect(wire.name).toBe(config.name);
        expect(wire.systemMessage).toEqual(config.systemMessage);
        expect(wire.tools[0]).toMatchObject({ name: "local_tool" });
        expect(wire.tools[0].handler).toBeUndefined();
        expect(config.tools[0].handler).toBe(tool);
        const session = (h.client as any).sessions.get("local");
        expect(session.toolHandlers.get("local_tool")).toBe(tool);
        expect(config.onPermissionRequest).toBe(permission);
    });

    it("maps list, resume and control callbacks without creating a session again", async () => {
        const h = harness();
        const create = vi.fn();
        h.server.onRequest("session.create", create);
        const endpoint = await h.client.createAhpEndpoint({
            onListSessions: () => [{ sessionId: "visible" }],
            onResumeSession: ({ sessionId }) => ({ sessionId }),
            onSessionControl: ({ kind, payload }) => ({ applied: true, result: { kind, payload } }),
        });
        await endpoint.openConnection({ onMessage: () => undefined });
        await expect(
            h.server.sendRequest("ahp.listSessions", { endpointId: "endpoint-1" })
        ).resolves.toEqual({ sessionIds: ["visible"] });
        await expect(
            h.server.sendRequest("ahp.resumeSession", {
                endpointId: "endpoint-1",
                connectionId: "connection-1",
                sessionId: "visible",
            })
        ).resolves.toEqual({ sessionId: "visible" });
        await expect(
            h.server.sendRequest("ahp.sessionControl", {
                endpointId: "endpoint-1",
                sessionId: "visible",
                kind: "custom",
                payload: null,
            })
        ).resolves.toEqual({ applied: true, result: { kind: "custom", payload: null } });
        expect(create).not.toHaveBeenCalled();
    });

    it("propagates callback rejection with no fallback and prevents recursive AHP operations", async () => {
        const h = harness();
        let recursive = false;
        const endpoint = await h.client.createAhpEndpoint({
            onListSessions: async () => {
                if (recursive) await endpoint.refreshExposure();
                throw new Error("Application policy denied");
            },
        });
        await expect(
            h.server.sendRequest("ahp.listSessions", { endpointId: endpoint.id })
        ).rejects.toThrow("Application policy denied");
        recursive = true;
        await expect(
            h.server.sendRequest("ahp.listSessions", { endpointId: endpoint.id })
        ).rejects.toThrow("Recursive AHP");
    });

    it.each(["createSession", "resumeSession"] as const)(
        "rejects an in-transit %s callback when its connection closes before reader dispatch",
        async (method) => {
            const h = harness();
            const sessionRpc = vi.fn((params: { sessionId: string }) => ({
                sessionId: params.sessionId,
            }));
            h.server.onRequest(
                method === "createSession" ? "session.create" : "session.resume",
                sessionRpc
            );
            const callback = vi.fn(() =>
                method === "createSession"
                    ? h.client.createSession({ sessionId: "ghost" })
                    : h.client.resumeSession("ghost", {})
            );
            const endpoint = await h.client.createAhpEndpoint(
                method === "createSession"
                    ? { onCreateSession: callback }
                    : { onResumeSession: callback }
            );
            const connection = await endpoint.openConnection({ onMessage: () => undefined });
            const response = h.server
                .sendRequest(`ahp.${method}`, {
                    endpointId: endpoint.id,
                    connectionId: connection.id,
                    ...(method === "createSession"
                        ? { requestedSessionId: "ghost" }
                        : { sessionId: "ghost" }),
                })
                .catch((error: Error) => error);
            // close() removes the registration synchronously, before the paired
            // reader dispatches the already-sent callback request.
            await connection.close();
            expect(await response).toMatchObject({ message: expect.stringContaining("closed") });
            expect(callback).not.toHaveBeenCalled();
            expect(sessionRpc).not.toHaveBeenCalled();
        }
    );

    it.each(["cancel", "dispose", "close", "stop", "eof"] as const)(
        "aborts callbacks and rejects late completion on %s",
        async (action) => {
            const h = harness();
            const started = deferred<AbortSignal>();
            const blocked = deferred<{ sessionId: string }>();
            const endpoint = await h.client.createAhpEndpoint({
                onCreateSession: (_request, { signal }) => {
                    started.resolve(signal);
                    return blocked.promise;
                },
            });
            const connection = await endpoint.openConnection({ onMessage: () => undefined });
            const source = new CancellationTokenSource();
            const request = h.server.sendRequest(
                "ahp.createSession",
                {
                    endpointId: endpoint.id,
                    connectionId: connection.id,
                    requestedSessionId: "requested",
                },
                source.token
            );
            // Observe rejection immediately, including EOF where the peer is disposed below.
            const result = request.catch((error: Error) => error);
            const signal = await started.promise;
            if (action === "cancel") source.cancel();
            if (action === "dispose") await endpoint.dispose();
            if (action === "close") await connection.close();
            if (action === "stop") await h.client.stop();
            if (action === "eof") h.toClient.end();
            await vi.waitFor(() => expect(signal.aborted).toBe(true));
            if (action === "stop" || action === "eof") h.server.dispose();
            expect(await result).toBeInstanceOf(Error);
            blocked.resolve({ sessionId: "too-late" });
            source.dispose();
        }
    );

    it("preserves opaque messages and string, null, and numeric AHP IDs verbatim", async () => {
        const h = harness();
        const received: string[] = [];
        const endpoint = await h.client.createAhpEndpoint();
        const connection = await endpoint.openConnection({
            onMessage: (text) => {
                received.push(text);
            },
        });
        const frames = [
            '{"jsonrpc":"2.0","id":"opaque:id","method":"x"}',
            '{ "jsonrpc": "2.0", "id": null, "result": {} }',
            '{"jsonrpc":"2.0","id":9007199254740993,"result":"unchanged"}',
            "not parsed by the SDK",
        ];
        for (const frame of frames) {
            await connection.send(frame);
            await expect(h.message(connection.id, frame)).resolves.toBeNull();
        }
        await vi.waitFor(() => expect(received).toEqual(frames));
        expect(h.send.mock.calls.map((call: any) => call[0].message)).toEqual(frames);
    });

    it("serializes delivery independently without blocking the global reader", async () => {
        const h = harness();
        const blocked = deferred();
        const delivered = deferred();
        const first = vi.fn(() => blocked.promise);
        const endpoint = await h.client.createAhpEndpoint();
        const a = await endpoint.openConnection({ onMessage: first });
        const b = await endpoint.openConnection({ onMessage: () => delivered.resolve() });
        let firstConnectionAcknowledged = false;
        const pending = Promise.all([h.message(a.id, "a1"), h.message(a.id, "a2")]).then(() => {
            firstConnectionAcknowledged = true;
        });
        await h.message(b.id, "b1");
        await delivered.promise;
        expect(first).toHaveBeenCalledTimes(1);
        expect(firstConnectionAcknowledged).toBe(false);
        await expect(h.client.ping()).resolves.toMatchObject({ message: "pong" });
        blocked.resolve();
        await pending;
        await vi.waitFor(() => expect(first).toHaveBeenCalledTimes(2));
    });

    it.each([
        "cancel-active",
        "cancel-queued",
        "close",
        "dispose",
        "native-close",
        "native-endpoint-close",
        "stop",
        "eof",
    ] as const)("rejects active and queued output ACKs on %s", async (action) => {
        const h = harness();
        const started = deferred();
        const blocked = deferred();
        const source = new CancellationTokenSource();
        const endpoint = await h.client.createAhpEndpoint({ onListSessions: () => [] });
        const onMessage = vi.fn(() => {
            started.resolve();
            return blocked.promise;
        });
        const onClose = vi.fn();
        const connection = await endpoint.openConnection({ onMessage, onClose });
        const active = h
            .message(
                connection.id,
                "active",
                action === "cancel-active" ? source.token : CancellationToken.None
            )
            .catch((error: Error) => error);
        const queued = h
            .message(
                connection.id,
                "queued",
                action === "cancel-queued" ? source.token : CancellationToken.None
            )
            .catch((error: Error) => error);
        await started.promise;
        // A later request is a reader barrier: both messages are admitted, even
        // though the active application callback and both ACKs are still pending.
        await h.server.sendRequest("ahp.listSessions", { endpointId: endpoint.id });
        if (action.startsWith("cancel")) source.cancel();
        if (action === "close") await connection.close();
        if (action === "dispose") await endpoint.dispose();
        if (action === "native-close") {
            await h.server.sendNotification("ahp.connectionClosed", {
                endpointId: endpoint.id,
                connectionId: connection.id,
            });
        }
        if (action === "native-endpoint-close") {
            await h.server.sendNotification("ahp.endpointClosed", { endpointId: endpoint.id });
        }
        if (action === "stop") await h.client.stop();
        if (action === "eof") h.toClient.end();
        await vi.waitFor(() => expect(onClose).toHaveBeenCalledOnce());
        if (action === "stop" || action === "eof") h.server.dispose();
        for (const result of await Promise.all([active, queued])) {
            expect(result).toBeInstanceOf(Error);
            if (action.startsWith("cancel")) expect(result).toMatchObject({ code: -32800 });
        }
        expect(onMessage).toHaveBeenCalledOnce();
        blocked.resolve();
        await connection.close();
        if (action !== "stop" && action !== "eof") {
            await expect(h.message(connection.id, "late")).rejects.toThrow("closed");
            await h.client.ping();
        }
        source.dispose();
    });

    it.each([
        { name: "message", limits: { maxMessageBytes: 3 }, frames: ["💡"] },
        { name: "count", limits: { maxQueuedMessages: 1 }, frames: ["a", "b"] },
        { name: "bytes", limits: { maxBufferedBytes: 3 }, frames: ["ab", "cd"] },
    ])("enforces $name bounds in both directions", async ({ limits, frames }) => {
        const h = harness();
        const blocked = deferred<null>();
        h.server.onRequest("ahp.send", () => blocked.promise);
        const endpoint = await h.client.createAhpEndpoint({ limits });
        const inputClosed = vi.fn();
        const outputClosed = vi.fn();
        const a = await endpoint.openConnection({
            onMessage: () => undefined,
            onClose: inputClosed,
        });
        const b = await endpoint.openConnection({
            onMessage: () => blocked.promise.then(() => undefined),
            onClose: outputClosed,
        });
        const pending = frames.map((frame) => a.send(frame).catch((error: Error) => error));
        const outputPending = frames.map((frame) =>
            h.message(b.id, frame).catch((error: Error) => error)
        );
        await vi.waitFor(() => {
            expect(inputClosed).toHaveBeenCalledOnce();
            expect(outputClosed).toHaveBeenCalledOnce();
            expect(h.close).toHaveBeenCalledTimes(2);
        });
        for (const result of await Promise.all(pending)) expect(result).toBeInstanceOf(Error);
        for (const result of await Promise.all(outputPending)) expect(result).toBeInstanceOf(Error);
        expect(inputClosed.mock.calls[0][0]).toBeInstanceOf(Error);
        blocked.resolve(null);
    });

    it("closes on output callback failure, not the endpoint or an unrelated owner session", async () => {
        const h = harness();
        const disconnect = vi.fn();
        h.server.onRequest("session.disconnect", disconnect);
        h.server.onRequest("session.create", (params: { sessionId: string }) => ({
            sessionId: params.sessionId,
        }));
        const owner = await h.client.createSession({
            sessionId: "unrelated-owner",
            onPermissionRequest: () => ({ kind: "no-result" }),
        });
        const ownerDisconnect = vi.spyOn(owner, "disconnect");
        const endpoint = await h.client.createAhpEndpoint();
        const onClose = vi.fn();
        const connection = await endpoint.openConnection({
            onMessage: () => Promise.reject(new Error("socket write failed")),
            onClose,
        });
        await expect(h.message(connection.id, "opaque")).rejects.toThrow("socket write failed");
        await vi.waitFor(() =>
            expect(onClose).toHaveBeenCalledWith(
                expect.objectContaining({ message: "socket write failed" })
            )
        );
        await connection.close();
        await endpoint.refreshExposure();
        await endpoint.dispose();
        expect(disconnect).not.toHaveBeenCalled();
        expect(ownerDisconnect).not.toHaveBeenCalled();
    });

    it("close/dispose are idempotent and forward exposure/capabilities", async () => {
        const h = harness();
        const refresh = vi.fn((_params: unknown) => null);
        const capabilities = vi.fn((_params: unknown) => null);
        h.server.onRequest("ahp.refreshExposure", refresh);
        h.server.onRequest("ahp.setCapabilities", capabilities);
        const endpoint = await h.client.createAhpEndpoint();
        expect(h.register.mock.calls[0][0]).toEqual({
            callbacks: {
                createSession: false,
                resumeSession: false,
                listSessions: false,
                sessionControl: false,
            },
        });
        const onClose = vi.fn();
        const connection = await endpoint.openConnection({ onMessage: () => undefined, onClose });
        await endpoint.refreshExposure();
        await endpoint.setCapabilities({ custom: true });
        expect(refresh.mock.calls[0][0]).toEqual({ endpointId: endpoint.id });
        expect(capabilities.mock.calls[0][0]).toEqual({
            endpointId: endpoint.id,
            capabilities: { custom: true },
        });
        await Promise.all([connection.close(), connection.close()]);
        await Promise.all([endpoint.dispose(), endpoint.dispose()]);
        expect(h.close).toHaveBeenCalledOnce();
        expect(h.dispose).toHaveBeenCalledOnce();
        expect(onClose).toHaveBeenCalledOnce();
        await expect(connection.send("late")).rejects.toThrow("closed");
        await expect(endpoint.openConnection({ onMessage: () => undefined })).rejects.toThrow(
            "closed"
        );
    });

    it("locally tears down even when remote disposal fails", async () => {
        const h = harness();
        h.server.onRequest("ahp.disposeEndpoint", () => {
            throw new Error("runtime gone");
        });
        const endpoint = await h.client.createAhpEndpoint();
        const onClose = vi.fn();
        await endpoint.openConnection({ onMessage: () => undefined, onClose });
        await expect(endpoint.dispose()).rejects.toThrow("runtime gone");
        await expect(endpoint.dispose()).rejects.toThrow("runtime gone");
        expect(onClose).toHaveBeenCalledOnce();
    });

    it("handles native close notifications without echoing cleanup or disconnecting sessions", async () => {
        const h = harness();
        const endpoint = await h.client.createAhpEndpoint();
        const onClose = vi.fn();
        const a = await endpoint.openConnection({ onMessage: () => undefined, onClose });
        const b = await endpoint.openConnection({ onMessage: () => undefined, onClose });
        await h.server.sendNotification("ahp.connectionClosed", {
            endpointId: endpoint.id,
            connectionId: a.id,
            error: "native overflow",
        });
        await vi.waitFor(() =>
            expect(onClose).toHaveBeenCalledWith(
                expect.objectContaining({ message: "native overflow" })
            )
        );
        await h.server.sendNotification("ahp.endpointClosed", { endpointId: endpoint.id });
        await vi.waitFor(() => expect(onClose).toHaveBeenCalledTimes(2));
        await Promise.all([a.close(), b.close(), endpoint.dispose()]);
        expect(h.close).not.toHaveBeenCalled();
        expect(h.dispose).not.toHaveBeenCalled();
    });

    it("rejects all queued sends when runtime admission fails", async () => {
        const h = harness();
        const blocked = deferred<null>();
        const started = deferred();
        h.server.onRequest("ahp.send", () => {
            started.resolve();
            return blocked.promise;
        });
        const endpoint = await h.client.createAhpEndpoint();
        const onClose = vi.fn();
        const connection = await endpoint.openConnection({ onMessage: () => undefined, onClose });
        const first = connection.send("first").catch((error: Error) => error);
        const second = connection.send("second").catch((error: Error) => error);
        await started.promise;
        blocked.reject(new Error("native admission denied"));
        expect(await first).toMatchObject({
            message: expect.stringContaining("native admission denied"),
        });
        expect(await second).toMatchObject({
            message: expect.stringContaining("native admission denied"),
        });
        await connection.close();
        expect(onClose).toHaveBeenCalledOnce();
    });

    it.each([
        undefined,
        { maxMessageBytes: 1024 * 1024, maxQueuedMessages: 128, maxBufferedBytes: 8 * 1024 * 1024 },
    ])("enforces hard limits with default or explicit ceilings: %j", async (limits) => {
        const h = harness();
        const blocked = deferred<null>();
        h.server.onRequest("ahp.send", () => blocked.promise);
        const endpoint = await h.client.createAhpEndpoint({ limits });
        const large = await endpoint.openConnection({ onMessage: () => undefined });
        await expect(large.send("a".repeat(1024 * 1024 + 1))).rejects.toThrow("message size limit");
        const queued = await endpoint.openConnection({ onMessage: () => undefined });
        const pending = Array.from({ length: 128 }, () =>
            queued.send("a".repeat(64 * 1024)).catch((error: Error) => error)
        );
        await expect(queued.send("a".repeat(64 * 1024))).rejects.toThrow("buffer limit");
        expect((await Promise.all(pending)).every((result) => result instanceof Error)).toBe(true);
        const buffered = await endpoint.openConnection({ onMessage: () => undefined });
        const bytes = Array.from({ length: 8 }, () =>
            buffered.send("a".repeat(1024 * 1024)).catch((error: Error) => error)
        );
        await expect(buffered.send("overflow")).rejects.toThrow("buffer limit");
        expect((await Promise.all(bytes)).every((result) => result instanceof Error)).toBe(true);
        blocked.resolve(null);
        await Promise.all([large.close(), queued.close(), buffered.close()]);
        await h.client.ping();
    });

    it("reports missing runtime methods clearly, preserving other errors", async () => {
        const h = harness();
        h.server.onRequest("ahp.registerEndpoint", () => {
            throw new ResponseError(-32601, "missing");
        });
        await expect(h.client.createAhpEndpoint()).rejects.toThrow("does not support native AHP");
        h.server.onRequest("ahp.registerEndpoint", () => {
            throw new ResponseError(-32000, "policy denied");
        });
        await expect(h.client.createAhpEndpoint()).rejects.toThrow("policy denied");
    });

    it("validates limits before registering", async () => {
        const h = harness();
        await expect(
            h.client.createAhpEndpoint({ limits: { maxMessageBytes: 0 } } as AhpEndpointOptions)
        ).rejects.toThrow("positive safe integers");
        expect(h.register).not.toHaveBeenCalled();
    });

    it.each([
        { maxMessageBytes: 1024 * 1024 + 1 },
        { maxQueuedMessages: 129 },
        { maxBufferedBytes: 8 * 1024 * 1024 + 1 },
        { maxQueuedMessages: 256, maxBufferedBytes: 16 * 1024 * 1024 },
    ])("rejects configuration above hard ceilings before registration: %j", async (limits) => {
        const h = harness();
        await expect(h.client.createAhpEndpoint({ limits })).rejects.toThrow("cannot exceed");
        expect(h.register).not.toHaveBeenCalled();
    });
});
