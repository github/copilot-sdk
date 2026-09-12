/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { createServer, type Socket } from "node:net";
import { PassThrough } from "node:stream";
import { describe, expect, it, onTestFinished } from "vitest";
import * as rpc9 from "vscode-jsonrpc/node";
import * as rpc8 from "vscode-jsonrpc-8/node.js";
import {
    CopilotClient,
    CopilotSession,
    RuntimeConnection,
    type SessionLifecycleEvent,
    type SessionLifecycleEventType,
} from "../src/index.js";

// A union of the full 8/9 overload sets cannot resolve named-method registration.
interface PeerReceiver {
    onRequest<P, R, E>(method: string, handler: rpc9.RequestHandler<P, R, E>): rpc9.Disposable;
    onNotification<P>(method: string, handler: rpc9.NotificationHandler<P>): rpc9.Disposable;
}

function deferred<T>() {
    let resolve!: (value: T | PromiseLike<T>) => void;
    const promise = new Promise<T>((resolvePromise) => {
        resolve = resolvePromise;
    });
    return { promise, resolve };
}

async function within<T>(promise: Promise<T>): Promise<T> {
    let timer: ReturnType<typeof setTimeout> | undefined;
    try {
        return await Promise.race([
            promise,
            new Promise<never>((_resolve, reject) => {
                timer = setTimeout(() => reject(new Error("JSON-RPC test timed out")), 5000);
            }),
        ]);
    } finally {
        clearTimeout(timer);
    }
}

function streamPeers() {
    const toEight = new PassThrough();
    const toNine = new PassThrough();
    const connections: { dispose(): void }[] = [];
    const errors: unknown[] = [];
    onTestFinished(() => {
        for (const connection of connections) connection.dispose();
        toEight.destroy();
        toNine.destroy();
    });

    const nine = rpc9.createMessageConnection(
        new rpc9.StreamMessageReader(toNine),
        new rpc9.StreamMessageWriter(toEight)
    );
    connections.push(nine);
    const eight = rpc8.createMessageConnection(
        new rpc8.StreamMessageReader(toEight),
        new rpc8.StreamMessageWriter(toNine)
    );
    connections.push(eight);
    nine.onError((error) => errors.push(error));
    eight.onError((error) => errors.push(error));
    nine.onUnhandledNotification((notification) => errors.push(notification));
    eight.onUnhandledNotification((notification) => errors.push(notification));
    nine.listen();
    eight.listen();
    return { nine, eight, errors };
}

describe("JSON-RPC 9 compatibility with an 8.2.1 peer", () => {
    it("delivers SDK lifecycle events over TCP and unsubscribes typed and wildcard handlers", async () => {
        const accepted = deferred<rpc8.MessageConnection>();
        const sockets = new Set<Socket>();
        const connections: rpc8.MessageConnection[] = [];
        const requests: { method: string; params: unknown }[] = [];
        const errors: unknown[] = [];
        let client: CopilotClient | undefined;
        const timestamp = "2026-01-02T03:04:05.000Z";
        const server = createServer((socket) => {
            sockets.add(socket);
            socket.on("close", () => sockets.delete(socket));
            socket.on("error", (error) => errors.push(error));
            const peer = rpc8.createMessageConnection(
                new rpc8.StreamMessageReader(socket),
                new rpc8.StreamMessageWriter(socket)
            );
            connections.push(peer);
            peer.onError((error) => errors.push(error));
            peer.onUnhandledNotification((notification) => errors.push(notification));
            peer.onRequest("connect", (params: { supportedTaskKinds: string[] }) => {
                requests.push({ method: "connect", params });
                return { protocolVersion: 3 };
            });
            peer.onRequest("ping", (params: { message: string }) => {
                requests.push({ method: "ping", params });
                return { message: params.message, timestamp, protocolVersion: 3 };
            });
            peer.listen();
            accepted.resolve(peer);
        });
        onTestFinished(async () => {
            try {
                if (client) await within(client.forceStop());
            } finally {
                for (const connection of connections) connection.dispose();
                for (const socket of sockets) socket.destroy();
                if (server.listening) {
                    await within(
                        new Promise<void>((resolve, reject) => {
                            server.close((error) => (error ? reject(error) : resolve()));
                        })
                    );
                }
            }
        });
        await within(
            new Promise<void>((resolve, reject) => {
                server.once("error", reject);
                server.listen(0, "127.0.0.1", () => {
                    server.off("error", reject);
                    resolve();
                });
            })
        );
        server.on("error", (error) => errors.push(error));
        const address = server.address();
        if (address === null || typeof address === "string") {
            throw new Error("Expected a loopback TCP address");
        }
        const sdk = new CopilotClient({
            connection: RuntimeConnection.forUri(`127.0.0.1:${address.port}`),
        });
        client = sdk;
        await within(sdk.start());
        const peer = await within(accepted.promise);
        expect(requests).toStrictEqual([
            { method: "connect", params: { supportedTaskKinds: ["agent", "client", "shell"] } },
        ]);

        const typed: {
            [K in SessionLifecycleEventType]: Extract<SessionLifecycleEvent, { type: K }>[];
        } = {
            "session.created": [],
            "session.deleted": [],
            "session.updated": [],
            "session.foreground": [],
            "session.background": [],
        };
        const wildcard: SessionLifecycleEvent[] = [];
        const unsubscribe = {
            created: sdk.onLifecycle("session.created", (event) =>
                typed["session.created"].push(event)
            ),
            deleted: sdk.onLifecycle("session.deleted", (event) =>
                typed["session.deleted"].push(event)
            ),
            updated: sdk.onLifecycle("session.updated", (event) =>
                typed["session.updated"].push(event)
            ),
            foreground: sdk.onLifecycle("session.foreground", (event) =>
                typed["session.foreground"].push(event)
            ),
            background: sdk.onLifecycle("session.background", (event) =>
                typed["session.background"].push(event)
            ),
            wildcard: sdk.onLifecycle((event) => wildcard.push(event)),
        };
        const wireMetadata = {
            startTime: timestamp,
            modifiedTime: "2026-01-02T04:05:06.000Z",
            summary: "Lifecycle compatibility",
        };
        const metadata = {
            startTime: new Date(wireMetadata.startTime),
            modifiedTime: new Date(wireMetadata.modifiedTime),
            summary: wireMetadata.summary,
        };
        const sessionId = "compat-lifecycle";
        const deleted = { type: "session.deleted", sessionId };
        const created = { type: "session.created", sessionId, metadata: wireMetadata };
        const wireEvents = [
            created,
            deleted,
            { type: "session.updated", sessionId, metadata: wireMetadata },
            { type: "session.foreground", sessionId, metadata: wireMetadata },
            { type: "session.background", sessionId, metadata: wireMetadata },
        ];
        const expected: SessionLifecycleEvent[] = [
            { type: "session.created", sessionId, metadata },
            { type: "session.deleted", sessionId, metadata: undefined },
            { type: "session.updated", sessionId, metadata },
            { type: "session.foreground", sessionId, metadata },
            { type: "session.background", sessionId, metadata },
        ];
        const barriers: string[] = [];
        async function barrier(message: string): Promise<void> {
            barriers.push(message);
            // The ping response follows the lifecycle notifications on the same stream.
            await expect(within(sdk.ping(message))).resolves.toStrictEqual({
                message,
                timestamp,
                protocolVersion: 3,
            });
        }

        for (const event of wireEvents) {
            await within(peer.sendNotification("session.lifecycle", event));
        }
        await barrier("initial delivery");
        expect(wildcard).toStrictEqual(expected);
        for (const event of expected) {
            expect(typed[event.type]).toStrictEqual([event]);
            if (event.metadata !== undefined) {
                expect(typed[event.type][0].metadata?.startTime).toBeInstanceOf(Date);
                expect(typed[event.type][0].metadata?.modifiedTime).toBeInstanceOf(Date);
            }
        }
        expect(Object.hasOwn(deleted, "metadata")).toBe(false);
        expect(Object.hasOwn(typed["session.deleted"][0], "metadata")).toBe(true);
        expect(typed["session.deleted"][0].metadata).toBeUndefined();

        unsubscribe.deleted();
        unsubscribe.deleted();
        await within(peer.sendNotification("session.lifecycle", deleted));
        await within(peer.sendNotification("session.lifecycle", created));
        await barrier("typed unsubscribe");
        expect(typed["session.deleted"]).toStrictEqual([expected[1]]);
        expect(typed["session.created"]).toStrictEqual([expected[0], expected[0]]);
        expect(wildcard).toStrictEqual([...expected, expected[1], expected[0]]);

        unsubscribe.wildcard();
        unsubscribe.wildcard();
        await within(peer.sendNotification("session.lifecycle", deleted));
        await within(peer.sendNotification("session.lifecycle", created));
        await barrier("wildcard unsubscribe");
        expect(typed["session.deleted"]).toStrictEqual([expected[1]]);
        expect(typed["session.created"]).toStrictEqual([expected[0], expected[0], expected[0]]);
        expect(wildcard).toStrictEqual([...expected, expected[1], expected[0]]);

        for (const dispose of Object.values(unsubscribe)) dispose();
        for (const event of wireEvents) {
            await within(peer.sendNotification("session.lifecycle", event));
        }
        await barrier("all unsubscribed");
        expect(typed).toStrictEqual({
            "session.created": [expected[0], expected[0], expected[0]],
            "session.deleted": [expected[1]],
            "session.updated": [expected[2]],
            "session.foreground": [expected[3]],
            "session.background": [expected[4]],
        });
        expect(wildcard).toStrictEqual([...expected, expected[1], expected[0]]);
        await expect(within(sdk.stop())).resolves.toStrictEqual([]);
        await expect(within(sdk.ping("after stop"))).rejects.toThrow("Client not connected");
        expect(connections).toHaveLength(1);
        expect(requests).toStrictEqual([
            { method: "connect", params: { supportedTaskKinds: ["agent", "client", "shell"] } },
            ...barriers.map((message) => ({ method: "ping", params: { message } })),
        ]);
        expect(errors).toStrictEqual([]);
    }, 15000);

    it("sends and disconnects a public SDK session through the 8.2.1 peer", async () => {
        const { nine, eight, errors } = streamPeers();
        const received: { method: string; params: unknown }[] = [];
        eight.onRequest(
            "session.send",
            (params: { sessionId: string; prompt: string; displayPrompt: string }) => {
                received.push({ method: "session.send", params });
                return { messageId: "compat-message" };
            }
        );
        eight.onRequest("session.detach", (params: { sessionId: string }) => {
            received.push({ method: "session.detach", params });
            return { success: true };
        });
        const session = new CopilotSession("compat-session", nine);
        await expect(
            within(session.send({ prompt: "wire prompt", displayPrompt: "display prompt" }))
        ).resolves.toBe("compat-message");
        await within(session.disconnect());
        expect(received).toStrictEqual([
            {
                method: "session.send",
                params: {
                    sessionId: "compat-session",
                    prompt: "wire prompt",
                    displayPrompt: "display prompt",
                },
            },
            { method: "session.detach", params: { sessionId: "compat-session" } },
        ]);
        expect(errors).toStrictEqual([]);
    }, 15000);

    describe.each(["9 to 8", "8 to 9"])("%s", (direction) => {
        function peers() {
            const { nine, eight, errors } = streamPeers();
            const receiver: PeerReceiver = direction === "9 to 8" ? eight : nine;
            return {
                sender: direction === "9 to 8" ? nine : eight,
                receiver,
                sending: direction === "9 to 8" ? rpc9 : rpc8,
                receiving: direction === "9 to 8" ? rpc8 : rpc9,
                errors,
            };
        }

        it("preserves object requests, notifications and structured response errors", async () => {
            const { sender, receiver, sending, receiving, errors } = peers();
            const params = { text: "caf\u00e9", nested: { enabled: false }, values: [0, null, ""] };
            const received: unknown[] = [];
            const notified = deferred<typeof params>();
            receiver.onRequest("compat.echo", (value: typeof params) => {
                received.push(value);
                return { echoed: value, count: 3 };
            });
            receiver.onNotification("compat.notice", (value: typeof params) => {
                notified.resolve(value);
            });
            receiver.onRequest("compat.fail", (value: { operation: string }) => {
                received.push(value);
                throw new receiving.ResponseError(-32042, "compatibility failure", {
                    operation: value.operation,
                    retryable: false,
                });
            });

            await expect(within(sender.sendRequest("compat.echo", params))).resolves.toStrictEqual({
                echoed: params,
                count: 3,
            });
            await within(sender.sendNotification("compat.notice", params));
            await expect(within(notified.promise)).resolves.toStrictEqual(params);
            const failure = within(sender.sendRequest("compat.fail", { operation: "read" }));
            await expect(failure).rejects.toBeInstanceOf(sending.ResponseError);
            await expect(failure).rejects.toStrictEqual(
                new sending.ResponseError(-32042, "compatibility failure", {
                    operation: "read",
                    retryable: false,
                })
            );
            await expect(within(sender.sendRequest("compat.missing", {}))).rejects.toStrictEqual(
                new sending.ResponseError(-32601, "Unhandled method compat.missing")
            );
            expect(received).toStrictEqual([params, { operation: "read" }]);
            expect(errors).toStrictEqual([]);
        }, 15000);

        it("delivers cancellation to the remote request token and preserves its error response", async () => {
            const { sender, receiver, sending, receiving, errors } = peers();
            const source = new sending.CancellationTokenSource();
            const started = deferred<{ operation: string; initiallyCancelled: boolean }>();
            const cancelled = deferred<boolean>();
            const release = deferred<void>();
            onTestFinished(() => {
                release.resolve();
                source.dispose();
            });
            receiver.onRequest(
                "compat.cancel",
                async (params: { operation: string }, token: rpc9.CancellationToken) => {
                    const subscription = token.onCancellationRequested(() => {
                        cancelled.resolve(token.isCancellationRequested);
                    });
                    try {
                        started.resolve({
                            operation: params.operation,
                            initiallyCancelled: token.isCancellationRequested,
                        });
                        await release.promise;
                        return new receiving.ResponseError(-32800, "cancelled by peer", params);
                    } finally {
                        subscription.dispose();
                    }
                }
            );
            const response = sender
                .sendRequest("compat.cancel", { operation: "wait" }, source.token)
                .then(
                    (value: unknown) => ({ value }),
                    (error: unknown) => ({ error })
                );
            let outcome: { value: unknown } | { error: unknown };
            try {
                await expect(within(started.promise)).resolves.toStrictEqual({
                    operation: "wait",
                    initiallyCancelled: false,
                });
                source.cancel();
                await expect(within(cancelled.promise)).resolves.toBe(true);
            } finally {
                release.resolve();
                outcome = await within(response);
            }
            expect(outcome).toStrictEqual({
                error: new sending.ResponseError(-32800, "cancelled by peer", {
                    operation: "wait",
                }),
            });
            if (!("error" in outcome)) throw new Error("Expected a cancellation error response");
            expect(outcome.error).toBeInstanceOf(sending.ResponseError);
            expect(errors).toStrictEqual([]);
        }, 15000);

        it("does not let a pending async notification block the following request by default", async () => {
            const { sender, receiver, errors } = peers();
            const started = deferred<{ label: string }>();
            const release = deferred<void>();
            const finished = deferred<void>();
            const order: string[] = [];
            const requests: unknown[] = [];
            onTestFinished(() => release.resolve());
            receiver.onNotification("compat.slow", async (params: { label: string }) => {
                order.push("notification started");
                started.resolve(params);
                await release.promise;
                order.push("notification finished");
                finished.resolve();
            });
            receiver.onRequest("compat.after", (params: { label: string }) => {
                requests.push(params);
                order.push("request handled");
                return { label: params.label, notificationPending: order.length === 2 };
            });
            try {
                await within(sender.sendNotification("compat.slow", { label: "slow" }));
                await expect(within(started.promise)).resolves.toStrictEqual({ label: "slow" });
                await expect(
                    within(sender.sendRequest("compat.after", { label: "following" }))
                ).resolves.toStrictEqual({ label: "following", notificationPending: true });
                expect(order).toStrictEqual(["notification started", "request handled"]);
                expect(requests).toStrictEqual([{ label: "following" }]);
            } finally {
                release.resolve();
                await within(finished.promise);
            }
            expect(order).toStrictEqual([
                "notification started",
                "request handled",
                "notification finished",
            ]);
            expect(errors).toStrictEqual([]);
        }, 15000);
    });
});
