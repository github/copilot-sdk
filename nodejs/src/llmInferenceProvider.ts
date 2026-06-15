/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type {
    LlmInferenceHandler,
    LlmInferenceHeaders,
    LlmInferenceHttpRequestChunkRequest,
    LlmInferenceHttpRequestChunkResult,
    LlmInferenceHttpRequestStartRequest,
    LlmInferenceHttpRequestStartResult,
} from "./generated/rpc.js";
import type { createServerRpc } from "./generated/rpc.js";

type ServerRpc = ReturnType<typeof createServerRpc>;

/**
 * An outbound model-layer HTTP request the runtime is asking the SDK
 * consumer to handle on its behalf.
 *
 * This is a low-level shape: URL / method / headers verbatim, body bytes
 * delivered as an async iterable, response delivered through the
 * {@link LlmInferenceResponseSink}. The runtime does not classify the
 * request (no provider type, endpoint kind, wire API). Consumers that
 * need that information derive it themselves from the URL / headers.
 */
export interface LlmInferenceRequest {
    /** Opaque runtime-minted id, stable across the request lifecycle. */
    requestId: string;
    /**
     * Id of the runtime session that triggered this request, when one is
     * in scope. Absent for out-of-session requests (e.g. startup model
     * catalog).
     */
    sessionId?: string;
    /** HTTP method (`GET`, `POST`, ...). */
    method: string;
    /** Absolute URL. */
    url: string;
    /** HTTP request headers, multi-valued. */
    headers: LlmInferenceHeaders;
    /**
     * Request body bytes, yielded as they arrive from the runtime.
     * Always iterable; an empty body yields zero chunks before completing.
     */
    requestBody: AsyncIterable<Uint8Array>;
    /**
     * Sink the consumer writes the upstream response into. Call
     * {@link LlmInferenceResponseSink.start} exactly once before writing
     * body chunks, then one or more {@link LlmInferenceResponseSink.write}
     * calls, and finish with {@link LlmInferenceResponseSink.end} or
     * {@link LlmInferenceResponseSink.error}.
     */
    responseBody: LlmInferenceResponseSink;
}

/** Response head passed to {@link LlmInferenceResponseSink.start}. */
export interface LlmInferenceResponseInit {
    status: number;
    statusText?: string;
    headers?: LlmInferenceHeaders;
}

/**
 * Sink the consumer writes the upstream response into. The state machine
 * is strict: `start` once → 0..N `write` → exactly one of `end` or
 * `error`. Calling out of order throws.
 */
export interface LlmInferenceResponseSink {
    /** Send the response head (status + headers) back to the runtime. */
    start(init: LlmInferenceResponseInit): Promise<void>;
    /**
     * Send a body chunk. `string` is encoded as UTF-8; `Uint8Array` is sent
     * as binary (base64 on the wire).
     */
    write(data: string | Uint8Array): Promise<void>;
    /** Mark end-of-stream cleanly. */
    end(): Promise<void>;
    /** Mark end-of-stream with a transport-level failure. */
    error(error: { message: string; code?: string }): Promise<void>;
}

/**
 * Interface for an LLM inference provider. The SDK consumer implements
 * `onLlmRequest`. The same callback handles both buffered and streaming
 * responses — the consumer just calls `responseBody.write` zero or more
 * times before `end`.
 *
 * Use {@link createLlmInferenceAdapter} to convert an
 * {@link LlmInferenceProvider} into the {@link LlmInferenceHandler} the
 * SDK's RPC layer registers.
 */
export interface LlmInferenceProvider {
    /**
     * Called by the runtime once per outbound LLM HTTP request the
     * consumer has opted to handle. The consumer is responsible for
     * eventually calling either `responseBody.end()` or
     * `responseBody.error(...)`; failing to do so leaks runtime state.
     * Throwing surfaces a transport-level failure to the runtime
     * (equivalent to `responseBody.error({ message: err.message })`
     * provided `start` has not yet been called).
     */
    onLlmRequest(request: LlmInferenceRequest): Promise<void> | void;
}

interface BodyQueueItem {
    chunk?: Uint8Array;
    end?: boolean;
    cancel?: { reason?: string };
}

interface BodyQueue {
    push(item: BodyQueueItem): void;
    iterable: AsyncIterable<Uint8Array>;
}

function makeBodyQueue(): BodyQueue {
    const buffer: BodyQueueItem[] = [];
    let waker: (() => void) | null = null;
    let done = false;
    const wake = (): void => {
        const w = waker;
        waker = null;
        w?.();
    };
    return {
        push(item: BodyQueueItem): void {
            buffer.push(item);
            wake();
        },
        iterable: {
            [Symbol.asyncIterator](): AsyncIterator<Uint8Array> {
                return {
                    async next(): Promise<IteratorResult<Uint8Array>> {
                        if (done) {
                            return { value: undefined, done: true };
                        }
                        while (buffer.length === 0) {
                            await new Promise<void>((resolve) => {
                                waker = resolve;
                            });
                        }
                        const item = buffer.shift()!;
                        if (item.cancel) {
                            done = true;
                            const reason = item.cancel.reason
                                ? `Request cancelled by runtime: ${item.cancel.reason}`
                                : "Request cancelled by runtime";
                            throw new Error(reason);
                        }
                        if (item.end) {
                            done = true;
                            return { value: undefined, done: true };
                        }
                        return { value: item.chunk ?? new Uint8Array(), done: false };
                    },
                };
            },
        },
    };
}

function decodeChunkData(data: string, binary: boolean): Uint8Array {
    if (binary) {
        return new Uint8Array(Buffer.from(data, "base64"));
    }
    return new TextEncoder().encode(data);
}

interface PendingState {
    queue: BodyQueue;
    started: boolean;
    finished: boolean;
}

/**
 * Adapt an {@link LlmInferenceProvider} into the generated
 * {@link LlmInferenceHandler} shape consumed by the SDK's RPC dispatcher.
 *
 * Maintains a per-`requestId` state table: each `httpRequestStart`
 * allocates a body queue + response sink and fires
 * `provider.onLlmRequest` in the background. Subsequent `httpRequestChunk`
 * frames are routed into the queue. The sink translates `start` /
 * `write` / `end` / `error` calls into outbound
 * `serverRpc.llmInference.httpResponseStart` / `httpResponseChunk` calls.
 *
 * The handler returns from `httpRequestStart` immediately (synchronously
 * after registering state) so the runtime's RPC reply is not gated on the
 * consumer's I/O. The actual provider work runs asynchronously.
 */
export function createLlmInferenceAdapter(
    provider: LlmInferenceProvider,
    getServerRpc: () => ServerRpc | undefined,
): LlmInferenceHandler {
    const pending = new Map<string, PendingState>();

    function makeSink(requestId: string, state: PendingState): LlmInferenceResponseSink {
        const rpc = (): ServerRpc => {
            const r = getServerRpc();
            if (!r) {
                throw new Error("LLM inference response sink used after RPC connection closed.");
            }
            return r;
        };
        return {
            async start(init: LlmInferenceResponseInit): Promise<void> {
                if (state.started) {
                    throw new Error("LLM inference response sink.start() called twice.");
                }
                if (state.finished) {
                    throw new Error("LLM inference response sink already finished.");
                }
                state.started = true;
                await rpc().llmInference.httpResponseStart({
                    requestId,
                    status: init.status,
                    statusText: init.statusText,
                    headers: init.headers ?? {},
                });
            },
            async write(data: string | Uint8Array): Promise<void> {
                if (!state.started) {
                    throw new Error("LLM inference response sink.write() called before start().");
                }
                if (state.finished) {
                    throw new Error("LLM inference response sink.write() called after end()/error().");
                }
                const isString = typeof data === "string";
                await rpc().llmInference.httpResponseChunk({
                    requestId,
                    data: isString ? data : Buffer.from(data).toString("base64"),
                    binary: !isString,
                    end: false,
                });
            },
            async end(): Promise<void> {
                if (state.finished) {
                    return;
                }
                state.finished = true;
                pending.delete(requestId);
                await rpc().llmInference.httpResponseChunk({
                    requestId,
                    data: "",
                    end: true,
                });
            },
            async error(err: { message: string; code?: string }): Promise<void> {
                if (state.finished) {
                    return;
                }
                state.finished = true;
                pending.delete(requestId);
                await rpc().llmInference.httpResponseChunk({
                    requestId,
                    data: "",
                    end: true,
                    error: { message: err.message, code: err.code },
                });
            },
        };
    }

    async function failViaSink(
        sink: LlmInferenceResponseSink,
        state: PendingState,
        message: string,
    ): Promise<void> {
        if (state.finished) {
            return;
        }
        try {
            if (!state.started) {
                await sink.start({ status: 502, headers: {} });
            }
            await sink.error({ message });
        } catch {
            // Best-effort — the connection may already be dead.
        }
    }

    return {
        async httpRequestStart(
            params: LlmInferenceHttpRequestStartRequest,
        ): Promise<LlmInferenceHttpRequestStartResult> {
            const state: PendingState = {
                queue: makeBodyQueue(),
                started: false,
                finished: false,
            };
            pending.set(params.requestId, state);
            const sink = makeSink(params.requestId, state);
            const request: LlmInferenceRequest = {
                requestId: params.requestId,
                sessionId: params.sessionId,
                method: params.method,
                url: params.url,
                headers: params.headers,
                requestBody: state.queue.iterable,
                responseBody: sink,
            };
            void (async () => {
                try {
                    await provider.onLlmRequest(request);
                    if (!state.finished) {
                        await failViaSink(
                            sink,
                            state,
                            "LLM inference provider returned without finalising the response (call responseBody.end() or .error()).",
                        );
                    }
                } catch (err) {
                    const message = err instanceof Error ? err.message : String(err);
                    await failViaSink(sink, state, message);
                }
            })();
            return {};
        },
        async httpRequestChunk(
            params: LlmInferenceHttpRequestChunkRequest,
        ): Promise<LlmInferenceHttpRequestChunkResult> {
            const state = pending.get(params.requestId);
            if (!state) {
                return {};
            }
            if (params.cancel) {
                state.queue.push({ cancel: { reason: params.cancelReason } });
                return {};
            }
            if (params.data && params.data.length > 0) {
                state.queue.push({ chunk: decodeChunkData(params.data, !!params.binary) });
            }
            if (params.end) {
                state.queue.push({ end: true });
            }
            return {};
        },
    };
}
