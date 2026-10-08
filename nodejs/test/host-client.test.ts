import { once } from "node:events";
import { createServer, type Socket } from "node:net";
import { describe, expect, it, onTestFinished, vi } from "vitest";
import {
    createMessageConnection,
    type MessageConnection,
    StreamMessageReader,
    StreamMessageWriter,
} from "vscode-jsonrpc/node.js";
import { approveAll, CopilotClient, RuntimeConnection, type AhpHostOptions } from "../src/index.js";
import type { HostStartRequest } from "../src/generated/rpc.js";

// Wire-level SDK unit tests. Real runtime/listener coverage lives in test/e2e.
async function fixture(
    configure: (rpc: MessageConnection, socket: Socket, writer: StreamMessageWriter) => void
) {
    const sockets = new Set<Socket>();
    const connections = new Set<MessageConnection>();
    const server = createServer((socket) => {
        sockets.add(socket);
        socket.once("close", () => sockets.delete(socket));
        const writer = new StreamMessageWriter(socket);
        const rpc = createMessageConnection(new StreamMessageReader(socket), writer);
        connections.add(rpc);
        rpc.onRequest("connect", () => ({ protocolVersion: 3 }));
        rpc.onRequest("ping", () => ({ message: "ok", timestamp: Date.now() }));
        configure(rpc, socket, writer);
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
    return client;
}

function info(hostId: string) {
    return { hostId, url: "ws://127.0.0.1:54321", token: "test-only-token" };
}

describe("CopilotClient.startAhpHost", () => {
    it("requires an explicit transport without starting the client", async () => {
        const client = new CopilotClient();
        const start = vi.spyOn(client, "start");
        await expect(client.startAhpHost({})).rejects.toThrow("At least one");
        expect(start).not.toHaveBeenCalled();
    });

    it.each([false, true])(
        "forwards GitHub hosting with optional local transport (%s)",
        async (local) => {
            const githubEnvironment = {
                name: "Application",
                computeId: "stable-installation-id",
                requireConnectionBinding: false,
            };
            const start = vi.fn(({ hostId }: HostStartRequest) => ({
                hostId,
                environmentId: "environment-1",
                ...(local ? { url: "ws://127.0.0.1:54321", token: "local-token" } : {}),
            }));
            const client = await fixture((rpc) => rpc.onRequest("host.start", start));
            const options = {
                computeId: githubEnvironment.computeId,
                githubEnvironment,
                ...(local ? { localServer: {} } : {}),
            };
            const host = await client.startAhpHost(options);
            expect(start.mock.calls[0]?.[0]).toEqual({ hostId: host.hostId, ...options });
            expect(host.environmentId).toBe("environment-1");
            expect(host.url).toBe(local ? "ws://127.0.0.1:54321" : undefined);
            expect(host.token).toBe(local ? "local-token" : undefined);
            expect(host.pid).toBeUndefined();
        }
    );

    it.each([undefined, "", "stable-local-compute"])(
        "forwards local compute identity without supplying SDK defaults (%s)",
        async (computeId) => {
            const start = vi.fn(({ hostId }: HostStartRequest) => info(hostId));
            const client = await fixture((rpc) => rpc.onRequest("host.start", start));
            await client.startAhpHost({ localServer: {}, computeId });
            expect(start.mock.calls[0]?.[0]).toEqual({
                hostId: expect.any(String),
                localServer: {},
                ...(computeId === undefined ? {} : { computeId }),
            });
        }
    );

    it("snapshots factories and release callbacks at startup", async () => {
        let rpc!: MessageConnection;
        const client = await fixture((connection) => {
            rpc = connection;
            rpc.onRequest("host.start", ({ hostId }: HostStartRequest) => info(hostId));
            rpc.onRequest("session.create", ({ sessionId }: { sessionId: string }) => ({
                sessionId,
            }));
            rpc.onRequest("session.resume", ({ sessionId }: { sessionId: string }) => ({
                sessionId,
            }));
        });
        const released = vi.fn();
        const replacement = vi.fn();
        const options: AhpHostOptions = {
            localServer: {},
            createSession: ({ config }) =>
                client.createSession({ ...config, onPermissionRequest: approveAll }),
            resumeSession: ({ sessionId, config }) =>
                client.resumeSession(sessionId, { ...config, onPermissionRequest: approveAll }),
            onSessionReleased: released,
        };
        const host = await client.startAhpHost(options);
        options.createSession = replacement;
        options.resumeSession = undefined;
        options.onSessionReleased = replacement;
        for (const resume of [false, true]) {
            const sessionId = resume ? "resumed" : "created";
            await expect(
                rpc.sendRequest("host.materializeSession", {
                    hostId: host.hostId,
                    handoffId: sessionId,
                    resume,
                    config: { sessionId },
                })
            ).resolves.toEqual({ sessionId });
            await rpc.sendNotification("host.sessionReleased", {
                hostId: host.hostId,
                handoffId: sessionId,
            });
        }
        await vi.waitFor(() => expect(released).toHaveBeenCalledTimes(2));
        expect(replacement).not.toHaveBeenCalled();
    });

    it.each([false, true])(
        "normalizes the configuration directory before invoking factories (resume=%s)",
        async (resume) => {
            let rpc!: MessageConnection;
            const materialize = vi.fn(({ sessionId }: { sessionId: string }) => ({ sessionId }));
            const client = await fixture((connection) => {
                rpc = connection;
                rpc.onRequest("host.start", ({ hostId }: HostStartRequest) => info(hostId));
                rpc.onRequest(resume ? "session.resume" : "session.create", materialize);
            });
            const checkConfig = (config: object) => {
                expect(config).toHaveProperty("configDirectory", "/selected-config");
                expect(config).not.toHaveProperty("configDir");
            };
            const host = await client.startAhpHost({
                localServer: {},
                createSession: ({ config }) => {
                    checkConfig(config);
                    return client.createSession({ ...config, onPermissionRequest: approveAll });
                },
                resumeSession: ({ sessionId, config }) => {
                    checkConfig(config);
                    return client.resumeSession(sessionId, {
                        ...config,
                        onPermissionRequest: approveAll,
                    });
                },
            });
            await expect(
                rpc.sendRequest("host.materializeSession", {
                    hostId: host.hostId,
                    handoffId: "config",
                    resume,
                    config: { sessionId: "configured", configDir: "/selected-config" },
                })
            ).resolves.toEqual({ sessionId: "configured" });
            expect(materialize.mock.calls[0][0]).toMatchObject({
                configDir: "/selected-config",
            });
        }
    );

    it("reports listener-task exit without a process PID or exit code", async () => {
        let rpc!: MessageConnection;
        const client = await fixture((connection) => {
            rpc = connection;
            rpc.onRequest("host.start", ({ hostId }: HostStartRequest) => info(hostId));
        });
        const onExit = vi.fn();
        const host = await client.startAhpHost({ localServer: {}, onExit });
        expect(host.pid).toBeUndefined();
        const exit = { hostId: host.hostId, reason: "exited" };
        await rpc.sendNotification("host.exited", exit);
        await client.ping();
        expect(onExit).toHaveBeenCalledExactlyOnceWith(exit);
        expect(onExit.mock.calls[0][0]).not.toHaveProperty("exitCode");
    });

    it("resumes with the retained original after its creation snapshot was consumed", async () => {
        let rpc!: MessageConnection;
        const create = vi.fn(({ sessionId }: { sessionId: string }) => ({ sessionId }));
        const resume = vi.fn();
        const client = await fixture((connection) => {
            rpc = connection;
            rpc.onRequest("host.start", ({ hostId }: HostStartRequest) => info(hostId));
            rpc.onRequest("session.create", create);
            rpc.onRequest("session.resume", resume);
        });
        const released = vi.fn();
        let original: Awaited<ReturnType<CopilotClient["createSession"]>>;
        const host = await client.startAhpHost({
            localServer: {},
            createSession: async ({ config }) => {
                original = await client.createSession({
                    ...config,
                    onPermissionRequest: approveAll,
                });
                return original;
            },
            resumeSession: async () => original,
            onSessionReleased: released,
        });
        const config = { sessionId: "retained", workingDirectory: "/workspace" };
        await rpc.sendRequest("host.materializeSession", {
            hostId: host.hostId,
            handoffId: "create",
            config,
        });
        await rpc.sendNotification("host.sessionReleased", {
            hostId: host.hostId,
            handoffId: "create",
        });
        await vi.waitFor(() => expect(released).toHaveBeenCalledExactlyOnceWith(original));
        await expect(
            rpc.sendRequest("host.materializeSession", {
                hostId: host.hostId,
                handoffId: "resume",
                resume: true,
                config: { ...config, continuePendingWork: false, suppressResumeEvent: true },
            })
        ).resolves.toEqual({ sessionId: "retained" });
        await rpc.sendNotification("host.sessionReleased", {
            hostId: host.hostId,
            handoffId: "resume",
        });
        await vi.waitFor(() => expect(released).toHaveBeenCalledTimes(2));
        expect(released.mock.calls[1][0]).toBe(original!);
        expect(create).toHaveBeenCalledOnce();
        expect(resume).not.toHaveBeenCalled();
    });

    it.each([false, true])(
        "does not retain configuration snapshots from a failed factory (resume=%s)",
        async (resume) => {
            let rpc!: MessageConnection;
            const materialize = vi.fn(({ sessionId }: { sessionId: string }) => ({ sessionId }));
            const client = await fixture((connection) => {
                rpc = connection;
                rpc.onRequest("host.start", ({ hostId }: HostStartRequest) => info(hostId));
                rpc.onRequest("session.create", materialize);
                rpc.onRequest("session.resume", materialize);
            });
            let original: Awaited<ReturnType<CopilotClient["createSession"]>> | undefined;
            const host = await client.startAhpHost({
                localServer: {},
                createSession: async ({ config }) => {
                    original = await client.createSession({
                        ...config,
                        onPermissionRequest: approveAll,
                    });
                    throw new Error("application setup failed");
                },
                resumeSession: async ({ sessionId, config }) => {
                    if (original) return original;
                    original = await client.resumeSession(sessionId, {
                        ...config,
                        onPermissionRequest: approveAll,
                    });
                    throw new Error("application setup failed");
                },
            });
            const config = { sessionId: "retained", workingDirectory: "/workspace" };
            await expect(
                rpc.sendRequest("host.materializeSession", {
                    hostId: host.hostId,
                    handoffId: "failed",
                    resume,
                    config,
                })
            ).rejects.toThrow("application setup failed");
            await expect(
                rpc.sendRequest("host.materializeSession", {
                    hostId: host.hostId,
                    handoffId: "resume",
                    resume: true,
                    config: { ...config, continuePendingWork: false, suppressResumeEvent: true },
                })
            ).resolves.toEqual({ sessionId: "retained" });
            expect(client["sessions"].get("retained")).toBe(original);
            expect(materialize).toHaveBeenCalledOnce();
        }
    );

    it("registers a distinct resume callback before start and releases the original object", async () => {
        let rpc!: MessageConnection;
        const creates = vi.fn();
        const resumes = vi.fn(({ sessionId }: { sessionId: string }) => ({ sessionId }));
        const client = await fixture((connection) => {
            rpc = connection;
            rpc.onRequest("session.resume", resumes);
            rpc.onRequest("host.start", async (request: HostStartRequest) => {
                expect(request.resumeFactory).toBe(true);
                await rpc.sendRequest("host.materializeSession", {
                    hostId: request.hostId,
                    handoffId: "resume",
                    resume: true,
                    config: {
                        sessionId: "durable",
                        workingDirectory: "/workspace",
                        continuePendingWork: false,
                        suppressResumeEvent: true,
                    },
                });
                return info(request.hostId);
            });
        });
        const released = vi.fn();
        const host = await client.startAhpHost({
            localServer: {},
            createSession: creates,
            resumeSession: ({ sessionId, config }) =>
                client.resumeSession(sessionId, { ...config, onPermissionRequest: approveAll }),
            onSessionReleased: released,
        });
        const original = client["sessions"].get("durable");
        expect(original).toBeDefined();
        expect(creates).not.toHaveBeenCalled();
        expect(resumes).toHaveBeenCalledOnce();
        expect(resumes.mock.calls[0][0]).toMatchObject({
            sessionId: "durable",
            workingDirectory: "/workspace",
            continuePendingWork: false,
            disableResume: true,
        });
        await rpc.sendNotification("host.sessionReleased", {
            hostId: host.hostId,
            handoffId: "resume",
        });
        await vi.waitFor(() => expect(released).toHaveBeenCalledExactlyOnceWith(original));
        expect(client["sessions"].get("durable")).toBe(original);
    });

    it("does not fall back to the create callback for an application resume", async () => {
        let rpc!: MessageConnection;
        const creates = vi.fn();
        const client = await fixture((connection) => {
            rpc = connection;
            rpc.onRequest("host.start", ({ hostId }: HostStartRequest) => info(hostId));
        });
        const host = await client.startAhpHost({ localServer: {}, createSession: creates });
        await expect(
            rpc.sendRequest("host.materializeSession", {
                hostId: host.hostId,
                handoffId: "missing-resume",
                resume: true,
                config: { sessionId: "durable" },
            })
        ).rejects.toThrow("unavailable");
        expect(creates).not.toHaveBeenCalled();
    });

    it.each([false, true])(
        "releases late results without stale captures (resume=%s)",
        async (resume) => {
            let rpc!: MessageConnection;
            let finish!: () => void;
            const gate = new Promise<void>((resolve) => {
                finish = resolve;
            });
            let original: Awaited<ReturnType<CopilotClient["resumeSession"]>> | undefined;
            let signal: AbortSignal | undefined;
            const client = await fixture((connection) => {
                rpc = connection;
                rpc.onRequest("host.start", ({ hostId }: HostStartRequest) => info(hostId));
                rpc.onRequest("session.resume", ({ sessionId }: { sessionId: string }) => ({
                    sessionId,
                }));
                rpc.onRequest("session.create", ({ sessionId }: { sessionId: string }) => ({
                    sessionId,
                }));
            });
            const released = vi.fn();
            const host = await client.startAhpHost({
                localServer: {},
                createSession: async (request) => {
                    signal = request.signal;
                    original = await client.createSession({
                        ...request.config,
                        onPermissionRequest: approveAll,
                    });
                    await gate;
                    return original;
                },
                resumeSession: async (request) => {
                    if (original) return original;
                    signal = request.signal;
                    original = await client.resumeSession(request.sessionId, {
                        ...request.config,
                        onPermissionRequest: approveAll,
                    });
                    await gate;
                    return original;
                },
                onSessionReleased: released,
            });
            const response = rpc.sendRequest("host.materializeSession", {
                hostId: host.hostId,
                handoffId: "late-resume",
                resume,
                config: { sessionId: "durable" },
            });
            const rejected = expect(response).rejects.toThrow("handoff ended");
            await vi.waitFor(() => expect(original).toBeDefined());
            await rpc.sendNotification("host.sessionReleased", {
                hostId: host.hostId,
                handoffId: "late-resume",
            });
            await rejected;
            expect(signal?.aborted).toBe(true);
            expect(released).not.toHaveBeenCalled();
            finish();
            await vi.waitFor(() => expect(released).toHaveBeenCalledExactlyOnceWith(original));
            await expect(
                rpc.sendRequest("host.materializeSession", {
                    hostId: host.hostId,
                    handoffId: "retained-resume",
                    resume: true,
                    config: { sessionId: "durable", continuePendingWork: false },
                })
            ).resolves.toEqual({ sessionId: "durable" });
        }
    );

    it("publishes an existing session using the same owner's host RPC", async () => {
        const publish = vi.fn(({ sessionId }: { sessionId: string }) => ({
            sessionId,
            sessionUri: `copilot:/${sessionId}`,
        }));
        const listSessions = vi.fn((_params: { hostId: string }) => ({ sessions: [] }));
        const client = await fixture((rpc) => {
            rpc.onRequest("host.start", ({ hostId }: HostStartRequest) => info(hostId));
            rpc.onRequest("host.publishSession", publish);
            rpc.onRequest("host.listSessions", listSessions);
        });
        const host = await client.startAhpHost({ localServer: {} });
        await expect(host.publishSession("existing")).resolves.toEqual({
            sessionId: "existing",
            sessionUri: "copilot:/existing",
        });
        expect(publish.mock.calls[0][0]).toEqual({ hostId: host.hostId, sessionId: "existing" });
        await expect(host.listSessions()).resolves.toEqual({ sessions: [] });
        expect(listSessions).toHaveBeenCalledOnce();
        expect(listSessions.mock.calls[0]?.[0]).toEqual({ hostId: host.hostId });
    });
    it("allows the owner callback to create a session while host.start is still pending", async () => {
        const client = await fixture((rpc) => {
            rpc.onRequest("session.create", ({ sessionId }: { sessionId: string }) => ({
                sessionId,
            }));
            rpc.onRequest("host.start", async ({ hostId }: HostStartRequest) => {
                await rpc.sendRequest("host.materializeSession", {
                    hostId,
                    handoffId: "during-start",
                    config: { sessionId: "during-start" },
                });
                return info(hostId);
            });
        });
        const released = vi.fn();
        const host = await client.startAhpHost({
            localServer: {},
            createSession: ({ config }) =>
                client.createSession({
                    ...config,
                    onPermissionRequest: approveAll,
                }),
            onSessionReleased: released,
        });
        const original = client["sessions"].get("during-start");
        expect(original).toBeDefined();
        expect(host.hostId).toBeTruthy();
        expect(released).not.toHaveBeenCalled();
        await client.forceStop();
        expect(released).toHaveBeenCalledExactlyOnceWith(original);
        expect(client["hostSessionFactories"].size).toBe(0);
        expect(client["hostHandoffs"].size).toBe(0);
    });

    it("rejects a different client's object without disconnecting that object", async () => {
        let rpc!: MessageConnection;
        const client = await fixture((connection) => {
            rpc = connection;
            rpc.onRequest("host.start", ({ hostId }: HostStartRequest) => info(hostId));
        });
        const other = await fixture((connection) => {
            connection.onRequest("session.create", ({ sessionId }: { sessionId: string }) => ({
                sessionId,
            }));
        });
        const original = await other.createSession({
            sessionId: "other-client",
            onPermissionRequest: approveAll,
        });
        const released = vi.fn();
        const host = await client.startAhpHost({
            localServer: {},
            createSession: async () => original,
            onSessionReleased: released,
        });
        await expect(
            rpc.sendRequest("host.materializeSession", {
                hostId: host.hostId,
                handoffId: "wrong-owner",
                config: { sessionId: original.sessionId },
            })
        ).rejects.toThrow("this client");
        expect(released).toHaveBeenCalledExactlyOnceWith(original);
        expect(other["sessions"].get(original.sessionId)).toBe(original);
    });

    it("keeps a normal app session and its tools until one release without disconnecting it", async () => {
        let rpc!: MessageConnection;
        const creates = vi.fn(({ sessionId }: { sessionId: string }) => ({ sessionId }));
        const disconnect = vi.fn(() => ({}));
        const client = await fixture((connection) => {
            rpc = connection;
            rpc.onRequest("host.start", ({ hostId }: HostStartRequest) => info(hostId));
            rpc.onRequest("session.create", creates);
            rpc.onRequest("session.detach", disconnect);
        });
        const onSessionReleased = vi.fn();
        const toolHandler = vi.fn(() => "app-result");
        const host = await client.startAhpHost({
            localServer: {},
            createSession: ({ config }) =>
                client.createSession({
                    ...config,
                    onPermissionRequest: approveAll,
                    systemMessage: { mode: "append", content: "app prompt" },
                    tools: [{ name: "app_tool", parameters: {}, handler: toolHandler }],
                }),
            onSessionReleased,
        });
        const result = await rpc.sendRequest("host.materializeSession", {
            hostId: host.hostId,
            handoffId: "participation",
            config: { sessionId: "app-session", workingDirectory: "/workspace", streaming: true },
        });
        expect(result).toEqual({ sessionId: "app-session" });
        const original = client["sessions"].get("app-session");
        expect(original).toBeDefined();
        expect(creates.mock.calls[0]?.[0]).toMatchObject({
            systemMessage: { content: "app prompt" },
            tools: [{ name: "app_tool" }],
        });
        expect(onSessionReleased).not.toHaveBeenCalled();
        await rpc.sendNotification("host.sessionReleased", {
            hostId: host.hostId,
            handoffId: "participation",
        });
        await vi.waitFor(() => expect(onSessionReleased).toHaveBeenCalledExactlyOnceWith(original));
        await rpc.sendNotification("host.sessionReleased", {
            hostId: host.hostId,
            handoffId: "participation",
        });
        await rpc.sendNotification("host.exited", { hostId: host.hostId, reason: "exited" });
        await vi.waitFor(() => expect(client["hostSessionFactories"].size).toBe(0));
        expect(onSessionReleased).toHaveBeenCalledOnce();
        expect(disconnect).not.toHaveBeenCalled();
        expect(client["hostHandoffs"].size).toBe(0);
        expect(client["sessions"].get("app-session")).toBe(original);
    });

    it("cancels a pending handoff and releases a late original object once", async () => {
        let rpc!: MessageConnection;
        const client = await fixture((connection) => {
            rpc = connection;
            rpc.onRequest("host.start", ({ hostId }: HostStartRequest) => info(hostId));
            rpc.onRequest("session.create", ({ sessionId }: { sessionId: string }) => ({
                sessionId,
            }));
        });
        let finish!: () => void;
        const pending = new Promise<void>((resolve) => {
            finish = resolve;
        });
        let signal: AbortSignal | undefined;
        let original: Awaited<ReturnType<typeof client.createSession>> | undefined;
        const released = vi.fn();
        const host = await client.startAhpHost({
            localServer: {},
            createSession: async (request) => {
                signal = request.signal;
                await pending;
                original = await client.createSession({
                    ...request.config,
                    onPermissionRequest: approveAll,
                });
                return original;
            },
            onSessionReleased: released,
        });
        const creation = rpc.sendRequest("host.materializeSession", {
            hostId: host.hostId,
            handoffId: "pending",
            config: { sessionId: "late-session" },
        });
        const failed = expect(creation).rejects.toThrow("handoff ended");
        await vi.waitFor(() => expect(signal).toBeDefined());
        await rpc.sendNotification("host.sessionReleased", {
            hostId: host.hostId,
            handoffId: "pending",
        });
        await failed;
        expect(signal?.aborted).toBe(true);
        expect(released).not.toHaveBeenCalled();
        finish();
        await vi.waitFor(() => expect(released).toHaveBeenCalledExactlyOnceWith(original));
        expect(client["hostHandoffs"].size).toBe(0);
    });

    it("rejects ignored host settings and releases the returned object", async () => {
        let rpc!: MessageConnection;
        const client = await fixture((connection) => {
            rpc = connection;
            rpc.onRequest("host.start", ({ hostId }: HostStartRequest) => info(hostId));
            rpc.onRequest("session.create", ({ sessionId }: { sessionId: string }) => ({
                sessionId,
            }));
        });
        const released = vi.fn();
        const host = await client.startAhpHost({
            localServer: {},
            createSession: ({ config }) =>
                client.createSession({
                    sessionId: config.sessionId,
                    onPermissionRequest: approveAll,
                }),
            onSessionReleased: released,
        });
        await expect(
            rpc.sendRequest("host.materializeSession", {
                hostId: host.hostId,
                handoffId: "wrong-config",
                config: { sessionId: "selected-session", enableConfigDiscovery: true },
            })
        ).rejects.toThrow("preserve");
        expect(released).toHaveBeenCalledExactlyOnceWith(
            client["sessions"].get("selected-session")
        );
    });

    it("supports explicit local defaults and token-free responses without retaining callbacks", async () => {
        const start = vi.fn(({ hostId }: HostStartRequest) => {
            const { token: _token, ...withoutToken } = info(hostId);
            return withoutToken;
        });
        const client = await fixture((rpc) => rpc.onRequest("host.start", start));

        const host = await client.startAhpHost({ localServer: {} });
        expect(start.mock.calls[0]?.[0]).toEqual({ hostId: host.hostId, localServer: {} });
        expect(host.token).toBeUndefined();
        expect(client["hostExitCallbacks"].size).toBe(0);
    });

    it("leaves defaults to the runtime and forwards every disposal without synthesizing exits", async () => {
        const start = vi.fn(({ hostId }: HostStartRequest) => info(hostId));
        const dispose = vi.fn((_params: { hostId: string }) => ({}));
        const onExit = vi.fn();
        const client = await fixture((rpc) => {
            rpc.onRequest("host.start", start);
            rpc.onRequest("host.dispose", dispose);
        });

        const host = await client.startAhpHost({ localServer: {}, onExit });
        expect(start).toHaveBeenCalledOnce();
        expect(host.hostId).toMatch(/^[a-f0-9-]{36}$/);
        expect(start.mock.calls[0]?.[0]).toEqual({ hostId: host.hostId, localServer: {} });
        expect(client).not.toHaveProperty("startHost");
        await Promise.all([host.dispose(), host.dispose()]);
        await host.dispose();
        await host[Symbol.asyncDispose]();
        expect(dispose).toHaveBeenCalledTimes(4);
        expect(dispose.mock.calls.map(([params]) => params)).toEqual(
            Array.from({ length: 4 }, () => ({ hostId: host.hostId }))
        );
        expect(onExit).not.toHaveBeenCalled();
    });

    it("forwards only wire options, preserving explicit values and generating its own ID", async () => {
        const start = vi.fn(({ hostId }: HostStartRequest) => info(hostId));
        const client = await fixture((rpc) => rpc.onRequest("host.start", start));
        const onExit = Object.assign(vi.fn(), { toJSON: () => "must-not-serialize" });
        const options: AhpHostOptions & { hostId: string; workingDirectory: string } = {
            localServer: {
                hostname: "::1",
                port: 0,
                token: "explicit-test-token",
                requireConnectionToken: false,
            },
            onExit,
            hostId: "caller-cannot-select-id",
            workingDirectory: "/not-sent",
        };

        const host = await client.startAhpHost(options);
        expect(start.mock.calls[0]?.[0]).toEqual({
            hostId: host.hostId,
            localServer: options.localServer,
        });
        expect(host.hostId).not.toBe(options.hostId);
        expect(onExit).not.toHaveBeenCalled();
    });

    it("leaves listener validation and startup errors to the runtime", async () => {
        const start = vi.fn(() => {
            throw new Error("Runtime rejected listener configuration");
        });
        const client = await fixture((rpc) => rpc.onRequest("host.start", start));
        const options = {
            localServer: {
                hostname: "",
                port: -1,
                token: "",
                requireConnectionToken: true,
            },
        };

        await expect(client.startAhpHost(options)).rejects.toThrow(
            "Runtime rejected listener configuration"
        );
        expect(start).toHaveBeenCalledExactlyOnceWith(
            expect.objectContaining(options),
            expect.anything()
        );
    });

    it("does not lose an exit delivered before the start response", async () => {
        const onExit = vi.fn();
        const client = await fixture((rpc) => {
            rpc.onRequest("host.start", async ({ hostId }: HostStartRequest) => {
                await rpc.sendNotification("host.exited", {
                    hostId,
                    reason: "exited",
                    error: "hosting task failed",
                });
                return info(hostId);
            });
        });

        const host = await client.startAhpHost({ localServer: {}, onExit });
        expect(onExit).toHaveBeenCalledExactlyOnceWith({
            hostId: host.hostId,
            reason: "exited",
            error: "hosting task failed",
        });
        expect(client["hostExitCallbacks"].size).toBe(0);
    });

    it("propagates startup errors and releases the callback registration", async () => {
        const onExit = vi.fn();
        const client = await fixture((rpc) => {
            rpc.onRequest("host.start", () => {
                throw new Error("Failed to bind AHP listener: address already in use");
            });
        });

        await expect(client.startAhpHost({ localServer: {}, onExit })).rejects.toThrow(
            "address already in use"
        );
        expect(client["hostExitCallbacks"].size).toBe(0);
        await client.forceStop();
        expect(onExit).not.toHaveBeenCalled();
    });

    it("releases early-exit registrations even if startup subsequently fails", async () => {
        const onExit = vi.fn();
        const client = await fixture((rpc) => {
            rpc.onRequest("host.start", async ({ hostId }: HostStartRequest) => {
                await rpc.sendNotification("host.exited", { hostId, reason: "exited" });
                throw new Error("Startup failed after exit");
            });
        });

        await expect(client.startAhpHost({ localServer: {}, onExit })).rejects.toThrow(
            "Startup failed after exit"
        );
        expect(onExit).toHaveBeenCalledOnce();
        expect(client["hostExitCallbacks"].size).toBe(0);
        await client.forceStop();
        expect(onExit).toHaveBeenCalledOnce();
    });

    it("rejects startup when the owner connection closes before readiness", async () => {
        const onExit = vi.fn();
        const client = await fixture((rpc, socket) => {
            rpc.onRequest("host.start", () => {
                socket.destroy();
                return new Promise<never>(() => {});
            });
        });

        await expect(client.startAhpHost({ localServer: {}, onExit })).rejects.toThrow();
        expect(onExit).toHaveBeenCalledExactlyOnceWith(
            expect.objectContaining({ reason: "ownerDisconnected" })
        );
        expect(client["hostExitCallbacks"].size).toBe(0);
    });

    it("drains notifications and successful responses received immediately before EOF", async () => {
        const client = await fixture((rpc, socket, writer) => {
            rpc.onRequest("host.start", ({ hostId }: HostStartRequest) => info(hostId));
            rpc.onRequest("ping", () => new Promise<never>(() => {}));
            const write = writer.write.bind(writer);
            vi.spyOn(writer, "write").mockImplementation((message) => {
                if (
                    "result" in message &&
                    typeof message.result === "object" &&
                    message.result !== null &&
                    "hostId" in message.result
                ) {
                    const hostId = message.result.hostId;
                    const messages = [
                        ...Array.from({ length: 16 }, (_, index) => ({
                            jsonrpc: "2.0",
                            method: "host.exited",
                            params: {
                                hostId: index === 15 ? hostId : `unrelated-${index}`,
                                reason: "exited",
                                exitCode: 0,
                            },
                        })),
                        message,
                    ];
                    socket.end(
                        messages
                            .map((value) => {
                                const body = JSON.stringify(value);
                                return `Content-Length: ${Buffer.byteLength(body)}\r\n\r\n${body}`;
                            })
                            .join("")
                    );
                    return Promise.resolve();
                }
                return write(message);
            });
        });
        await client.start();
        const unanswered = expect(client.ping()).rejects.toThrow();
        const onExit = vi.fn();
        const host = await client.startAhpHost({ localServer: {}, onExit });
        expect(onExit).toHaveBeenCalledExactlyOnceWith({
            hostId: host.hostId,
            reason: "exited",
            exitCode: 0,
        });
        await unanswered;
    });

    it("suppresses closed-stream response failures when EOF cancels a pending factory", async () => {
        let rpc!: MessageConnection;
        let socket!: Socket;
        const client = await fixture((connection, transport) => {
            rpc = connection;
            socket = transport;
            rpc.onRequest("host.start", ({ hostId }: HostStartRequest) => info(hostId));
        });
        let signal: AbortSignal | undefined;
        const onExit = vi.fn();
        const host = await client.startAhpHost({
            localServer: {},
            createSession: (request) => {
                signal = request.signal;
                return new Promise<never>(() => {});
            },
            onExit,
        });
        const pending = rpc.sendRequest("host.materializeSession", {
            hostId: host.hostId,
            handoffId: "pending",
            config: { sessionId: "pending" },
        });
        const rejected = expect(pending).rejects.toThrow();
        await vi.waitFor(() => expect(signal).toBeDefined());
        const writer = client["messageWriter"]!;
        const write = writer.write.bind(writer);
        const failures: unknown[] = [];
        const response = vi.spyOn(writer, "write").mockImplementation((message) => {
            const result = write(message);
            void result.catch((error) => failures.push(error));
            return result;
        });
        const closedWrite = vi
            .spyOn(StreamMessageWriter.prototype, "write")
            .mockRejectedValueOnce(
                Object.assign(new Error("stream destroyed"), { code: "ERR_STREAM_DESTROYED" })
            );
        onTestFinished(() => closedWrite.mockRestore());
        socket.end();
        await vi.waitFor(() => expect(response).toHaveBeenCalledOnce());
        rpc.dispose();
        await rejected;
        await expect(response.mock.results[0].value).resolves.toBeUndefined();
        expect(failures).toEqual([]);
        expect(signal!.aborted).toBe(true);
        expect(onExit).toHaveBeenCalledExactlyOnceWith(
            expect.objectContaining({ reason: "ownerDisconnected" })
        );
    });

    it("keeps concurrent hosts independently disposable", async () => {
        const dispose = vi.fn();
        const client = await fixture((rpc) => {
            rpc.onRequest("host.start", ({ hostId }: HostStartRequest) => info(hostId));
            rpc.onRequest("host.dispose", async ({ hostId }: { hostId: string }) => {
                dispose(hostId);
                await rpc.sendNotification("host.exited", { hostId, reason: "disposed" });
                return {};
            });
        });
        const firstExit = vi.fn();
        const secondExit = vi.fn();
        const [first, second] = await Promise.all([
            client.startAhpHost({ localServer: {}, onExit: firstExit }),
            client.startAhpHost({ localServer: {}, onExit: secondExit }),
        ]);

        expect(first.hostId).not.toBe(second.hostId);
        await first.dispose();
        expect(firstExit).toHaveBeenCalledOnce();
        expect(secondExit).not.toHaveBeenCalled();
        await second.dispose();
        expect(secondExit).toHaveBeenCalledOnce();
        expect(dispose.mock.calls.map(([hostId]) => hostId)).toEqual([first.hostId, second.hostId]);
        expect(client["hostExitCallbacks"].size).toBe(0);
    });

    it("routes exits by ID at most once and still forwards disposal after exit", async () => {
        let serverRpc!: MessageConnection;
        const dispose = vi.fn(() => ({}));
        const onExit = vi.fn();
        const client = await fixture((rpc) => {
            serverRpc = rpc;
            rpc.onRequest("host.start", ({ hostId }: HostStartRequest) => info(hostId));
            rpc.onRequest("host.dispose", dispose);
        });
        const host = await client.startAhpHost({ localServer: {}, onExit });
        await serverRpc.sendNotification("host.exited", { hostId: "unrelated", reason: "exited" });
        await client.ping();
        expect(onExit).not.toHaveBeenCalled();
        const exit = {
            hostId: host.hostId,
            reason: "runtimeShutdown",
            exitCode: null,
            error: null,
        };
        await serverRpc.sendNotification("host.exited", exit);
        await serverRpc.sendNotification("host.exited", exit);
        await client.ping();
        expect(onExit).toHaveBeenCalledExactlyOnceWith(exit);
        expect(client["hostExitCallbacks"].size).toBe(0);
        await Promise.all([host.dispose(), host.dispose()]);
        expect(dispose).toHaveBeenCalledTimes(2);
        await client.forceStop();
        expect(onExit).toHaveBeenCalledOnce();
    });

    it("does not retry failed disposal or report a synthetic exit", async () => {
        const onExit = vi.fn();
        const dispose = vi.fn(() => {
            throw new Error("Runtime cleanup failed");
        });
        const client = await fixture((rpc) => {
            rpc.onRequest("host.start", ({ hostId }: HostStartRequest) => info(hostId));
            rpc.onRequest("host.dispose", dispose);
        });
        const host = await client.startAhpHost({ localServer: {}, onExit });
        await expect(host.dispose()).rejects.toThrow("Runtime cleanup failed");
        expect(dispose).toHaveBeenCalledOnce();
        expect(onExit).not.toHaveBeenCalled();
        await expect(host.dispose()).rejects.toThrow("Runtime cleanup failed");
        expect(dispose).toHaveBeenCalledTimes(2);
    });

    it.each(["stop", "forceStop"] as const)(
        "%s reports disconnection without trying host disposal",
        async (method) => {
            const dispose = vi.fn(() => ({}));
            const onExit = vi.fn();
            const client = await fixture((rpc) => {
                rpc.onRequest("host.start", ({ hostId }: HostStartRequest) => info(hostId));
                rpc.onRequest("host.dispose", dispose);
            });
            await client.startAhpHost({ localServer: {}, onExit });
            await client[method]();
            expect(onExit).toHaveBeenCalledExactlyOnceWith(
                expect.objectContaining({ reason: "ownerDisconnected" })
            );
            expect(client["hostExitCallbacks"].size).toBe(0);
            expect(dispose).not.toHaveBeenCalled();
        }
    );

    it.each([false, true])(
        "logs callback errors without breaking early-exit RPC handling (async: %s)",
        async (asyncCallback) => {
            const error = new Error("Consumer callback failed");
            const log = vi.spyOn(console, "error").mockImplementation(() => {});
            onTestFinished(() => log.mockRestore());
            const onExit = vi.fn(() => {
                if (asyncCallback) return Promise.reject(error);
                throw error;
            });
            const client = await fixture((rpc) => {
                rpc.onRequest("host.start", async ({ hostId }: HostStartRequest) => {
                    await rpc.sendNotification("host.exited", { hostId, reason: "exited" });
                    return info(hostId);
                });
            });

            const host = await client.startAhpHost({ localServer: {}, onExit });
            expect(onExit).toHaveBeenCalledOnce();
            expect(log).toHaveBeenCalledExactlyOnceWith("AHP host exit callback failed", {
                hostId: host.hostId,
                error,
            });
            expect(client["hostExitCallbacks"].size).toBe(0);
            await expect(client.ping()).resolves.toMatchObject({ message: "ok" });
        }
    );

    it("continues disconnect cleanup when a callback throws", async () => {
        const error = new Error("Consumer callback failed");
        const log = vi.spyOn(console, "error").mockImplementation(() => {});
        onTestFinished(() => log.mockRestore());
        const client = await fixture((rpc) => {
            rpc.onRequest("host.start", ({ hostId }: HostStartRequest) => info(hostId));
        });
        const first = await client.startAhpHost({
            localServer: {},
            onExit: () => {
                throw error;
            },
        });
        const secondExit = vi.fn();
        await client.startAhpHost({ localServer: {}, onExit: secondExit });
        await client.forceStop();
        expect(log).toHaveBeenCalledExactlyOnceWith("AHP host exit callback failed", {
            hostId: first.hostId,
            error,
        });
        expect(secondExit).toHaveBeenCalledOnce();
        expect(client["hostExitCallbacks"].size).toBe(0);
    });

    it("reports owner disconnect without disposal RPCs and never reclaims on reconnect", async () => {
        let disconnect: (() => void) | undefined;
        const start = vi.fn(({ hostId }: HostStartRequest) => info(hostId));
        const dispose = vi.fn(() => ({}));
        const onExit = vi.fn();
        const client = await fixture((rpc, socket) => {
            rpc.onRequest("host.start", start);
            rpc.onRequest("host.dispose", dispose);
            disconnect = () => socket.destroy();
        });

        const host = await client.startAhpHost({ localServer: {}, onExit });
        disconnect?.();
        await vi.waitFor(() =>
            expect(onExit).toHaveBeenCalledExactlyOnceWith(
                expect.objectContaining({
                    reason: "ownerDisconnected",
                    error: expect.stringContaining("cannot be acknowledged"),
                })
            )
        );
        expect(client["hostExitCallbacks"].size).toBe(0);
        await client.forceStop();
        await client.start();
        expect(start).toHaveBeenCalledOnce();
        await expect(host.dispose()).rejects.toThrow();
        expect(dispose).not.toHaveBeenCalled();
        expect(onExit).toHaveBeenCalledOnce();
    });
});
