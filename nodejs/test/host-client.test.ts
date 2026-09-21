import { once } from "node:events";
import { createServer, type Socket } from "node:net";
import { describe, expect, it, onTestFinished, vi } from "vitest";
import {
    createMessageConnection,
    type MessageConnection,
    StreamMessageReader,
    StreamMessageWriter,
} from "vscode-jsonrpc/node.js";
import { CopilotClient, RuntimeConnection } from "../src/index.js";
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

describe("CopilotClient.startHost", () => {
    it("starts through generated RPC and disposes once", async () => {
        const start = vi.fn(({ hostId }: HostStartRequest) => info(hostId));
        const dispose = vi.fn((_params: { hostId: string }) => ({}));
        const client = await fixture((rpc) => {
            rpc.onRequest("host.start", start);
            rpc.onRequest("host.dispose", dispose);
        });

        const host = await client.startHost();
        expect(start).toHaveBeenCalledOnce();
        expect(host.hostId).toMatch(/^[a-f0-9-]{36}$/);
        await Promise.all([host.dispose(), host.dispose()]);
        expect(dispose).toHaveBeenCalledOnce();
        expect(dispose.mock.calls[0]?.[0]).toEqual({ hostId: host.hostId });
        await expect(host.closed).resolves.toMatchObject({ reason: "disposed" });
    });

    it("does not lose an exit delivered before the start response", async () => {
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

        const host = await client.startHost();
        await expect(host.closed).resolves.toMatchObject({
            hostId: host.hostId,
            reason: "exited",
            error: "child failed",
        });
        await host.dispose();
    });

    it("propagates startup errors", async () => {
        const client = await fixture((rpc) => {
            rpc.onRequest("host.start", () => {
                throw new Error("Configured copilotd-lite executable does not exist");
            });
        });

        await expect(client.startHost()).rejects.toThrow("executable does not exist");
    });

    it("rejects startup when the owner connection closes before readiness", async () => {
        const client = await fixture((rpc, socket) => {
            rpc.onRequest("host.start", () => {
                socket.destroy();
                return new Promise<never>(() => {});
            });
        });

        await expect(client.startHost()).rejects.toThrow();
    });

    it("drains notifications and successful responses received immediately before EOF", async () => {
        const client = await fixture((rpc, socket, writer) => {
            rpc.onRequest("host.start", ({ hostId }: HostStartRequest) => info(hostId));
            rpc.onRequest("ping", () => new Promise<never>(() => {}));
            const write = writer.write.bind(writer);
            vi.spyOn(writer, "write").mockImplementation((message) => {
                if ("result" in message && message.result?.hostId) {
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
        const host = await client.startHost();
        await expect(host.closed).resolves.toMatchObject({
            hostId: host.hostId,
            reason: "exited",
            exitCode: 0,
        });
        await unanswered;
    });

    it("keeps concurrent hosts independently disposable", async () => {
        const dispose = vi.fn((_params: { hostId: string }) => ({}));
        const client = await fixture((rpc) => {
            rpc.onRequest("host.start", ({ hostId }: HostStartRequest) => info(hostId));
            rpc.onRequest("host.dispose", dispose);
        });
        const [first, second] = await Promise.all([client.startHost(), client.startHost()]);
        const secondClosed = vi.fn();
        void second.closed.then(secondClosed);

        expect(first.hostId).not.toBe(second.hostId);
        await first.dispose();
        await first.closed;
        expect(secondClosed).not.toHaveBeenCalled();
        await second.dispose();
        await second.closed;
        expect(dispose.mock.calls.map(([params]) => params.hostId)).toEqual([
            first.hostId,
            second.hostId,
        ]);
    });

    it("settles handles on owner disconnect and never reclaims on reconnect", async () => {
        let disconnect: (() => void) | undefined;
        const start = vi.fn(({ hostId }: HostStartRequest) => info(hostId));
        const client = await fixture((rpc, socket) => {
            rpc.onRequest("host.start", start);
            disconnect = () => socket.destroy();
        });

        const host = await client.startHost();
        disconnect?.();
        await expect(host.closed).resolves.toMatchObject({ reason: "ownerDisconnected" });
        await client.forceStop();
        await client.start();
        expect(start).toHaveBeenCalledOnce();
        await host.dispose();
    });
});
