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

const sharedTextDecoder = new TextDecoder("utf-8", { fatal: false });
const sharedTextEncoder = new TextEncoder();

const kBridge = Symbol("copilotWebSocketResponseBridge");
const kCompletion = Symbol("copilotWebSocketCompletion");
const kOpen = Symbol("copilotWebSocketOpen");
const kSuppressCloseOnDispose = Symbol("copilotWebSocketSuppressCloseOnDispose");
const kHandle = Symbol("copilotRequestHandle");

type InternalContext = CopilotRequestContext & { [kBridge]: CopilotWebSocketResponseBridge };

/**
 * Per-request context handed to every {@link CopilotRequestHandler} hook.
 *
 * @experimental
 */
export interface CopilotRequestContext {
    readonly requestId: string;
    readonly sessionId?: string;
    readonly transport: "http" | "websocket";
    readonly url: string;
    readonly headers: LlmInferenceHeaders;
    readonly signal: AbortSignal;
}

/**
 * Terminal status for a callback-owned WebSocket connection.
 *
 * @experimental
 */
export class CopilotWebSocketCloseStatus {
    static readonly normalClosure = new CopilotWebSocketCloseStatus();

    constructor(
        readonly description?: string,
        readonly errorCode?: string,
        readonly error?: Error
    ) {}
}

/**
 * Lower-level WebSocket handler with no upstream connection.
 *
 * This is the abstract base shared by all WebSocket handlers. It does not open
 * or forward to any upstream server on its own — subclass it directly only when
 * you want to service a fully synthetic connection yourself (e.g. answer the
 * runtime without any real backend). For the common case of mutating and
 * forwarding traffic to the real upstream, subclass {@link CopilotWebSocketHandler}
 * instead, which connects upstream and forwards by default.
 *
 * @experimental
 */
export abstract class CopilotWebSocketHandlerBase implements AsyncDisposable {
    readonly #response: CopilotWebSocketResponseBridge;
    readonly #completion: Promise<CopilotWebSocketCloseStatus>;
    #resolveCompletion!: (status: CopilotWebSocketCloseStatus) => void;
    #closed = false;
    [kSuppressCloseOnDispose] = false;

    protected readonly context: CopilotRequestContext;

    protected constructor(context: CopilotRequestContext) {
        this.context = context;
        const bridge = (context as Partial<InternalContext>)[kBridge];
        if (!bridge) {
            throw new Error("WebSocket response bridge is not attached");
        }
        this.#response = bridge;
        this.#completion = new Promise<CopilotWebSocketCloseStatus>((resolve) => {
            this.#resolveCompletion = resolve;
        });
    }

    async sendResponseMessage(data: string | Uint8Array): Promise<void> {
        await this.#response.write(data);
    }

    async close(
        status: CopilotWebSocketCloseStatus = CopilotWebSocketCloseStatus.normalClosure
    ): Promise<void> {
        if (this.#closed) {
            return;
        }
        this.#closed = true;
        if (status.error) {
            await this.#response.error({
                message: status.description ?? status.error.message,
                code: status.errorCode,
            });
        } else {
            await this.#response.end();
        }
        this.#resolveCompletion(status);
    }

    abstract sendRequestMessage(data: string | Uint8Array): Promise<void> | void;

    async [Symbol.asyncDispose](): Promise<void> {
        if (!this[kSuppressCloseOnDispose] && !this.#closed) {
            await this.close(CopilotWebSocketCloseStatus.normalClosure);
        }
    }

    /** @internal */
    get [kCompletion](): Promise<CopilotWebSocketCloseStatus> {
        return this.#completion;
    }

    /** @internal */
    async [kOpen](): Promise<void> {}
}

/**
 * WebSocket handler that connects to the real upstream and forwards traffic by
 * default. This is the type returned by the default
 * {@link CopilotRequestHandler.openWebSocket}.
 *
 * Override nothing to get full pass-through. To mutate traffic, subclass this
 * type and override a message hook, then call `super` to keep forwarding to the
 * upstream. (Subclassing {@link CopilotWebSocketHandlerBase} instead would drop
 * forwarding entirely.)
 *
 * @experimental
 */
export class CopilotWebSocketHandler extends CopilotWebSocketHandlerBase {
    readonly #url: string;
    #upstream: WebSocket | null = null;

    constructor(context: CopilotRequestContext, url = context.url) {
        super(context);
        this.#url = url;
    }

    override sendRequestMessage(data: string | Uint8Array): void {
        if (this.#upstream?.readyState !== WebSocket.OPEN) {
            return;
        }
        this.#upstream.send(data);
    }

    /** @internal */
    override async [kOpen](): Promise<void> {
        if (this.#upstream) {
            return;
        }
        const upstream = new WebSocket(this.#url);
        upstream.binaryType = "arraybuffer";
        this.#upstream = upstream;
        upstream.addEventListener("message", (event) => {
            void this.sendResponseMessage(normalizeWsData(event.data)).catch(
                async (err: unknown) => {
                    await this.close(
                        new CopilotWebSocketCloseStatus(
                            err instanceof Error ? err.message : String(err),
                            undefined,
                            err instanceof Error ? err : new Error(String(err))
                        )
                    );
                }
            );
        });
        upstream.addEventListener("close", () => {
            void this.close(CopilotWebSocketCloseStatus.normalClosure);
        });
        upstream.addEventListener("error", () => {
            void this.close(
                new CopilotWebSocketCloseStatus(
                    "WebSocket error",
                    undefined,
                    new Error("WebSocket error")
                )
            );
        });
        await new Promise<void>((resolve, reject) => {
            if (upstream.readyState === WebSocket.OPEN) {
                resolve();
                return;
            }
            upstream.addEventListener("open", () => resolve(), { once: true });
            upstream.addEventListener("error", () => reject(new Error("WebSocket error")), {
                once: true,
            });
        });
    }

    override async close(
        status: CopilotWebSocketCloseStatus = CopilotWebSocketCloseStatus.normalClosure
    ): Promise<void> {
        try {
            if (
                this.#upstream?.readyState === WebSocket.OPEN ||
                this.#upstream?.readyState === WebSocket.CONNECTING
            ) {
                this.#upstream?.close();
            }
        } catch {
            // Best-effort; the socket may already be closed.
        }
        await super.close(status);
    }

    override async [Symbol.asyncDispose](): Promise<void> {
        try {
            await super[Symbol.asyncDispose]();
        } finally {
            try {
                this.#upstream?.close();
            } catch {
                // Best-effort.
            }
        }
    }
}

/**
 * Base class for SDK consumers who want to observe or mutate the outbound
 * model-layer requests the runtime issues (for both CAPI and BYOK providers).
 * Subclass and override {@link sendRequest} or {@link openWebSocket}; an
 * instance that overrides nothing is a transparent pass-through.
 *
 * @experimental
 */
export class CopilotRequestHandler {
    protected sendRequest(request: Request, ctx: CopilotRequestContext): Promise<Response> {
        return fetch(request, { signal: ctx.signal });
    }

    protected openWebSocket(ctx: CopilotRequestContext): Promise<CopilotWebSocketHandlerBase> {
        return Promise.resolve(new CopilotWebSocketHandler(ctx));
    }

    /** @internal */
    async [kHandle](exchange: CopilotRequestExchange): Promise<void> {
        const bridge = new CopilotWebSocketResponseBridge(exchange);
        const ctx: InternalContext = {
            requestId: exchange.requestId,
            sessionId: exchange.sessionId,
            transport: exchange.transport,
            url: exchange.url,
            headers: exchange.headers,
            signal: exchange.signal,
            [kBridge]: bridge,
        };

        if (exchange.transport === "websocket") {
            await this.#handleWebSocket(exchange, ctx);
        } else {
            await this.#handleHttp(exchange, ctx);
        }
    }

    async #handleHttp(exchange: CopilotRequestExchange, ctx: CopilotRequestContext): Promise<void> {
        const request = await buildFetchRequest(exchange);
        const response = await this.sendRequest(request, ctx);
        await streamResponse(response, exchange);
    }

    async #handleWebSocket(exchange: CopilotRequestExchange, ctx: InternalContext): Promise<void> {
        const handler = await this.openWebSocket(ctx);
        try {
            await handler[kOpen]();

            // The runtime blocks the WebSocket connect until it receives the
            // 101 response head (the upgrade acknowledgement) and only then
            // begins forwarding inbound messages as request-body chunks. Emit
            // it eagerly here — waiting for the first upstream message would
            // deadlock, since the upstream stays silent until it receives a
            // request message the runtime won't send before the upgrade
            // completes.
            await ctx[kBridge].start();

            let cancelled: unknown;
            const clientSettled = (async () => {
                for await (const chunk of exchange.requestBody) {
                    await handler.sendRequestMessage(decodeFrame(chunk));
                }
                return "client-complete" as const;
            })().catch((err) => {
                cancelled = err;
                return "client-error" as const;
            });

            const first = await Promise.race([
                clientSettled,
                handler[kCompletion].then(() => "server-done" as const),
            ]);

            if (first === "client-error") {
                handler[kSuppressCloseOnDispose] = true;
                throw cancelled instanceof Error ? cancelled : new Error(String(cancelled));
            }

            if (first === "client-complete") {
                await handler.close(CopilotWebSocketCloseStatus.normalClosure);
                await handler[kCompletion];
                return;
            }

            const status = await handler[kCompletion];
            if (status.error) {
                throw status.error;
            }
        } finally {
            await handler[Symbol.asyncDispose]();
        }
    }
}

/**
 * Adapt a {@link CopilotRequestHandler} into the generated
 * {@link LlmInferenceHandler} shape consumed by the SDK's RPC dispatcher.
 *
 * Maintains a per-`requestId` table of {@link CopilotRequestExchange}: each
 * `httpRequestStart` allocates one and fires the handler in the background,
 * returning immediately so the runtime's RPC reply is not gated on the
 * consumer's I/O. Subsequent `httpRequestChunk` frames are routed into the
 * matching exchange's body stream.
 *
 * @internal
 */
export function createCopilotRequestAdapter(
    handler: CopilotRequestHandler,
    getServerRpc: () => ServerRpc | undefined
): LlmInferenceHandler {
    const pending = new Map<string, CopilotRequestExchange>();

    function getOrCreate(requestId: string): CopilotRequestExchange {
        // The runtime dispatches httpRequestStart and httpRequestChunk frames
        // independently. get-or-create keeps the adapter correct regardless of
        // arrival order: a body chunk (including the terminal end frame) that
        // races ahead of its start frame is buffered into the same exchange
        // rather than dropped, which would otherwise hang the body drain.
        let exchange = pending.get(requestId);
        if (!exchange) {
            exchange = new CopilotRequestExchange(requestId, getServerRpc);
            pending.set(requestId, exchange);
        }
        return exchange;
    }

    async function run(exchange: CopilotRequestExchange): Promise<void> {
        try {
            await handler[kHandle](exchange);
            if (!exchange.finished) {
                await finalize(
                    exchange,
                    502,
                    "Copilot request handler returned without finalising the response (call responseBody.end() or .error())."
                );
            }
        } catch (err) {
            if (exchange.cancelled || exchange.signal.aborted) {
                // The runtime already cancelled this request; the handler's
                // throw is just the abort propagating out of its upstream call.
                await finalize(exchange, 499, "Request cancelled by runtime", "cancelled");
                return;
            }
            const message = err instanceof Error ? err.message : String(err);
            await finalize(exchange, 502, message);
        } finally {
            pending.delete(exchange.requestId);
        }
    }

    return {
        async httpRequestStart(
            params: LlmInferenceHttpRequestStartRequest
        ): Promise<LlmInferenceHttpRequestStartResult> {
            // Adopt any exchange a racing chunk already created — with its
            // buffered body — rather than dropping those frames.
            const exchange = getOrCreate(params.requestId);
            exchange.setContext(params);
            void run(exchange);
            return {};
        },
        async httpRequestChunk(
            params: LlmInferenceHttpRequestChunkRequest
        ): Promise<LlmInferenceHttpRequestChunkResult> {
            // May arrive before the matching start frame; get-or-create so the
            // body is buffered, never lost.
            routeChunk(getOrCreate(params.requestId), params);
            return {};
        },
    };
}

async function finalize(
    exchange: CopilotRequestExchange,
    status: number,
    message: string,
    code?: string
): Promise<void> {
    if (exchange.finished) {
        return;
    }
    try {
        if (!exchange.started) {
            await exchange.startResponse({ status, headers: {} });
        }
        await exchange.errorResponse({ message, code });
    } catch {
        // Best-effort — the connection may already be dead.
    }
}

function routeChunk(
    exchange: CopilotRequestExchange,
    params: LlmInferenceHttpRequestChunkRequest
): void {
    if (params.cancel) {
        exchange.pushCancel(params.cancelReason);
        return;
    }
    if (params.data && params.data.length > 0) {
        exchange.pushChunk(decodeChunkData(params.data, !!params.binary));
    }
    if (params.end) {
        exchange.pushEnd();
    }
}

/** Response head emitted to the runtime via {@link CopilotRequestExchange.startResponse}. */
interface ResponseInit {
    status: number;
    statusText?: string;
    headers?: LlmInferenceHeaders;
}

interface BodyQueueItem {
    chunk?: Uint8Array;
    end?: boolean;
    cancel?: { reason?: string };
}

/**
 * One intercepted request in flight. Carries the request context plus the body
 * byte stream the runtime feeds in via `httpRequestChunk` frames, and emits the
 * handler's response straight back to the runtime through the generated
 * `llmInference` server API. Replaces the former provider/sink/response-channel
 * indirection with a single object the adapter owns and the handler drives.
 */
class CopilotRequestExchange {
    readonly requestId: string;
    sessionId?: string;
    method = "GET";
    url = "";
    headers: LlmInferenceHeaders = {};
    transport: "http" | "websocket" = "http";

    readonly #getServerRpc: () => ServerRpc | undefined;
    readonly #abort = new AbortController();
    readonly #buffer: BodyQueueItem[] = [];
    #waker: (() => void) | null = null;
    #drained = false;
    #started = false;
    #finished = false;
    #cancelled = false;

    constructor(requestId: string, getServerRpc: () => ServerRpc | undefined) {
        this.requestId = requestId;
        this.#getServerRpc = getServerRpc;
    }

    /** Fill in the request context once the matching start frame arrives. */
    setContext(params: LlmInferenceHttpRequestStartRequest): void {
        this.sessionId = params.sessionId;
        this.method = params.method;
        this.url = params.url;
        this.headers = params.headers;
        this.transport = params.transport ?? "http";
    }

    get signal(): AbortSignal {
        return this.#abort.signal;
    }

    get started(): boolean {
        return this.#started;
    }

    get finished(): boolean {
        return this.#finished;
    }

    get cancelled(): boolean {
        return this.#cancelled;
    }

    // --- Request body feed (driven by the adapter as chunk frames arrive) ---

    pushChunk(chunk: Uint8Array): void {
        this.#push({ chunk });
    }

    pushEnd(): void {
        this.#push({ end: true });
    }

    pushCancel(reason?: string): void {
        this.#cancelled = true;
        this.#abort.abort();
        this.#push({ cancel: { reason } });
    }

    #push(item: BodyQueueItem): void {
        this.#buffer.push(item);
        const w = this.#waker;
        this.#waker = null;
        w?.();
    }

    /**
     * Request body bytes, yielded as they arrive. A cancel frame surfaces as a
     * thrown error so the handler's upstream call is torn down.
     */
    get requestBody(): AsyncIterable<Uint8Array> {
        return {
            [Symbol.asyncIterator]: (): AsyncIterator<Uint8Array> => ({
                next: async (): Promise<IteratorResult<Uint8Array>> => {
                    if (this.#drained) {
                        return { value: undefined, done: true };
                    }
                    while (this.#buffer.length === 0) {
                        await new Promise<void>((resolve) => {
                            this.#waker = resolve;
                        });
                    }
                    const item = this.#buffer.shift()!;
                    if (item.cancel) {
                        this.#drained = true;
                        throw new Error(
                            item.cancel.reason
                                ? `Request cancelled by runtime: ${item.cancel.reason}`
                                : "Request cancelled by runtime"
                        );
                    }
                    if (item.end) {
                        this.#drained = true;
                        return { value: undefined, done: true };
                    }
                    return { value: item.chunk ?? new Uint8Array(), done: false };
                },
            }),
        };
    }

    // --- Response emit (driven by the handler). Strict state machine: ---
    // startResponse once -> 0..N writeResponse -> exactly one of
    // endResponse / errorResponse.

    async startResponse(init: ResponseInit): Promise<void> {
        if (this.#started) {
            throw new Error("Copilot request response start() called twice.");
        }
        if (this.#finished) {
            throw new Error("Copilot request response already finished.");
        }
        this.#started = true;
        await this.#rpc().llmInference.httpResponseStart({
            requestId: this.requestId,
            status: init.status,
            statusText: init.statusText,
            headers: init.headers ?? {},
        });
    }

    async writeResponse(data: string | Uint8Array): Promise<void> {
        if (this.#cancelled) {
            throw new Error("Copilot request was cancelled by the runtime.");
        }
        if (!this.#started) {
            throw new Error("Copilot request response write() called before start().");
        }
        if (this.#finished) {
            throw new Error("Copilot request response write() called after end()/error().");
        }
        const isString = typeof data === "string";
        await this.#rpc().llmInference.httpResponseChunk({
            requestId: this.requestId,
            data: isString ? data : Buffer.from(data).toString("base64"),
            binary: !isString,
            end: false,
        });
    }

    async endResponse(): Promise<void> {
        if (this.#finished) {
            return;
        }
        this.#finished = true;
        await this.#rpc().llmInference.httpResponseChunk({
            requestId: this.requestId,
            data: "",
            end: true,
        });
    }

    async errorResponse(error: { message: string; code?: string }): Promise<void> {
        if (this.#finished) {
            return;
        }
        this.#finished = true;
        await this.#rpc().llmInference.httpResponseChunk({
            requestId: this.requestId,
            data: "",
            end: true,
            error: { message: error.message, code: error.code },
        });
    }

    #rpc(): ServerRpc {
        const r = this.#getServerRpc();
        if (!r) {
            throw new Error("Copilot request response used after RPC connection closed.");
        }
        return r;
    }
}

const FORBIDDEN_REQUEST_HEADERS = new Set([
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

async function buildFetchRequest(exchange: CopilotRequestExchange): Promise<Request> {
    const headers = new Headers();
    for (const [name, values] of Object.entries(exchange.headers)) {
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

    const method = exchange.method.toUpperCase();
    const hasBody = method !== "GET" && method !== "HEAD";

    let body: Uint8Array | undefined;
    if (hasBody) {
        const buffered = await drainAsync(exchange.requestBody);
        if (buffered.length > 0) {
            body = buffered;
        }
    } else {
        await drainAsync(exchange.requestBody);
    }

    return new Request(exchange.url, { method, headers, body });
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

async function streamResponse(response: Response, exchange: CopilotRequestExchange): Promise<void> {
    await exchange.startResponse({
        status: response.status,
        statusText: response.statusText || undefined,
        headers: headersToMultiMap(response.headers),
    });

    const body = response.body;
    if (!body) {
        await exchange.endResponse();
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
                await exchange.writeResponse(value);
            }
        }
        await exchange.endResponse();
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

function decodeChunkData(data: string, binary: boolean): Uint8Array {
    if (binary) {
        return new Uint8Array(Buffer.from(data, "base64"));
    }
    return sharedTextEncoder.encode(data);
}

function decodeFrame(chunk: Uint8Array): string {
    return sharedTextDecoder.decode(chunk);
}

function normalizeWsData(data: unknown): string | Uint8Array {
    if (typeof data === "string") {
        return data;
    }
    if (data instanceof Uint8Array) {
        return data;
    }
    if (data instanceof ArrayBuffer) {
        return new Uint8Array(data);
    }
    return new Uint8Array();
}

/**
 * Forwards upstream WebSocket messages back to the owning
 * {@link CopilotRequestExchange}. The 101 upgrade head is emitted eagerly via
 * {@link start} (the runtime gates the connect on it); thereafter writes are
 * serialised so the head always precedes any body or terminal frame.
 */
class CopilotWebSocketResponseBridge {
    readonly #exchange: CopilotRequestExchange;
    #started = false;
    #completed = false;
    #serial: Promise<void> = Promise.resolve();

    constructor(exchange: CopilotRequestExchange) {
        this.#exchange = exchange;
    }

    /** Emit the 101 upgrade head now, acknowledging the WebSocket connect. */
    start(): Promise<void> {
        return this.#run(false, () => Promise.resolve());
    }

    write(data: string | Uint8Array): Promise<void> {
        return this.#run(false, () => this.#exchange.writeResponse(data));
    }

    end(): Promise<void> {
        return this.#run(true, () => this.#exchange.endResponse());
    }

    error(error: { message: string; code?: string }): Promise<void> {
        return this.#run(true, () => this.#exchange.errorResponse(error));
    }

    #run(terminal: boolean, action: () => Promise<void>): Promise<void> {
        const task = this.#serial.then(async () => {
            if (this.#completed) {
                return;
            }
            if (!this.#started) {
                this.#started = true;
                await this.#exchange.startResponse({ status: 101, headers: {} });
            }
            if (terminal) {
                this.#completed = true;
            }
            await action();
        });
        this.#serial = task.catch(() => {});
        return task;
    }
}
