/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { LlmInferenceHeaders } from "./generated/rpc.js";
import type { LlmInferenceProvider, LlmInferenceRequest, LlmInferenceResponseSink } from "./llmInferenceProvider.js";

const sharedTextDecoder = new TextDecoder("utf-8", { fatal: false });
const kBridge = Symbol("llmWebSocketResponseBridge");
const kCompletion = Symbol("llmWebSocketCompletion");
const kOpen = Symbol("llmWebSocketOpen");
const kSuppressCloseOnDispose = Symbol("llmWebSocketSuppressCloseOnDispose");

type InternalContext = LlmRequestContext & { [kBridge]: LlmWebSocketResponseBridge };

/**
 * Per-request context handed to every {@link LlmRequestHandler} hook.
 *
 * @experimental
 */
export interface LlmRequestContext {
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
export class LlmWebSocketCloseStatus {
    static readonly normalClosure = new LlmWebSocketCloseStatus();

    constructor(
        readonly description?: string,
        readonly errorCode?: string,
        readonly error?: Error
    ) {}
}

/**
 * Per-connection WebSocket handler returned by {@link LlmRequestHandler.openWebSocket}.
 *
 * @experimental
 */
export abstract class CopilotWebSocketHandler implements AsyncDisposable {
    readonly #response: LlmWebSocketResponseBridge;
    readonly #completion: Promise<LlmWebSocketCloseStatus>;
    #resolveCompletion!: (status: LlmWebSocketCloseStatus) => void;
    #closed = false;
    [kSuppressCloseOnDispose] = false;

    protected readonly context: LlmRequestContext;

    protected constructor(context: LlmRequestContext) {
        this.context = context;
        const bridge = (context as Partial<InternalContext>)[kBridge];
        if (!bridge) {
            throw new Error("WebSocket response bridge is not attached");
        }
        this.#response = bridge;
        this.#completion = new Promise<LlmWebSocketCloseStatus>((resolve) => {
            this.#resolveCompletion = resolve;
        });
    }

    async sendResponseMessage(data: string | Uint8Array): Promise<void> {
        await this.#response.write(data);
    }

    async close(status: LlmWebSocketCloseStatus = LlmWebSocketCloseStatus.normalClosure): Promise<void> {
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
            await this.close(LlmWebSocketCloseStatus.normalClosure);
        }
    }

    /** @internal */
    get [kCompletion](): Promise<LlmWebSocketCloseStatus> {
        return this.#completion;
    }

    /** @internal */
    async [kOpen](): Promise<void> {}
}

/**
 * Default pass-through WebSocket handler backed by the WHATWG `WebSocket`.
 *
 * @experimental
 */
export class ForwardingWebSocketHandler extends CopilotWebSocketHandler {
    readonly #url: string;
    #upstream: WebSocket | null = null;

    constructor(context: LlmRequestContext, url = context.url) {
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
            void this.sendResponseMessage(normalizeWsData(event.data)).catch(async (err: unknown) => {
                await this.close(
                    new LlmWebSocketCloseStatus(
                        err instanceof Error ? err.message : String(err),
                        undefined,
                        err instanceof Error ? err : new Error(String(err))
                    )
                );
            });
        });
        upstream.addEventListener("close", () => {
            void this.close(LlmWebSocketCloseStatus.normalClosure);
        });
        upstream.addEventListener("error", () => {
            void this.close(new LlmWebSocketCloseStatus("WebSocket error", undefined, new Error("WebSocket error")));
        });
        await new Promise<void>((resolve, reject) => {
            if (upstream.readyState === WebSocket.OPEN) {
                resolve();
                return;
            }
            upstream.addEventListener("open", () => resolve(), { once: true });
            upstream.addEventListener("error", () => reject(new Error("WebSocket error")), { once: true });
        });
    }

    override async close(
        status: LlmWebSocketCloseStatus = LlmWebSocketCloseStatus.normalClosure
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
 * Base class for SDK consumers who want to observe or mutate the LLM
 * inference requests the runtime issues.
 *
 * @experimental
 */
export class LlmRequestHandler implements LlmInferenceProvider {
    async onLlmRequest(req: LlmInferenceRequest): Promise<void> {
        const bridge = new LlmWebSocketResponseBridge(req.responseBody);
        const ctx: InternalContext = {
            requestId: req.requestId,
            sessionId: req.sessionId,
            transport: req.transport,
            url: req.url,
            headers: req.headers,
            signal: req.signal,
            [kBridge]: bridge,
        };

        if (req.transport === "websocket") {
            await this.#handleWebSocket(req, ctx);
        } else {
            await this.#handleHttp(req, ctx);
        }
    }

    protected sendRequest(request: Request, ctx: LlmRequestContext): Promise<Response> {
        return fetch(request, { signal: ctx.signal });
    }

    protected openWebSocket(ctx: LlmRequestContext): Promise<CopilotWebSocketHandler> {
        return Promise.resolve(new ForwardingWebSocketHandler(ctx));
    }

    async #handleHttp(req: LlmInferenceRequest, ctx: LlmRequestContext): Promise<void> {
        const request = await buildFetchRequest(req);
        const response = await this.sendRequest(request, ctx);
        await streamResponseToSink(response, req);
    }

    async #handleWebSocket(req: LlmInferenceRequest, ctx: InternalContext): Promise<void> {
        const handler = await this.openWebSocket(ctx);
        try {
            await handler[kOpen]();
            await ctx[kBridge].start();

            let cancelled: unknown;
            const clientSettled = (async () => {
                for await (const chunk of req.requestBody) {
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
                await handler.close(LlmWebSocketCloseStatus.normalClosure);
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

class LlmWebSocketResponseBridge {
    readonly #sink: LlmInferenceResponseSink;
    readonly #pending: Array<() => Promise<void>> = [];
    #started = false;
    #completed = false;
    #serial: Promise<void> = Promise.resolve();

    constructor(sink: LlmInferenceResponseSink) {
        this.#sink = sink;
    }

    async start(): Promise<void> {
        await this.#enqueue(async () => {
            if (this.#started) {
                return;
            }
            this.#started = true;
            await this.#sink.start({ status: 101, headers: {} });
            while (this.#pending.length > 0) {
                await this.#pending.shift()!();
            }
        });
    }

    async write(data: string | Uint8Array): Promise<void> {
        await this.#enqueueOrBuffer(async () => {
            if (!this.#completed) {
                await this.#sink.write(data);
            }
        });
    }

    async end(): Promise<void> {
        await this.#enqueueOrBuffer(async () => {
            if (this.#completed) {
                return;
            }
            this.#completed = true;
            await this.#sink.end();
        });
    }

    async error(error: { message: string; code?: string }): Promise<void> {
        await this.#enqueueOrBuffer(async () => {
            if (this.#completed) {
                return;
            }
            this.#completed = true;
            await this.#sink.error(error);
        });
    }

    async #enqueueOrBuffer(action: () => Promise<void>): Promise<void> {
        if (!this.#started) {
            this.#pending.push(action);
            return;
        }
        await this.#enqueue(action);
    }

    async #enqueue(action: () => Promise<void>): Promise<void> {
        const run = this.#serial.then(action, action);
        this.#serial = run.catch(() => {});
        await run;
    }
}
