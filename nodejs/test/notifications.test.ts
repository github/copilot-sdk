// Copyright (c) Microsoft Corporation. All rights reserved.

import { once } from "node:events";
import { createServer, type Socket } from "node:net";
import { describe, expect, it, onTestFinished, vi } from "vitest";
import {
    createMessageConnection,
    ErrorCodes,
    ResponseError,
    type CancellationToken,
    type MessageConnection,
    StreamMessageReader,
    StreamMessageWriter,
} from "vscode-jsonrpc/node.js";
import { CopilotClient } from "../src/client.js";
import { defaultJoinSessionPermissionHandler, RuntimeConnection } from "../src/types.js";

async function fixture(configure: (rpc: MessageConnection) => void = () => {}) {
    const sockets = new Set<Socket>();
    const connections = new Set<MessageConnection>();
    const resumed = vi.fn(({ sessionId }: { sessionId: string }) => ({ sessionId }));
    const server = createServer((socket) => {
        sockets.add(socket);
        socket.once("close", () => sockets.delete(socket));
        const rpc = createMessageConnection(
            new StreamMessageReader(socket),
            new StreamMessageWriter(socket)
        );
        connections.add(rpc);
        rpc.onRequest("connect", () => ({ protocolVersion: 3 }));
        rpc.onRequest("ping", () => ({ message: "ok", timestamp: Date.now() }));
        rpc.onRequest("session.resume", resumed);
        configure(rpc);
        rpc.listen();
    });
    server.listen(0, "127.0.0.1");
    await once(server, "listening");
    const address = server.address();
    if (!address || typeof address === "string") throw new Error("Missing fixture address");
    const client = new CopilotClient({
        connection: RuntimeConnection.forUri(`127.0.0.1:${address.port}`),
    });
    onTestFinished(async () => {
        await client.forceStop();
        for (const rpc of connections) rpc.dispose();
        for (const socket of sockets) socket.destroy();
        await new Promise<void>((resolve, reject) => {
            server.close((error) => (error ? reject(error) : resolve()));
        });
    });
    return { client, resumed };
}

describe("notification extension opt-in", () => {
    it("sends the opt-in on extension resume without requiring a notification response", async () => {
        const { client, resumed } = await fixture();
        const session = await client.resumeSessionForExtension(
            "notification-session",
            { onPermissionRequest: defaultJoinSessionPermissionHandler },
            undefined,
            { requestNotifications: true }
        );

        expect(session.sessionId).toBe("notification-session");
        expect(resumed).toHaveBeenCalledTimes(1);
        expect(resumed.mock.calls[0]?.[0]).toMatchObject({
            sessionId: "notification-session",
            requestNotifications: true,
        });
    });

    describe("session.notifications", () => {
        const available = {
            status: "available",
            platform: "darwin",
            permission: { extension: "granted", os: "granted" },
            onClick: ["open-url", "focus-canvas"],
            sounds: { default: true, none: true, named: ["Glass"] },
        };
        const notification = {
            title: "Synthetic review",
            body: "A synthetic review is ready.",
            onClick: { kind: "open-url" as const, url: "https://example.test/review" },
            sound: { kind: "none" as const },
        };

        async function join(client: CopilotClient) {
            return client.resumeSessionForExtension(
                "notification-session",
                { onPermissionRequest: defaultJoinSessionPermissionHandler },
                undefined,
                { requestNotifications: true }
            );
        }

        it("discovers current capabilities without a handshake capability flag", async () => {
            const getCapabilities = vi.fn((_request: unknown) => available);
            const { client } = await fixture((rpc) =>
                rpc.onRequest("session.notifications.getCapabilities", getCapabilities)
            );
            const session = await join(client);

            expect(await session.notifications.getCapabilities()).toEqual(available);
            expect(getCapabilities.mock.calls[0]?.[0]).toEqual({
                sessionId: "notification-session",
            });
        });

        it("maps only capability method-not-found to unsupported", async () => {
            const { client } = await fixture();
            const session = await join(client);

            await expect(session.notifications.getCapabilities()).resolves.toEqual({
                status: "unsupported",
            });
            await expect(session.notifications.requestPermission()).rejects.toMatchObject({
                code: ErrorCodes.MethodNotFound,
            });
            await expect(session.notifications.show(notification)).rejects.toMatchObject({
                code: ErrorCodes.MethodNotFound,
            });
        });

        it("does not turn failed capability requests into an unsupported fallback", async () => {
            const { client } = await fixture((rpc) => {
                rpc.onRequest("session.notifications.getCapabilities", () => {
                    throw new ResponseError(ErrorCodes.InternalError, "Synthetic host failure");
                });
            });
            const session = await join(client);

            await expect(session.notifications.getCapabilities()).rejects.toMatchObject({
                code: ErrorCodes.InternalError,
            });
        });

        it("forwards permissions and notification results without conversation events", async () => {
            const permission = {
                status: "completed",
                permission: { extension: "granted", os: "not-required" },
            };
            const requestPermission = vi.fn(() => permission);
            const show = vi.fn((_request: unknown) => ({
                status: "accepted",
                notificationId: "synthetic-notification",
            }));
            const { client } = await fixture((rpc) => {
                rpc.onRequest("session.notifications.requestPermission", requestPermission);
                rpc.onRequest("session.notifications.show", show);
            });
            const session = await join(client);
            const events = vi.fn();
            session.on(events);

            await expect(session.notifications.requestPermission()).resolves.toEqual(permission);
            const untypedNotification = { ...notification, sessionId: "untrusted-session" };
            await expect(session.notifications.show(untypedNotification)).resolves.toEqual({
                status: "accepted",
                notificationId: "synthetic-notification",
            });
            expect(show).toHaveBeenCalledTimes(1);
            expect(show.mock.calls[0]?.[0]).toEqual({
                ...notification,
                sessionId: "notification-session",
            });
            expect(events).not.toHaveBeenCalled();
        });

        it.each(["denied", "unsupported", "unavailable", "failed", "invalid-request"])(
            "does not retry or fall back after delivery returns %s",
            async (status) => {
                const show = vi.fn(() => ({ status }));
                const { client } = await fixture((rpc) =>
                    rpc.onRequest("session.notifications.show", show)
                );
                const session = await join(client);

                await expect(session.notifications.show(notification)).resolves.toEqual({
                    status,
                });
                expect(show).toHaveBeenCalledTimes(1);
            }
        );

        it("does not retry a delivery whose response fails", async () => {
            const show = vi.fn(() => {
                throw new ResponseError(ErrorCodes.InternalError, "Synthetic handoff failure");
            });
            const { client } = await fixture((rpc) =>
                rpc.onRequest("session.notifications.show", show)
            );
            const session = await join(client);

            await expect(session.notifications.show(notification)).rejects.toMatchObject({
                code: ErrorCodes.InternalError,
            });
            expect(show).toHaveBeenCalledTimes(1);
        });

        it("sends nothing when the caller's signal is already aborted", async () => {
            const requested = vi.fn();
            const { client } = await fixture((rpc) => {
                rpc.onRequest("session.notifications.getCapabilities", requested);
                rpc.onRequest("session.notifications.requestPermission", requested);
                rpc.onRequest("session.notifications.show", requested);
            });
            const session = await join(client);
            const controller = new AbortController();
            controller.abort();
            const options = { signal: controller.signal };

            await expect(session.notifications.getCapabilities(options)).rejects.toMatchObject({
                name: "AbortError",
            });
            await expect(session.notifications.requestPermission(options)).rejects.toMatchObject({
                name: "AbortError",
            });
            await expect(session.notifications.show(notification, options)).rejects.toMatchObject({
                name: "AbortError",
            });
            expect(requested).not.toHaveBeenCalled();
        });

        it("forwards cancellation to the runtime without serializing the signal", async () => {
            const cancelled = vi.fn();
            const show = vi.fn((_request: unknown, token: CancellationToken) => {
                return new Promise((_resolve, reject) => {
                    token.onCancellationRequested(() => {
                        cancelled();
                        reject(new ResponseError(-32800, "Notification request cancelled"));
                    });
                });
            });
            const { client } = await fixture((rpc) =>
                rpc.onRequest("session.notifications.show", show)
            );
            const session = await join(client);
            const controller = new AbortController();
            const pending = session.notifications.show(notification, {
                signal: controller.signal,
            });
            const rejected = expect(pending).rejects.toMatchObject({ code: -32800 });
            await vi.waitFor(() => expect(show).toHaveBeenCalledTimes(1));
            controller.abort();
            await rejected;

            expect(cancelled).toHaveBeenCalledTimes(1);
            expect(show.mock.calls[0]?.[0]).toEqual({
                ...notification,
                sessionId: "notification-session",
            });
            expect(show).toHaveBeenCalledTimes(1);
        });
    });

    it("keeps notification opt-in off ordinary resume payloads", async () => {
        const { client, resumed } = await fixture();
        const config = {
            onPermissionRequest: defaultJoinSessionPermissionHandler,
            requestNotifications: true,
        };
        await client.resumeSession("ordinary-session", config);

        expect(resumed.mock.calls[0]?.[0]).not.toHaveProperty("requestNotifications");
    });

    it.each([undefined, false])("omits an inactive extension opt-in (%s)", async (enabled) => {
        const { client, resumed } = await fixture();
        await client.resumeSessionForExtension(
            "notification-session",
            { onPermissionRequest: defaultJoinSessionPermissionHandler },
            undefined,
            { requestNotifications: enabled }
        );

        expect(resumed.mock.calls[0]?.[0]).not.toHaveProperty("requestNotifications");
    });
});
