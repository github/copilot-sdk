/* eslint-disable @typescript-eslint/no-explicit-any */
import { PassThrough } from "node:stream";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
    createMessageConnection,
    StreamMessageReader,
    StreamMessageWriter,
} from "vscode-jsonrpc/node.js";
import { CopilotClient, type AhpTransport } from "../src/index.js";

function deferred<T = unknown>() {
    let resolve!: (value: T) => void;
    const promise = new Promise<T>((done) => {
        resolve = done;
    });
    return { promise, resolve };
}

function harness() {
    const client = new CopilotClient({ autoStart: false });
    const rpc = {
        sendRequest: vi.fn().mockResolvedValue({}),
        onRequest: vi.fn(),
        onNotification: vi.fn(),
        onClose: vi.fn(),
        onError: vi.fn(),
        dispose: vi.fn(),
    };
    (client as any).connection = rpc;
    (client as any).attachConnectionHandlers();
    const transport: AhpTransport = { send: vi.fn().mockResolvedValue(undefined), close: vi.fn() };
    const endpoints = (client as any).ahpEndpoints as Map<string, any>;
    const send = (endpointId: string, connectionId: string, message: string) =>
        rpc.onRequest.mock.calls.find(([method]) => method === "ahpTransport.send")![1]({
            endpointId,
            connectionId,
            message,
        });
    const ids = () => {
        const [endpointId, endpoint] = [...endpoints][0];
        return {
            endpointId,
            connectionId: [...endpoint.connections.keys()][0] as string,
            endpoint,
        };
    };
    return { client, rpc, transport, endpoints, send, ids };
}

afterEach(() => vi.useRealTimers());

describe("app-owned AHP transport", () => {
    it("accepts synchronously and closes a late open without retaining callbacks", async () => {
        const h = harness();
        const endpoint = await h.client.createAhpEndpoint();
        const open = deferred();
        h.rpc.sendRequest.mockImplementation((method) =>
            method === "ahp.openConnection" ? open.promise : Promise.resolve({})
        );
        const connection = endpoint.acceptConnection(h.transport);
        expect(connection).not.toBeInstanceOf(Promise);
        const registry = h.ids().endpoint.connections;
        const ending = connection.end();
        expect(registry.size).toBe(0);
        expect(h.transport.close).toHaveBeenCalledOnce();
        expect((connection as any).transport).toBeUndefined();
        await expect(connection.closed).resolves.toBeUndefined();
        await ending;
        expect(
            h.rpc.sendRequest.mock.calls.filter(([m]) => m === "ahp.closeConnection")
        ).toHaveLength(1);
        open.resolve({});
        await vi.waitFor(() =>
            expect(
                h.rpc.sendRequest.mock.calls.filter(([m]) => m === "ahp.closeConnection")
            ).toHaveLength(2)
        );
        await connection.end();
        expect(
            h.rpc.sendRequest.mock.calls.filter(([m]) => m === "ahp.closeConnection")
        ).toHaveLength(2);
        await endpoint.dispose();
    });

    it("sends a fresh dispose after endpoint creation succeeds beyond early disposal", async () => {
        const h = harness();
        const create = deferred();
        h.rpc.sendRequest.mockImplementation((method) =>
            method === "ahp.createEndpoint" ? create.promise : Promise.resolve({})
        );
        const creation = h.client.createAhpEndpoint();
        const rejected = expect(creation).rejects.toThrow("AHP endpoint disposed");
        const endpoint = [...h.endpoints.values()][0];
        await endpoint.dispose();
        await rejected;
        expect(h.endpoints.size).toBe(0);
        expect(
            h.rpc.sendRequest.mock.calls.filter(([m]) => m === "ahp.disposeEndpoint")
        ).toHaveLength(1);
        create.resolve({});
        await vi.waitFor(() =>
            expect(
                h.rpc.sendRequest.mock.calls.filter(([m]) => m === "ahp.disposeEndpoint")
            ).toHaveLength(2)
        );
        expect(h.endpoints.size).toBe(0);
    });

    it("aborts a blocked consumer send and releases its request immediately", async () => {
        const h = harness();
        const endpoint = await h.client.createAhpEndpoint();
        const started = deferred<AbortSignal>();
        h.transport.send = vi.fn((_text, signal) => {
            started.resolve(signal);
            return new Promise(() => {});
        });
        const connection = endpoint.acceptConnection(h.transport);
        const { endpointId, connectionId } = h.ids();
        const sending = h.send(endpointId, connectionId, '{"opaque":true}');
        const rejected = expect(sending).rejects.toThrow("AHP connection closed");
        const signal = await started.promise;
        const ending = connection.end();
        expect(signal.aborted).toBe(true);
        expect(h.transport.close).toHaveBeenCalledOnce();
        await rejected;
        await ending;
        await endpoint.dispose();
    });

    it("releases repeated connections and installs only one transport dispatcher", async () => {
        const h = harness();
        const endpoint = await h.client.createAhpEndpoint();
        const seen = new Set<string>();
        for (let i = 0; i < 30; i++) {
            const connection = endpoint.acceptConnection(h.transport);
            const { endpointId, connectionId, endpoint: internal } = h.ids();
            expect(seen.has(connectionId)).toBe(false);
            seen.add(connectionId);
            await connection.end();
            expect(internal.connections.size).toBe(0);
            await expect(h.send(endpointId, connectionId, "{}")).rejects.toThrow(
                "Unknown AHP connection"
            );
        }
        expect(h.rpc.onRequest.mock.calls.filter(([m]) => m === "ahpTransport.send")).toHaveLength(
            1
        );
        expect(
            h.rpc.onNotification.mock.calls.filter(([m]) => m === "ahpTransport.closed")
        ).toHaveLength(1);
        await endpoint.dispose();
        await endpoint.dispose();
        expect(h.endpoints.size).toBe(0);
        expect(() => endpoint.acceptConnection(h.transport)).toThrow("disposed");
    });

    it("handles runtime closure as a notification without a recursive close RPC", async () => {
        const h = harness();
        const endpoint = await h.client.createAhpEndpoint();
        const connection = endpoint.acceptConnection(h.transport);
        const { endpointId, connectionId, endpoint: internal } = h.ids();
        await connection.receive("ready");
        const notify = h.rpc.onNotification.mock.calls.find(
            ([method]) => method === "ahpTransport.closed"
        )![1];
        await notify({ endpointId, connectionId, error: "runtime ended connection" });
        await expect(connection.closed).rejects.toThrow("runtime ended connection");
        expect(internal.connections.size).toBe(0);
        expect(h.transport.close).toHaveBeenCalledOnce();
        expect(h.rpc.sendRequest.mock.calls.some(([m]) => m === "ahp.closeConnection")).toBe(false);
        await endpoint.dispose();
    });

    it("forwards opaque text in order and assembles split UTF-8 code points", async () => {
        const h = harness();
        const endpoint = await h.client.createAhpEndpoint();
        const connection = endpoint.acceptConnection(h.transport);
        const first = deferred();
        h.rpc.sendRequest.mockImplementation((method, params) =>
            method === "ahp.receive" && params.message === "not JSON"
                ? first.promise
                : Promise.resolve({})
        );
        const a = connection.receive("not JSON");
        const b = connection.receive("second");
        await vi.waitFor(() =>
            expect(h.rpc.sendRequest).toHaveBeenCalledWith(
                "ahp.receive",
                expect.objectContaining({ message: "not JSON" })
            )
        );
        expect(h.rpc.sendRequest.mock.calls.some(([, p]) => p.message === "second")).toBe(false);
        first.resolve({});
        await Promise.all([a, b]);
        const bytes = new TextEncoder().encode('{"value":"😀"}');
        await connection.receiveChunk(bytes.subarray(0, 12), { endOfMessage: false });
        await connection.receiveChunk(bytes.subarray(12), { endOfMessage: true });
        expect(
            h.rpc.sendRequest.mock.calls
                .filter(([m]) => m === "ahp.receive")
                .map(([, p]) => p.message)
        ).toEqual(["not JSON", "second", '{"value":"😀"}']);
        await endpoint.dispose();
    });

    it("bounds fragmented input and queued bytes and clears buffers on failure", async () => {
        const h = harness();
        const endpoint = await h.client.createAhpEndpoint();
        const connection = endpoint.acceptConnection(h.transport);
        await connection.receiveChunk(new Uint8Array(8 * 1024 * 1024), { endOfMessage: false });
        await expect(
            connection.receiveChunk(new Uint8Array(1), { endOfMessage: true })
        ).rejects.toThrow("8 MiB");
        await expect(connection.closed).rejects.toThrow("8 MiB");
        expect((connection as any).chunks.byteLength).toBe(0);
        expect(h.ids().endpoint.connections.size).toBe(0);
        await endpoint.dispose();
    });

    it("serializes outgoing writes and closes on a bounded send deadline", async () => {
        vi.useFakeTimers();
        const h = harness();
        const endpoint = await h.client.createAhpEndpoint();
        const started = deferred();
        h.transport.send = vi.fn(() => {
            started.resolve({});
            return new Promise(() => {});
        });
        const connection = endpoint.acceptConnection(h.transport);
        const { endpointId, connectionId } = h.ids();
        const a = h.send(endpointId, connectionId, "a");
        const b = h.send(endpointId, connectionId, "b");
        const rejects = [expect(a).rejects.toThrow("timed out"), expect(b).rejects.toThrow()];
        await started.promise;
        expect(h.transport.send).toHaveBeenCalledTimes(1);
        await vi.advanceTimersByTimeAsync(10_000);
        await Promise.all(rejects);
        await expect(connection.closed).rejects.toThrow("timed out");
        expect(h.transport.close).toHaveBeenCalledOnce();
        await endpoint.dispose();
    });

    it("surfaces close callback errors without preventing registry cleanup", async () => {
        const h = harness();
        const endpoint = await h.client.createAhpEndpoint();
        h.transport.close = () => {
            throw new Error("physical close failed");
        };
        const connection = endpoint.acceptConnection(h.transport);
        await expect(connection.end()).rejects.toThrow("physical close failed");
        await expect(connection.closed).rejects.toThrow("physical close failed");
        expect(h.ids().endpoint.connections.size).toBe(0);
        await endpoint.dispose();
    });

    it.each(["stop", "forceStop"] as const)(
        "%s retires all transport ownership",
        async (method) => {
            const h = harness();
            const endpoint = await h.client.createAhpEndpoint();
            const connection = endpoint.acceptConnection(h.transport);
            await h.client[method]();
            expect(h.endpoints.size).toBe(0);
            expect(h.transport.close).toHaveBeenCalledOnce();
            expect((connection as any).transport).toBeUndefined();
        }
    );

    it("disposes dead SDK RPC replies, pending receives, and endpoint creation", async () => {
        const inbound = new PassThrough();
        const outbound = new PassThrough();
        const rpc = createMessageConnection(
            new StreamMessageReader(inbound),
            new StreamMessageWriter(outbound)
        );
        const server = createMessageConnection(
            new StreamMessageReader(outbound),
            new StreamMessageWriter(inbound)
        );
        const client = new CopilotClient({ autoStart: false });
        (client as any).connection = rpc;
        (client as any).attachConnectionHandlers();
        rpc.listen();
        server.listen();
        const received = deferred();
        server.onRequest("ahp.createEndpoint", () => ({}));
        server.onRequest("ahp.openConnection", () => ({}));
        server.onRequest("ahp.receive", () => {
            received.resolve({});
            return new Promise(() => {});
        });
        server.onRequest("ping", () => new Promise(() => {}));
        const endpoint = await client.createAhpEndpoint();
        const close = vi.fn();
        const connection = endpoint.acceptConnection({ send: async () => {}, close });
        const receive = expect(connection.receive("opaque")).rejects.toThrow(
            "SDK RPC connection closed"
        );
        const ping = expect(client.ping()).rejects.toThrow();
        server.onRequest("ahp.createEndpoint", () => new Promise(() => {}));
        const creation = expect(client.createAhpEndpoint()).rejects.toThrow();
        await received.promise;
        inbound.end();
        await Promise.all([receive, ping, creation]);
        await expect(connection.closed).rejects.toThrow("SDK RPC connection closed");
        expect(close).toHaveBeenCalledOnce();
        expect((client as any).ahpEndpoints.size).toBe(0);
        await expect(client.ping()).rejects.toThrow();
        await client.forceStop();
        server.dispose();
        inbound.destroy();
        outbound.destroy();
    });
});
