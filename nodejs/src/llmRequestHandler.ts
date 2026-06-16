/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { LlmInferenceHeaders } from "./generated/rpc.js";
import type { LlmInferenceProvider, LlmInferenceRequest } from "./llmInferenceProvider.js";

/**
 * Per-request context handed to every {@link LlmRequestHandler} hook.
 * Mirrors the subset of {@link LlmInferenceRequest} fields that are
 * stable across the request lifetime; lets overrides observe routing /
 * cancellation without re-plumbing the underlying request.
 *
 * @experimental
 */
export interface LlmRequestContext {
    /** Opaque runtime-minted id, stable across the request lifecycle. */
    readonly requestId: string;
    /** Runtime session id that triggered the request, if any. */
    readonly sessionId?: string;
    /**
     * Transport the runtime would otherwise use. Hooks that branch on
     * transport (e.g. add a header on HTTP only) can read this field.
     */
    readonly transport: "http" | "websocket";
    /**
     * Aborts when the runtime cancels this in-flight request. Subclasses
     * that issue their own I/O should pass this through (e.g. `fetch`'s
     * `signal` option) so the upstream call is torn down too.
     */
    readonly signal: AbortSignal;
}

/**
 * A duplex upstream WebSocket-like channel returned by
 * {@link LlmRequestHandler.forwardWebSocket}. Modelled on the WHATWG
 * `WebSocket` interface (callbacks instead of events) so the default
 * implementation can wrap the global `WebSocket` directly, but kept
 * minimal so overrides can wrap any client (e.g. the `ws` package, when
 * custom upgrade headers are required).
 *
 * Contract:
 * - {@link onOpen} fires exactly once before any {@link send} succeeds
 *   and before {@link onMessage} fires.
 * - {@link onMessage} may fire zero or more times. `data` is a
 *   `string` for text frames and `Uint8Array` for binary frames.
 * - Exactly one of {@link onClose} or {@link onError} fires terminally,
 *   including when the terminal close is initiated locally via
 *   {@link close}. After it fires {@link send} is a no-op.
 *
 * @experimental
 */
export interface LlmWebSocketUpstream {
    /** Send an outbound frame. Text → `string`, binary → `Uint8Array`. */
    send(data: string | Uint8Array): void;
    /**
     * Close the channel. This still drives the terminal {@link onClose}
     * (or {@link onError}) callback — the wrapper does not suppress it —
     * so callers awaiting that signal observe the local close too.
     */
    close(code?: number, reason?: string): void;
    /** Registers the open-handshake-complete listener. Called once. */
    onOpen(handler: () => void): void;
    /** Registers the inbound-message listener. Called 0..N times. */
    onMessage(handler: (data: string | Uint8Array) => void): void;
    /** Registers the terminal close listener. Called at most once. */
    onClose(handler: (code: number, reason: string) => void): void;
    /** Registers the terminal error listener. Called at most once. */
    onError(handler: (error: Error) => void): void;
}

/**
 * Base class for SDK consumers who want to observe or mutate the LLM
 * inference requests the runtime issues. Implements
 * {@link LlmInferenceProvider}, so an instance can be returned directly
 * from {@link LlmInferenceConfig.createLlmInferenceProvider}.
 *
 * Default behaviour is a transparent pass-through: each request is
 * forwarded to its original URL via the WHATWG `fetch` global (HTTP)
 * or the WHATWG `WebSocket` global (WebSocket), and the upstream
 * response is streamed back to the runtime unchanged. Consumers
 * subclass and override one or more virtual methods to interpose:
 *
 * - {@link transformRequest} — mutate the outbound HTTP request, or
 *   short-circuit it with a `Response` (e.g. cache hit / canned reply).
 * - {@link forward} — replace the upstream HTTP call entirely (e.g. to
 *   call a non-`fetch` client, or to add per-call retry/observability).
 * - {@link transformResponse} — mutate the upstream HTTP response on
 *   its way back to the runtime.
 * - {@link forwardWebSocket} — replace the upstream WebSocket open
 *   (e.g. to set custom upgrade headers via the `ws` package).
 * - {@link transformRequestMessage} / {@link transformResponseMessage} —
 *   observe or mutate WebSocket messages in either direction.
 *
 * The same subclass handles both transports — {@link onLlmRequest}
 * dispatches on {@link LlmInferenceRequest.transport}.
 *
 * @experimental
 */
export class LlmRequestHandler implements LlmInferenceProvider {
    async onLlmRequest(req: LlmInferenceRequest): Promise<void> {
        const ctx: LlmRequestContext = {
            requestId: req.requestId,
            sessionId: req.sessionId,
            transport: req.transport,
            signal: req.signal,
        };
        if (req.transport === "websocket") {
            await this.#handleWebSocket(req, ctx);
        } else {
            await this.#handleHttp(req, ctx);
        }
    }

    // ─── HTTP virtual hooks ────────────────────────────────────────────

    /**
     * Mutate the outbound HTTP request, or short-circuit it by returning
     * a {@link Response} (in which case {@link forward} is skipped).
     * Default: pass through unchanged.
     */
    protected transformRequest(
        request: Request,
        _ctx: LlmRequestContext
    ): Request | Response | Promise<Request | Response> {
        return request;
    }

    /**
     * Issue the upstream HTTP call. Default: WHATWG `fetch` with the
     * request's `signal` wired to {@link LlmRequestContext.signal} so
     * cancellation propagates upstream.
     */
    protected forward(request: Request, ctx: LlmRequestContext): Promise<Response> {
        return fetch(request, { signal: ctx.signal });
    }

    /**
     * Mutate the upstream HTTP response before it streams back to the
     * runtime. Default: pass through unchanged.
     */
    protected transformResponse(
        response: Response,
        _ctx: LlmRequestContext
    ): Response | Promise<Response> {
        return response;
    }

    // ─── WebSocket virtual hooks ───────────────────────────────────────

    /**
     * Open the upstream WebSocket. Default: WHATWG `WebSocket` global,
     * which does **not** support custom upgrade headers in Node — if
     * your upstream needs `Authorization` or similar on the handshake,
     * override this to use a client that does (e.g. the `ws` package).
     */
    protected forwardWebSocket(
        url: string,
        _headers: LlmInferenceHeaders,
        _ctx: LlmRequestContext
    ): LlmWebSocketUpstream | Promise<LlmWebSocketUpstream> {
        return wrapGlobalWebSocket(new WebSocket(url));
    }

    /**
     * Observe or mutate an outbound (request) WebSocket message — i.e.
     * one the runtime is sending to the upstream. Return `null` to drop
     * the message. Default: pass through unchanged.
     */
    protected transformRequestMessage(
        data: string | Uint8Array,
        _ctx: LlmRequestContext
    ): string | Uint8Array | null | Promise<string | Uint8Array | null> {
        return data;
    }

    /**
     * Observe or mutate an inbound (response) WebSocket message — i.e.
     * one the upstream is sending back to the runtime. Return `null` to
     * drop the message. Default: pass through unchanged.
     */
    protected transformResponseMessage(
        data: string | Uint8Array,
        _ctx: LlmRequestContext
    ): string | Uint8Array | null | Promise<string | Uint8Array | null> {
        return data;
    }

    // ─── HTTP dispatch ─────────────────────────────────────────────────

    async #handleHttp(req: LlmInferenceRequest, ctx: LlmRequestContext): Promise<void> {
        const initialRequest = await buildFetchRequest(req);
        const transformed = await this.transformRequest(initialRequest, ctx);
        const response =
            transformed instanceof Response ? transformed : await this.forward(transformed, ctx);
        const finalResponse = await this.transformResponse(response, ctx);
        await streamResponseToSink(finalResponse, req);
    }

    // ─── WebSocket dispatch ────────────────────────────────────────────

    async #handleWebSocket(req: LlmInferenceRequest, ctx: LlmRequestContext): Promise<void> {
        const upstream = await this.forwardWebSocket(req.url, req.headers, ctx);

        // Wait for the upstream open before we ack the runtime — a failed
        // handshake surfaces as a transport-level error rather than a
        // confusing "101 then immediate close".
        await new Promise<void>((resolve, reject) => {
            const onOpen = (): void => resolve();
            const onError = (err: Error): void => reject(err);
            upstream.onOpen(onOpen);
            upstream.onError(onError);
        });

        // Ack the upgrade to the runtime (mirrors the protocol's
        // 101-equivalent start frame the runtime is waiting for).
        await req.responseBody.start({ status: 101, headers: {} });

        // Pump both directions concurrently. The HTTP case is the degenerate
        // form where the request body completes before the response begins,
        // but for WebSocket either side can terminate first: the upstream may
        // close while we're still parked awaiting the next runtime message, or
        // the runtime may cancel while the upstream is mid-stream. Racing the
        // two pumps means whichever terminates first tears the other down,
        // rather than the request pump blocking forever on an iterator that
        // will never yield again.
        let serverPumpError: Error | undefined;
        const serverDone = new Promise<void>((resolve) => {
            upstream.onMessage(async (data) => {
                try {
                    const mutated = await this.transformResponseMessage(data, ctx);
                    if (mutated === null) {
                        return;
                    }
                    await req.responseBody.write(mutated);
                } catch (err) {
                    serverPumpError ??= err instanceof Error ? err : new Error(String(err));
                    upstream.close();
                }
            });
            upstream.onClose(() => {
                resolve();
            });
            upstream.onError((err) => {
                serverPumpError ??= err;
                resolve();
            });
        });

        // Runtime → upstream. The async iterator throws when the runtime
        // cancels; we surface that so the adapter finalises cancellation.
        const clientDone = (async () => {
            for await (const chunk of req.requestBody) {
                const text = decodeFrame(chunk);
                const mutated = await this.transformRequestMessage(text, ctx);
                if (mutated === null) {
                    continue;
                }
                upstream.send(mutated);
            }
        })();

        let cancelled: unknown;
        const clientSettled = clientDone.then(
            () => "client-complete" as const,
            (err) => {
                cancelled = err;
                return "client-error" as const;
            }
        );
        const serverSettled = serverDone.then(() => "server-done" as const);

        const first = await Promise.race([clientSettled, serverSettled]);

        // Whichever side won, tear the upstream down so the loser unwinds:
        // closing makes `send` a no-op and drives the upstream's terminal
        // close callback.
        upstream.close();

        if (first === "client-error") {
            // Runtime cancellation propagating out of the request iterator.
            // Detach the server pump so its (resolved) settle isn't leaked,
            // and rethrow so the adapter finalises the cancellation.
            void serverSettled;
            throw cancelled instanceof Error ? cancelled : new Error(String(cancelled));
        }

        if (first === "client-complete") {
            // The runtime closed the request side cleanly while the upstream
            // was still open; wait for the upstream to reach its terminal
            // state (the `upstream.close()` above drives it there).
            await serverSettled;
        }

        // The upstream has terminated. If it errored, surface that — detach
        // the request pump (it self-terminates once we stop responding).
        if (serverPumpError) {
            void clientSettled;
            throw serverPumpError;
        }

        // Finalise the response. This tells the runtime to stop the request
        // stream; the request pump then settles (its iterator throws a
        // teardown cancel which `clientSettled` already absorbs), so we must
        // not await it here or we'd deadlock waiting on a stream that only
        // ends *because* we finalised.
        void clientSettled;
        await req.responseBody.end();
    }
}

// ─── Helpers ───────────────────────────────────────────────────────────

const FORBIDDEN_REQUEST_HEADERS = new Set([
    // Computed/managed by the fetch implementation; setting them through
    // the WHATWG Headers ctor either throws or is silently ignored.
    "host",
    "connection",
    "content-length",
    "transfer-encoding",
    "keep-alive",
    "upgrade",
    "proxy-connection",
    "te",
    "trailer",
]);

async function buildFetchRequest(req: LlmInferenceRequest): Promise<Request> {
    const headers = new Headers();
    for (const [name, values] of Object.entries(req.headers)) {
        if (!values) {
            continue;
        }
        if (FORBIDDEN_REQUEST_HEADERS.has(name.toLowerCase())) {
            continue;
        }
        for (const value of values) {
            headers.append(name, value);
        }
    }

    const method = req.method.toUpperCase();
    const hasBody = method !== "GET" && method !== "HEAD";

    let body: Uint8Array | undefined;
    if (hasBody) {
        const buffered = await drainAsync(req.requestBody);
        if (buffered.length > 0) {
            body = buffered;
        }
    } else {
        // Drain even GET/HEAD to keep the runtime's chunk channel from
        // backing up — bodies are always allowed on the wire even if we
        // don't forward them.
        await drainAsync(req.requestBody);
    }

    return new Request(req.url, { method, headers, body });
}

async function drainAsync(stream: AsyncIterable<Uint8Array>): Promise<Uint8Array> {
    const parts: Uint8Array[] = [];
    let total = 0;
    for await (const chunk of stream) {
        parts.push(chunk);
        total += chunk.byteLength;
    }
    if (parts.length === 0) {
        return new Uint8Array(0);
    }
    if (parts.length === 1) {
        return parts[0];
    }
    const out = new Uint8Array(total);
    let off = 0;
    for (const part of parts) {
        out.set(part, off);
        off += part.byteLength;
    }
    return out;
}

async function streamResponseToSink(response: Response, req: LlmInferenceRequest): Promise<void> {
    const headers = headersToMultiMap(response.headers);
    await req.responseBody.start({
        status: response.status,
        statusText: response.statusText || undefined,
        headers,
    });

    const body = response.body;
    if (!body) {
        await req.responseBody.end();
        return;
    }

    const reader = body.getReader();
    try {
        for (;;) {
            const { value, done } = await reader.read();
            if (done) {
                break;
            }
            if (value && value.byteLength > 0) {
                await req.responseBody.write(value);
            }
        }
        await req.responseBody.end();
    } finally {
        reader.releaseLock();
    }
}

function headersToMultiMap(headers: Headers): LlmInferenceHeaders {
    const out: Record<string, string[]> = {};
    headers.forEach((value, name) => {
        if (name.toLowerCase() === "set-cookie") {
            return;
        }
        const list = out[name] ?? (out[name] = []);
        list.push(value);
    });
    const setCookies = headers.getSetCookie();
    if (setCookies.length > 0) {
        out["set-cookie"] = setCookies;
    }
    return out;
}

const sharedTextDecoder = new TextDecoder("utf-8", { fatal: false });
const sharedTextEncoder = new TextEncoder();

function decodeFrame(chunk: Uint8Array): string {
    // The runtime sends WS text frames as UTF-8 bytes over the chunk
    // channel; the consumer side has no `binary` flag plumbed yet, so we
    // surface everything as `string`. Override the message transform
    // hooks to convert back to bytes if needed.
    return sharedTextDecoder.decode(chunk);
}

/**
 * Wrap a WHATWG global `WebSocket` in the {@link LlmWebSocketUpstream}
 * shape the WS dispatch code consumes. Exported so subclasses that
 * override {@link LlmRequestHandler.forwardWebSocket} with a global
 * `WebSocket` variant can delegate.
 *
 * @experimental
 */
export function wrapGlobalWebSocket(ws: WebSocket): LlmWebSocketUpstream {
    ws.binaryType = "arraybuffer";
    let openHandler: (() => void) | null = null;
    let messageHandler: ((data: string | Uint8Array) => void) | null = null;
    let closeHandler: ((code: number, reason: string) => void) | null = null;
    let errorHandler: ((error: Error) => void) | null = null;
    // Messages can arrive between the socket opening and the consumer
    // registering `onMessage`; buffer them so the first frames of a fast
    // upstream are never dropped.
    let inboundBuffer: (string | Uint8Array)[] | null = [];

    const deliver = (data: string | Uint8Array): void => {
        if (messageHandler) {
            messageHandler(data);
        } else {
            inboundBuffer?.push(data);
        }
    };

    ws.addEventListener("open", () => {
        openHandler?.();
    });
    ws.addEventListener("message", (event) => {
        const data = event.data;
        if (typeof data === "string") {
            deliver(data);
        } else if (data instanceof ArrayBuffer) {
            deliver(new Uint8Array(data));
        } else if (data instanceof Uint8Array) {
            deliver(data);
        } else {
            // Blob isn't expected (binaryType: "arraybuffer") but be safe.
            deliver(sharedTextEncoder.encode(String(data)));
        }
    });
    ws.addEventListener("close", (event) => {
        closeHandler?.(event.code, event.reason);
    });
    ws.addEventListener("error", () => {
        errorHandler?.(new Error("WebSocket error"));
    });

    return {
        send(data) {
            if (ws.readyState !== WebSocket.OPEN) {
                return;
            }
            ws.send(data);
        },
        close(code, reason) {
            try {
                ws.close(code, reason);
            } catch {
                // Best-effort; the socket may already be closed.
            }
        },
        onOpen(handler) {
            openHandler = handler;
            if (ws.readyState === WebSocket.OPEN) {
                handler();
            }
        },
        onMessage(handler) {
            messageHandler = handler;
            const buffered = inboundBuffer;
            inboundBuffer = null;
            if (buffered) {
                for (const data of buffered) {
                    handler(data);
                }
            }
        },
        onClose(handler) {
            closeHandler = handler;
        },
        onError(handler) {
            errorHandler = handler;
        },
    };
}
