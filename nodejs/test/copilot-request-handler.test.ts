/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import {
    CopilotRequestHandler,
    type CopilotRequestContext,
    createCopilotRequestAdapter,
} from "../src/copilotRequestHandler.js";
import type {
    LlmInferenceHandler,
    LlmInferenceHttpResponseChunkRequest,
    LlmInferenceHttpResponseStartRequest,
} from "../src/generated/rpc.js";

const READ_AHEAD_BYTES = 32 * 1024;

interface Deferred<T> {
    promise: Promise<T>;
    resolve: (value: T) => void;
    reject: (reason?: unknown) => void;
}

function deferred<T>(): Deferred<T> {
    let resolve!: (value: T) => void;
    let reject!: (reason?: unknown) => void;
    const promise = new Promise<T>((resolvePromise, rejectPromise) => {
        resolve = resolvePromise;
        reject = rejectPromise;
    });
    return { promise, resolve, reject };
}

async function waitFor(predicate: () => boolean, message: string): Promise<void> {
    const deadline = Date.now() + 5_000;
    while (!predicate()) {
        if (Date.now() >= deadline) {
            throw new Error(message);
        }
        await new Promise<void>((resolve) => setTimeout(resolve, 1));
    }
}

async function withTimeout<T>(promise: Promise<T>, message: string): Promise<T> {
    return Promise.race([
        promise,
        new Promise<never>((_, reject) => setTimeout(() => reject(new Error(message)), 5_000)),
    ]);
}

class StaticResponseHandler extends CopilotRequestHandler {
    constructor(private readonly response: Response) {
        super();
    }

    protected override sendRequest(
        _request: Request,
        _ctx: CopilotRequestContext
    ): Promise<Response> {
        return Promise.resolve(this.response);
    }
}

class ProtocolPeer {
    readonly starts: LlmInferenceHttpResponseStartRequest[] = [];
    readonly chunks: LlmInferenceHttpResponseChunkRequest[] = [];
    readonly dataAcks: Array<Deferred<{ accepted: boolean }>> = [];
    readonly terminal = deferred<LlmInferenceHttpResponseChunkRequest>();
    outstandingDataRpcs = 0;
    maxOutstandingDataRpcs = 0;
    onStart: (() => void) | undefined;

    readonly rpc = {
        llmInference: {
            httpResponseStart: async (
                params: LlmInferenceHttpResponseStartRequest
            ): Promise<{ accepted: boolean }> => {
                this.starts.push(params);
                this.onStart?.();
                return { accepted: true };
            },
            httpResponseChunk: (
                params: LlmInferenceHttpResponseChunkRequest
            ): Promise<{ accepted: boolean }> => {
                this.chunks.push(params);
                if (params.end) {
                    this.terminal.resolve(params);
                    return Promise.resolve({ accepted: true });
                }

                this.outstandingDataRpcs++;
                this.maxOutstandingDataRpcs = Math.max(
                    this.maxOutstandingDataRpcs,
                    this.outstandingDataRpcs
                );
                const ack = deferred<{ accepted: boolean }>();
                this.dataAcks.push(ack);
                return ack.promise.finally(() => {
                    this.outstandingDataRpcs--;
                });
            },
        },
    };
}

function createAdapter(
    handler: CopilotRequestHandler,
    getRpc: () => ProtocolPeer["rpc"] | undefined
): LlmInferenceHandler {
    return createCopilotRequestAdapter(handler, () => getRpc() as never);
}

async function startGet(adapter: LlmInferenceHandler, requestId: string): Promise<void> {
    await adapter.httpRequestStart({
        requestId,
        method: "GET",
        url: "https://example.test/inference",
        headers: {},
    });
    await adapter.httpRequestChunk({ requestId, data: "", end: true });
}

function frameBytes(frame: LlmInferenceHttpResponseChunkRequest): Uint8Array {
    return frame.binary
        ? new Uint8Array(Buffer.from(frame.data, "base64"))
        : new TextEncoder().encode(frame.data);
}

describe("CopilotRequestHandler HTTP response protocol", () => {
    it("reads ahead, coalesces tiny source chunks, and keeps one data RPC outstanding", async () => {
        const totalBytes = 40_000;
        let sourceReads = 0;
        const expected = Uint8Array.from({ length: totalBytes }, (_, index) => index % 251);
        const body = new ReadableStream<Uint8Array>(
            {
                pull(controller) {
                    if (sourceReads === totalBytes) {
                        controller.close();
                        return;
                    }
                    controller.enqueue(expected.subarray(sourceReads, sourceReads + 1));
                    sourceReads++;
                },
            },
            { highWaterMark: 0 }
        );
        const peer = new ProtocolPeer();
        const adapter = createAdapter(
            new StaticResponseHandler(new Response(body, { status: 200 })),
            () => peer.rpc
        );

        await startGet(adapter, "coalesce");
        await waitFor(() => peer.dataAcks.length === 1, "first response chunk was not sent");

        const firstLength = frameBytes(peer.chunks[0]).byteLength;
        expect(firstLength).toBeGreaterThan(0);
        expect(firstLength).toBeLessThan(READ_AHEAD_BYTES);
        await waitFor(
            () => sourceReads === firstLength + READ_AHEAD_BYTES,
            "source did not read ahead while the first ACK was withheld"
        );
        await new Promise<void>((resolve) => setTimeout(resolve, 20));

        expect(sourceReads - firstLength).toBe(READ_AHEAD_BYTES);
        expect(peer.dataAcks).toHaveLength(1);
        expect(peer.outstandingDataRpcs).toBe(1);
        expect(peer.maxOutstandingDataRpcs).toBe(1);

        peer.dataAcks[0].resolve({ accepted: true });
        await waitFor(() => peer.dataAcks.length === 2, "coalesced response chunk was not sent");
        expect(frameBytes(peer.chunks[1])).toHaveLength(READ_AHEAD_BYTES);
        expect(peer.outstandingDataRpcs).toBe(1);
        await waitFor(
            () => sourceReads === totalBytes,
            "source did not finish reading behind the second ACK"
        );

        peer.dataAcks[1].resolve({ accepted: true });
        await waitFor(() => peer.dataAcks.length === 3, "final partial chunk was not sent");
        peer.dataAcks[2].resolve({ accepted: true });
        await withTimeout(peer.terminal.promise, "terminal response chunk was not sent");

        const dataFrames = peer.chunks.filter((chunk) => !chunk.end);
        expect(dataFrames).toHaveLength(3);
        expect(peer.maxOutstandingDataRpcs).toBe(1);
        expect(Buffer.concat(dataFrames.map((frame) => Buffer.from(frameBytes(frame))))).toEqual(
            Buffer.from(expected)
        );
    });

    it("cancels the source promptly when the runtime cancels with an ACK withheld", async () => {
        const sourceCancelled = deferred<void>();
        const body = new ReadableStream<Uint8Array>(
            {
                pull(controller) {
                    controller.enqueue(Uint8Array.of(1));
                },
                cancel() {
                    sourceCancelled.resolve();
                },
            },
            { highWaterMark: 0 }
        );
        const peer = new ProtocolPeer();
        const adapter = createAdapter(
            new StaticResponseHandler(new Response(body, { status: 200 })),
            () => peer.rpc
        );

        await startGet(adapter, "runtime-cancel");
        await waitFor(() => peer.dataAcks.length === 1, "response chunk was not sent");
        await adapter.httpRequestChunk({
            requestId: "runtime-cancel",
            data: "",
            cancel: true,
            cancelReason: "turn aborted",
        });

        await withTimeout(
            sourceCancelled.promise,
            "runtime cancellation did not cancel the source"
        );
        expect(peer.dataAcks).toHaveLength(1);
        peer.dataAcks[0].reject(new Error("runtime rejected cancelled request"));
        await withTimeout(peer.terminal.promise, "cancelled response did not settle");
    });

    it("cancels the source when a response chunk RPC rejects", async () => {
        const sourceCancelled = deferred<void>();
        const body = new ReadableStream<Uint8Array>(
            {
                pull(controller) {
                    controller.enqueue(Uint8Array.of(1));
                },
                cancel() {
                    sourceCancelled.resolve();
                },
            },
            { highWaterMark: 0 }
        );
        const peer = new ProtocolPeer();
        const adapter = createAdapter(
            new StaticResponseHandler(new Response(body, { status: 200 })),
            () => peer.rpc
        );

        await startGet(adapter, "rpc-rejection");
        await waitFor(() => peer.dataAcks.length === 1, "response chunk was not sent");
        peer.dataAcks[0].reject(new Error("response RPC rejected"));

        await withTimeout(sourceCancelled.promise, "RPC rejection did not cancel the source");
        const terminal = await withTimeout(
            peer.terminal.promise,
            "RPC rejection did not report a terminal error"
        );
        expect(terminal.error?.message).toBe("response RPC rejected");
    });

    it("cancels the source when the RPC connection disappears", async () => {
        const sourceCancelled = deferred<void>();
        const body = new ReadableStream<Uint8Array>(
            {
                pull(controller) {
                    controller.enqueue(Uint8Array.of(1));
                },
                cancel() {
                    sourceCancelled.resolve();
                },
            },
            { highWaterMark: 0 }
        );
        const peer = new ProtocolPeer();
        let connected = true;
        peer.onStart = () => {
            connected = false;
        };
        const adapter = createAdapter(
            new StaticResponseHandler(new Response(body, { status: 200 })),
            () => (connected ? peer.rpc : undefined)
        );

        await startGet(adapter, "connection-loss");
        await withTimeout(sourceCancelled.promise, "connection loss did not cancel the source");
        expect(peer.dataAcks).toHaveLength(0);
    });

    it("cancels an idle source when the RPC connection closes", async () => {
        const readStarted = deferred<void>();
        const sourceCancelled = deferred<void>();
        const body = new ReadableStream<Uint8Array>(
            {
                async pull() {
                    readStarted.resolve();
                    await new Promise<void>(() => {});
                },
                cancel() {
                    sourceCancelled.resolve();
                },
            },
            { highWaterMark: 0 }
        );
        const peer = new ProtocolPeer();
        const adapter = createCopilotRequestAdapter(
            new StaticResponseHandler(new Response(body, { status: 200 })),
            () => peer.rpc as never
        );

        await startGet(adapter, "idle-connection-loss");
        await withTimeout(readStarted.promise, "source read did not start");
        adapter.cancelPending();

        await withTimeout(sourceCancelled.promise, "connection loss did not cancel the source");
        expect(peer.dataAcks).toHaveLength(0);
        const terminal = await withTimeout(
            peer.terminal.promise,
            "connection loss did not settle the response"
        );
        expect(terminal.error?.code).toBe("cancelled");
    });

    it("flushes buffered bytes before reporting an upstream error", async () => {
        const releaseSecondChunk = deferred<void>();
        let pull = 0;
        const body = new ReadableStream<Uint8Array>(
            {
                async pull(controller) {
                    if (pull === 0) {
                        controller.enqueue(Uint8Array.of(1, 2));
                    } else if (pull === 1) {
                        await releaseSecondChunk.promise;
                        controller.enqueue(Uint8Array.of(3, 4));
                    } else {
                        controller.error(new Error("upstream failed"));
                    }
                    pull++;
                },
            },
            { highWaterMark: 0 }
        );
        const peer = new ProtocolPeer();
        const adapter = createAdapter(
            new StaticResponseHandler(new Response(body, { status: 200 })),
            () => peer.rpc
        );

        await startGet(adapter, "upstream-error");
        await waitFor(() => peer.dataAcks.length === 1, "first response chunk was not sent");
        expect(frameBytes(peer.chunks[0])).toEqual(Uint8Array.of(1, 2));

        releaseSecondChunk.resolve();
        await waitFor(() => pull === 3, "upstream error was not observed during read-ahead");
        peer.dataAcks[0].resolve({ accepted: true });
        await waitFor(() => peer.dataAcks.length === 2, "buffered response chunk was not sent");
        expect(frameBytes(peer.chunks[1])).toEqual(Uint8Array.of(3, 4));

        peer.dataAcks[1].resolve({ accepted: true });
        const terminal = await withTimeout(
            peer.terminal.promise,
            "upstream error was not reported"
        );
        expect(peer.chunks).toHaveLength(3);
        expect(terminal.end).toBe(true);
        expect(terminal.error?.message).toBe("upstream failed");
        expect(peer.maxOutstandingDataRpcs).toBe(1);
    });
});
