import { once } from "node:events";
import { createServer, type Socket } from "node:net";
import { describe, expect, it, onTestFinished, vi } from "vitest";
import {
    createMessageConnection,
    type MessageConnection,
    StreamMessageReader,
    StreamMessageWriter,
} from "vscode-jsonrpc/node.js";
import { CopilotClient, RuntimeConnection, type AhpHostOptions } from "../src/index.js";
import type { HostStartRequest } from "../src/generated/rpc.js";

// Wire-level SDK unit tests. Real child/listener coverage lives in test/e2e.
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
    return { hostId, url: "ws://127.0.0.1:54321", token: "test-only-token", pid: 1234 };
}

describe("CopilotClient.startAhpHost", () => {
    it("supports omitted options and token-free responses without retaining callbacks", async () => {
        const start = vi.fn(({ hostId }: HostStartRequest) => {
            const { token: _token, ...withoutToken } = info(hostId);
            return withoutToken;
        });
        const client = await fixture((rpc) => rpc.onRequest("host.start", start));

        const host = await client.startAhpHost();
        expect(start.mock.calls[0]?.[0]).toEqual({ hostId: host.hostId });
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

        const host = await client.startAhpHost({ onExit });
        expect(start).toHaveBeenCalledOnce();
        expect(host.hostId).toMatch(/^[a-f0-9-]{36}$/);
        expect(start.mock.calls[0]?.[0]).toEqual({ hostId: host.hostId });
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
            hostname: "::1",
            port: 0,
            token: "explicit-test-token",
            requireConnectionToken: false,
            onExit,
            hostId: "caller-cannot-select-id",
            workingDirectory: "/not-sent",
        };

        const host = await client.startAhpHost(options);
        expect(start.mock.calls[0]?.[0]).toEqual({
            hostId: host.hostId,
            hostname: "::1",
            port: 0,
            token: "explicit-test-token",
            requireConnectionToken: false,
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
            hostname: "",
            port: -1,
            token: "",
            requireConnectionToken: true,
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
                    exitCode: 1,
                    error: "child failed",
                });
                return info(hostId);
            });
        });

        const host = await client.startAhpHost({ onExit });
        expect(onExit).toHaveBeenCalledExactlyOnceWith({
            hostId: host.hostId,
            reason: "exited",
            exitCode: 1,
            error: "child failed",
        });
        expect(client["hostExitCallbacks"].size).toBe(0);
    });

    it("propagates startup errors and releases the callback registration", async () => {
        const onExit = vi.fn();
        const client = await fixture((rpc) => {
            rpc.onRequest("host.start", () => {
                throw new Error("Configured copilotd-lite executable does not exist");
            });
        });

        await expect(client.startAhpHost({ onExit })).rejects.toThrow("executable does not exist");
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

        await expect(client.startAhpHost({ onExit })).rejects.toThrow("Startup failed after exit");
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

        await expect(client.startAhpHost({ onExit })).rejects.toThrow();
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
        const host = await client.startAhpHost({ onExit });
        expect(onExit).toHaveBeenCalledExactlyOnceWith({
            hostId: host.hostId,
            reason: "exited",
            exitCode: 0,
        });
        await unanswered;
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
            client.startAhpHost({ onExit: firstExit }),
            client.startAhpHost({ onExit: secondExit }),
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
        const host = await client.startAhpHost({ onExit });
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
        const host = await client.startAhpHost({ onExit });
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
            await client.startAhpHost({ onExit });
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

            const host = await client.startAhpHost({ onExit });
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
            onExit: () => {
                throw error;
            },
        });
        const secondExit = vi.fn();
        await client.startAhpHost({ onExit: secondExit });
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

        const host = await client.startAhpHost({ onExit });
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
