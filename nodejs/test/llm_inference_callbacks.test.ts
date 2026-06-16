/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import {
    createLlmInferenceAdapter,
    LlmRequestHandler,
    type LlmInferenceProvider,
    type LlmInferenceRequest,
    type LlmInferenceResponseInit,
    type LlmInferenceResponseSink,
    type LlmWebSocketUpstream,
} from "../src/index.js";

/**
 * Minimal fake of the server RPC surface the adapter uses to send response
 * frames back to the runtime. Records every frame and lets the test decide
 * what `accepted` value the runtime returns.
 */
function makeFakeServerRpc(accepted: { start?: boolean; chunk?: boolean } = {}): {
    rpc: () => ReturnType<typeof createFakeRpc>;
    starts: LlmInferenceResponseInit[];
    chunks: { data: string; binary?: boolean; end?: boolean; error?: unknown }[];
} {
    const starts: LlmInferenceResponseInit[] = [];
    const chunks: { data: string; binary?: boolean; end?: boolean; error?: unknown }[] = [];
    function createFakeRpc() {
        return {
            llmInference: {
                async httpResponseStart(p: {
                    status: number;
                    statusText?: string;
                    headers: Record<string, string[]>;
                }) {
                    starts.push({ status: p.status, statusText: p.statusText, headers: p.headers });
                    return { accepted: accepted.start ?? true };
                },
                async httpResponseChunk(p: {
                    data: string;
                    binary?: boolean;
                    end?: boolean;
                    error?: unknown;
                }) {
                    chunks.push({ data: p.data, binary: p.binary, end: p.end, error: p.error });
                    return { accepted: accepted.chunk ?? true };
                },
            },
        };
    }
    const single = createFakeRpc();
    return { rpc: () => single, starts, chunks };
}

describe("createLlmInferenceAdapter", () => {
    it("stages body chunks that arrive before their start frame and replays them in order", async () => {
        const received: string[] = [];
        let resolveDone: () => void;
        const done = new Promise<void>((r) => {
            resolveDone = r;
        });
        const provider: LlmInferenceProvider = {
            async onLlmRequest(req: LlmInferenceRequest) {
                const decoder = new TextDecoder();
                for await (const chunk of req.requestBody) {
                    received.push(decoder.decode(chunk));
                }
                await req.responseBody.start({ status: 200, headers: {} });
                await req.responseBody.end();
                resolveDone();
            },
        };
        const fake = makeFakeServerRpc();
        const handler = createLlmInferenceAdapter(provider, () => fake.rpc() as never);

        // Chunks arrive BEFORE the start frame (simulating a reordering the
        // runtime should never actually produce). They must be staged and
        // delivered once the start frame registers the request.
        await handler.httpRequestChunk({
            requestId: "r1",
            data: "hello ",
            binary: false,
            end: false,
        });
        await handler.httpRequestChunk({
            requestId: "r1",
            data: "world",
            binary: false,
            end: false,
        });
        await handler.httpRequestChunk({ requestId: "r1", data: "", end: true });

        await handler.httpRequestStart({
            requestId: "r1",
            method: "POST",
            url: "https://example.test/v1/chat",
            headers: {},
            transport: "http",
        });

        await done;
        expect(received.join("")).toBe("hello world");
    });

    it("aborts the provider when the runtime rejects a response frame (accepted=false)", async () => {
        let aborted = false;
        let writeThrew = false;
        let finished: () => void;
        const settled = new Promise<void>((r) => {
            finished = r;
        });
        const provider: LlmInferenceProvider = {
            async onLlmRequest(req: LlmInferenceRequest) {
                req.signal.addEventListener("abort", () => {
                    aborted = true;
                });
                for await (const _ of req.requestBody) {
                    // drain
                }
                await req.responseBody.start({ status: 200, headers: {} });
                try {
                    await req.responseBody.write("rejected-chunk");
                } catch {
                    writeThrew = true;
                }
                finished();
            },
        };
        const fake = makeFakeServerRpc({ start: true, chunk: false });
        const handler = createLlmInferenceAdapter(provider, () => fake.rpc() as never);

        await handler.httpRequestStart({
            requestId: "r2",
            method: "POST",
            url: "https://example.test/v1/chat",
            headers: {},
            transport: "http",
        });
        await handler.httpRequestChunk({ requestId: "r2", data: "", end: true });

        await settled;
        expect(writeThrew).toBe(true);
        expect(aborted).toBe(true);
    });
});

/**
 * Controllable fake of {@link LlmWebSocketUpstream}. Auto-fires `open` once a
 * listener is registered (mirroring an already-connected socket); the test
 * drives messages, close, and error explicitly.
 */
class FakeUpstream implements LlmWebSocketUpstream {
    sent: (string | Uint8Array)[] = [];
    closed = false;
    #open: (() => void) | null = null;
    #message: ((data: string | Uint8Array) => void) | null = null;
    #close: ((code: number, reason: string) => void) | null = null;
    #error: ((error: Error) => void) | null = null;

    send(data: string | Uint8Array): void {
        this.sent.push(data);
    }
    close(): void {
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.#close?.(1000, "");
    }
    onOpen(handler: () => void): void {
        this.#open = handler;
        queueMicrotask(() => this.#open?.());
    }
    onMessage(handler: (data: string | Uint8Array) => void): void {
        this.#message = handler;
    }
    onClose(handler: (code: number, reason: string) => void): void {
        this.#close = handler;
    }
    onError(handler: (error: Error) => void): void {
        this.#error = handler;
    }

    emitMessage(data: string | Uint8Array): void {
        this.#message?.(data);
    }
    emitError(error: Error): void {
        this.#error?.(error);
    }
}

interface RecordingSink extends LlmInferenceResponseSink {
    starts: LlmInferenceResponseInit[];
    writes: (string | Uint8Array)[];
    ended: boolean;
    errored?: { message: string; code?: string };
}

function makeRecordingSink(): RecordingSink {
    const sink: RecordingSink = {
        starts: [],
        writes: [],
        ended: false,
        async start(init) {
            sink.starts.push(init);
        },
        async write(data) {
            sink.writes.push(data);
        },
        async end() {
            sink.ended = true;
        },
        async error(err) {
            sink.errored = err;
        },
    };
    return sink;
}

/** Async-iterable request body that yields nothing until the test releases it. */
function gatedRequestBody(): { body: AsyncIterable<Uint8Array>; release: () => void } {
    let release!: () => void;
    const gate = new Promise<void>((r) => {
        release = r;
    });
    return {
        release,
        body: {
            async *[Symbol.asyncIterator]() {
                await gate;
            },
        },
    };
}

describe("LlmRequestHandler WebSocket dispatch", () => {
    it("finalises the response when the upstream closes while the request stream is still open", async () => {
        const upstream = new FakeUpstream();
        class Handler extends LlmRequestHandler {
            protected override forwardWebSocket(): LlmWebSocketUpstream {
                return upstream;
            }
        }
        const handler = new Handler();
        const sink = makeRecordingSink();
        const gated = gatedRequestBody();
        const abort = new AbortController();
        const req: LlmInferenceRequest = {
            requestId: "ws1",
            method: "GET",
            url: "wss://example.test/responses",
            headers: {},
            transport: "websocket",
            requestBody: gated.body,
            signal: abort.signal,
            responseBody: sink,
        };

        const turn = handler.onLlmRequest(req);

        // Let the handler register its listeners and ack the upgrade, then
        // deliver an upstream message and close the socket — all while the
        // request body is still parked (no runtime → upstream frames yet).
        await new Promise((r) => setTimeout(r, 10));
        upstream.emitMessage("server-event-1");
        upstream.close();

        // The turn must resolve (not hang) because the upstream terminated.
        await turn;

        expect(sink.starts).toEqual([{ status: 101, headers: {} }]);
        expect(sink.writes).toContain("server-event-1");
        expect(sink.ended).toBe(true);

        gated.release();
    });

    it("surfaces an upstream error as a thrown failure", async () => {
        const upstream = new FakeUpstream();
        class Handler extends LlmRequestHandler {
            protected override forwardWebSocket(): LlmWebSocketUpstream {
                return upstream;
            }
        }
        const handler = new Handler();
        const sink = makeRecordingSink();
        const gated = gatedRequestBody();
        const abort = new AbortController();
        const req: LlmInferenceRequest = {
            requestId: "ws2",
            method: "GET",
            url: "wss://example.test/responses",
            headers: {},
            transport: "websocket",
            requestBody: gated.body,
            signal: abort.signal,
            responseBody: sink,
        };

        const turn = handler.onLlmRequest(req);
        await new Promise((r) => setTimeout(r, 10));
        upstream.emitError(new Error("upstream exploded"));

        await expect(turn).rejects.toThrow("upstream exploded");
        expect(sink.ended).toBe(false);

        gated.release();
    });
});
