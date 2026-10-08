/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { createServer, type Socket } from "node:net";
import { setImmediate } from "node:timers/promises";
import { queryObjects } from "node:v8";
import { describe, expect, it, onTestFinished } from "vitest";
import {
    createMessageConnection,
    type MessageConnection,
    StreamMessageReader,
    StreamMessageWriter,
} from "vscode-jsonrpc/node.js";
import { approveAll, CopilotClient, RuntimeConnection } from "../src/index.js";
import type { SessionConfig } from "../src/index.js";
import { SDK_PROTOCOL_VERSION } from "../src/sdkProtocolVersion.js";

async function connectedClient(expectedSends: number, beforeDetach?: () => Promise<void>) {
    const sockets = new Set<Socket>();
    const connections = new Set<MessageConnection>();
    let sent = 0;
    let detachRequests = 0;
    let markSent!: () => void;
    const sendsReceived = new Promise<void>((resolve) => {
        markSent = resolve;
    });

    const server = createServer((socket) => {
        sockets.add(socket);
        const connection = createMessageConnection(
            new StreamMessageReader(socket),
            new StreamMessageWriter(socket)
        );
        connections.add(connection);
        connection.onRequest("connect", () => ({
            ok: true,
            protocolVersion: SDK_PROTOCOL_VERSION,
            version: "force-stop-test",
        }));
        connection.onRequest("ping", () => ({
            message: "pong",
            timestamp: "2026-01-01T00:00:00Z",
            protocolVersion: SDK_PROTOCOL_VERSION,
        }));
        connection.onRequest("session.create", (params: { sessionId: string }) => ({
            sessionId: params.sessionId,
        }));
        connection.onRequest("session.eventLog.registerInterest", (params) => {
            expect(params).toMatchObject({
                eventType: "mcp.oauth_required",
            });
            return { success: true };
        });
        connection.onRequest("session.send", () => {
            sent++;
            if (sent === expectedSends) markSent();
            return { messageId: `message-${sent}` };
        });
        connection.onRequest("session.detach", async () => {
            detachRequests++;
            await beforeDetach?.();
            return { success: true };
        });
        connection.onRequest((method) => {
            throw new Error(`Unexpected RPC: ${method}`);
        });
        connection.listen();
    });
    let client: CopilotClient | undefined;
    onTestFinished(async () => {
        try {
            await client?.forceStop();
        } finally {
            for (const connection of connections) connection.dispose();
            for (const socket of sockets) socket.destroy();
            if (server.listening) {
                await new Promise<void>((resolve, reject) => {
                    server.close((error) => (error ? reject(error) : resolve()));
                });
            }
        }
    });
    await new Promise<void>((resolve, reject) => {
        server.once("error", reject);
        server.listen(0, "127.0.0.1", () => {
            server.off("error", reject);
            resolve();
        });
    });
    const address = server.address();
    if (!address || typeof address === "string") {
        throw new Error("Expected a TCP listening address");
    }
    client = new CopilotClient({
        connection: RuntimeConnection.forUri(`127.0.0.1:${address.port}`),
    });
    await client.start();
    return { client, sendsReceived, detachRequests: () => detachRequests };
}

describe("forceStop response retirement", () => {
    it.each([1, 2])(
        "rejects %i acknowledged plain waits without waiting for idle",
        async (count) => {
            const peer = await connectedClient(count);
            const session = await peer.client.createSession({
                sessionId: "retirement-session",
                onPermissionRequest: approveAll,
            });
            const waits = Array.from({ length: count }, () =>
                session.sendAndWait({ prompt: "hold the response" }, 1000)
            );
            const rejected = waits.map((wait) =>
                expect(wait).rejects.toThrow(/Session disconnected while waiting for a response/)
            );
            await peer.sendsReceived;
            // An ordered RPC reply proves the earlier send acknowledgments were received.
            await peer.client.ping();

            await peer.client.forceStop();

            await Promise.all(rejected);
            await session.disconnect();
            expect(peer.detachRequests()).toBe(0);
            expect(() => session.on(() => {})).toThrow("Session is disconnected");
            expect(() => session.on("session.idle", () => {})).toThrow("Session is disconnected");
        }
    );
});

type CapturedRegistration = "hooks" | "mcp" | "bearer" | "transforms";

function capturedConfig(
    kind: CapturedRegistration,
    Capture: new () => { read(): string }
): SessionConfig {
    const capture = new Capture();
    switch (kind) {
        case "hooks":
            return {
                onPermissionRequest: approveAll,
                hooks: { onSessionStart: () => ({ additionalContext: capture.read() }) },
            };
        case "mcp":
            return {
                onPermissionRequest: approveAll,
                onMcpAuthRequest: () => {
                    capture.read();
                    return null;
                },
            };
        case "bearer":
            return {
                onPermissionRequest: approveAll,
                model: "test-model",
                provider: {
                    baseUrl: "http://127.0.0.1",
                    bearerTokenProvider: async () => capture.read(),
                },
            };
        case "transforms":
            return {
                onPermissionRequest: approveAll,
                systemMessage: {
                    mode: "customize",
                    sections: {
                        task_instructions: {
                            action: (content) => capture.read() + content,
                        },
                    },
                },
            };
    }
}

describe("forceStop callback ownership", () => {
    it.each(["hooks", "mcp", "bearer", "transforms"] satisfies CapturedRegistration[])(
        "releases a retained session's %s capture",
        async (kind) => {
            class Capture {
                read() {
                    return "owned-capture";
                }
            }
            const peer = await connectedClient(0);
            const session = await peer.client.createSession(capturedConfig(kind, Capture));
            await setImmediate();
            expect(queryObjects(Capture, { format: "count" })).toBe(1);

            await peer.client.forceStop();
            await setImmediate();

            expect(queryObjects(Capture, { format: "count" })).toBe(0);
            expect(session.sessionId).toBeTruthy();
        }
    );

    it("preserves caller-owned hook and transform configuration", async () => {
        const peer = await connectedClient(0);
        const session = await peer.client.createSession({ onPermissionRequest: approveAll });
        const hook = () => ({ additionalContext: "caller-owned" });
        const hooks = { onSessionStart: hook };
        const transform = (content: string) => content;
        const transforms = new Map([["task_instructions", transform]]);
        session.registerHooks(hooks);
        session.registerTransformCallbacks(transforms);

        await peer.client.forceStop();

        expect(hooks.onSessionStart).toBe(hook);
        expect(transforms.get("task_instructions")).toBe(transform);
    });

    it("rejects retired callback registration and dispatch", async () => {
        const peer = await connectedClient(0);
        const session = await peer.client.createSession({ onPermissionRequest: approveAll });
        await peer.client.forceStop();
        const registrations = [
            () => session.registerTools(),
            () => session.registerCanvases(),
            () => session.registerWorkflows(),
            () => session.registerBearerTokenProviders(),
            () => session.registerCommands(),
            () => session.registerElicitationHandler(),
            () => session.registerExitPlanModeHandler(),
            () => session.registerAutoModeSwitchHandler(),
            () => session.registerPermissionHandler(),
            () => session.registerUserInputHandler(),
            () => session.registerSkillProvider(),
            () => session.registerHooks(),
            () => session.registerTransformCallbacks(),
        ];
        for (const register of registrations) {
            expect(register).toThrow("Session is disconnected");
        }
        await expect(session._handleHooksInvoke("sessionStart", {})).rejects.toThrow(
            "Session is disconnected"
        );
        await expect(
            session._handleSystemMessageTransform({
                task_instructions: { content: "unchanged" },
            })
        ).rejects.toThrow("Session is disconnected");
        await expect(session._handleSkillProviderList()).rejects.toThrow("Session is disconnected");
    });

    it("keeps hooks available through ordinary detach acknowledgement", async () => {
        let markDetach!: () => void;
        const detachStarted = new Promise<void>((resolve) => {
            markDetach = resolve;
        });
        let releaseDetach!: () => void;
        const detachAllowed = new Promise<void>((resolve) => {
            releaseDetach = resolve;
        });
        const peer = await connectedClient(0, () => {
            markDetach();
            return detachAllowed;
        });
        const hookCalls: string[] = [];
        const session = await peer.client.createSession({
            onPermissionRequest: approveAll,
            hooks: {
                onSessionEnd: () => {
                    hookCalls.push("detach-hook");
                },
            },
        });
        const disconnect = session.disconnect();
        let bodyFailure: unknown;
        try {
            await detachStarted;
            await session._handleHooksInvoke("sessionEnd", {});
            expect(hookCalls).toEqual(["detach-hook"]);
        } catch (error) {
            bodyFailure = error;
        } finally {
            releaseDetach();
            try {
                await disconnect;
            } catch (cleanupError) {
                bodyFailure =
                    bodyFailure === undefined
                        ? cleanupError
                        : new AggregateError(
                              [bodyFailure, cleanupError],
                              "Detach hook test and cleanup failed"
                          );
            }
        }
        if (bodyFailure !== undefined) throw bodyFailure;
        await expect(session._handleHooksInvoke("sessionEnd", {})).rejects.toThrow(
            "Session is disconnected"
        );
        expect(hookCalls).toEqual(["detach-hook"]);
    });
});
