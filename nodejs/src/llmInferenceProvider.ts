/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type {
    LlmInferenceHandler,
    LlmInferenceHeaders,
    LlmInferenceHttpRequestRequest,
    LlmInferenceHttpRequestResult,
    LlmInferenceHttpStreamStartRequest,
    LlmInferenceHttpStreamStartResult,
} from "./generated/rpc.js";
import type { createServerRpc } from "./generated/rpc.js";

type ServerRpc = ReturnType<typeof createServerRpc>;

/**
 * An outbound LLM HTTP request the runtime is asking the SDK consumer to
 * handle on its behalf.
 *
 * This is a deliberately low-level shape: the runtime forwards the request
 * verbatim and does not classify it (no provider type, endpoint kind, wire
 * API, model id, etc.). Consumers that need that information should derive
 * it themselves from the URL / headers / body.
 *
 * `body` is provided as either `bodyText` (when the runtime sent a text
 * body) or `bodyBase64` (when the runtime sent binary bytes) — at most one
 * is set, mirroring the wire shape.
 */
export interface LlmInferenceRequest {
    /** Opaque runtime-minted id for this request. Stable across the request lifecycle, useful for logging. */
    requestId: string;
    /**
     * Id of the runtime session that triggered this request. Absent for
     * requests issued outside any session (e.g. startup model catalog /
     * capability resolution).
     */
    sessionId?: string;
    /** HTTP method (`GET`, `POST`, ...). */
    method: string;
    /** Absolute URL the runtime would have sent the request to. */
    url: string;
    /**
     * HTTP headers, lowercased and multi-valued. Multi-valued headers
     * (e.g. `Set-Cookie`) preserve all values.
     */
    headers: LlmInferenceHeaders;
    /** Body as UTF-8 text. Set instead of `bodyBase64` when the body is text. */
    bodyText?: string;
    /** Body as base64-encoded bytes. Set instead of `bodyText` when the body is binary. */
    bodyBase64?: string;
}

/**
 * Response the SDK consumer returns from {@link LlmInferenceProvider.onLlmRequest}
 * to be surfaced to the runtime as if the runtime had issued the request itself.
 *
 * Set `bodyText` for UTF-8 text responses, `bodyBase64` for binary responses, or
 * neither if there is no body. Provide `error` to signal a transport-level
 * failure (the runtime will raise an `APIConnectionError` and apply its normal
 * retry policy).
 */
export interface LlmInferenceResponse {
    status: number;
    statusText?: string;
    headers?: LlmInferenceHeaders;
    bodyText?: string;
    bodyBase64?: string;
    error?: { message: string; code?: string };
}

/**
 * Response head returned synchronously from {@link LlmInferenceProvider.onLlmStreamRequest}.
 * Body chunks follow via the `pushChunk` / `end` callbacks the SDK passes to
 * the provider. The chunk pump runs asynchronously in the background; the
 * provider may finish issuing chunks long after `onLlmStreamRequest` itself
 * resolves.
 */
export interface LlmInferenceStreamStartResponse {
    status: number;
    statusText?: string;
    headers?: LlmInferenceHeaders;
    error?: { message: string; code?: string };
}

/**
 * Stream chunk sink the SDK hands the provider on a stream-start callback.
 * The provider calls `pushChunk(bytes)` for each body chunk and `end()` (or
 * `end(errorMessage)`) when the stream completes (or fails transport-side).
 *
 * `pushChunk` and `end` are safe to call any number of times after
 * `onLlmStreamRequest` resolves — the SDK retains the bound functions until
 * `end` is called.
 */
export interface LlmInferenceStreamSink {
    pushChunk(data: Uint8Array): Promise<void>;
    end(errorMessage?: string): Promise<void>;
}

/**
 * Interface for an LLM inference provider. The SDK consumer implements
 * `onLlmRequest`, throws on failure or returns a response.
 *
 * Use {@link createLlmInferenceAdapter} to convert an
 * {@link LlmInferenceProvider} into the {@link LlmInferenceHandler} expected
 * by the SDK's RPC layer.
 */
export interface LlmInferenceProvider {
    /**
     * Called by the runtime once per outbound LLM HTTP request the consumer
     * has opted to handle. Throwing is equivalent to returning
     * `{ error: { message: err.message } }`.
     */
    onLlmRequest(request: LlmInferenceRequest): Promise<LlmInferenceResponse>;

    /**
     * Called by the runtime for streaming inference requests (chat completions
     * / responses streaming). Return the response head synchronously, and use
     * `sink.pushChunk` / `sink.end` to deliver body chunks asynchronously.
     *
     * If absent, streaming inference falls back to a transport error — the
     * runtime treats this provider as not handling streaming.
     */
    onLlmStreamRequest?(
        request: LlmInferenceRequest,
        sink: LlmInferenceStreamSink,
    ): Promise<LlmInferenceStreamStartResponse>;
}

/**
 * Adapt an {@link LlmInferenceProvider} into the generated
 * {@link LlmInferenceHandler} shape consumed by the SDK's RPC dispatcher.
 *
 * Errors thrown by the provider are caught and converted to a
 * transport-error response (`{ error: { message } }`). Returning the result
 * verbatim lets the consumer either throw idiomatically or return a
 * structured error.
 *
 * `serverRpc` is used to send streamed body chunks back to the runtime via
 * the `llmInference.streamChunk` / `streamEnd` server methods.
 */
export function createLlmInferenceAdapter(
    provider: LlmInferenceProvider,
    getServerRpc: () => ServerRpc | undefined,
): LlmInferenceHandler {
    return {
        httpRequest: async (params: LlmInferenceHttpRequestRequest): Promise<LlmInferenceHttpRequestResult> => {
            let response: LlmInferenceResponse;
            try {
                response = await provider.onLlmRequest({
                    requestId: params.requestId,
                    sessionId: params.sessionId,
                    method: params.method,
                    url: params.url,
                    headers: params.headers,
                    bodyText: params.bodyText,
                    bodyBase64: params.bodyBase64,
                });
            } catch (err) {
                const message = err instanceof Error ? err.message : String(err);
                return {
                    status: 0,
                    headers: {},
                    error: { message },
                };
            }
            return {
                status: response.status,
                statusText: response.statusText,
                headers: response.headers ?? {},
                bodyText: response.bodyText,
                bodyBase64: response.bodyBase64,
                error: response.error,
            };
        },
        httpStreamStart: async (
            params: LlmInferenceHttpStreamStartRequest,
        ): Promise<LlmInferenceHttpStreamStartResult> => {
            if (!provider.onLlmStreamRequest) {
                return {
                    status: 0,
                    headers: {},
                    error: { message: "LLM inference provider does not implement onLlmStreamRequest." },
                };
            }
            const sink: LlmInferenceStreamSink = {
                async pushChunk(data: Uint8Array): Promise<void> {
                    const rpc = getServerRpc();
                    if (!rpc) {
                        return;
                    }
                    await rpc.llmInference.streamChunk({
                        streamToken: params.streamToken,
                        dataBase64: Buffer.from(data).toString("base64"),
                    });
                },
                async end(errorMessage?: string): Promise<void> {
                    const rpc = getServerRpc();
                    if (!rpc) {
                        return;
                    }
                    await rpc.llmInference.streamEnd({
                        streamToken: params.streamToken,
                        error: errorMessage,
                    });
                },
            };
            const request: LlmInferenceRequest = {
                requestId: params.requestId,
                sessionId: params.sessionId,
                method: params.method,
                url: params.url,
                headers: params.headers,
                bodyText: params.bodyText,
                bodyBase64: params.bodyBase64,
            };
            let head: LlmInferenceStreamStartResponse;
            try {
                head = await provider.onLlmStreamRequest(request, sink);
            } catch (err) {
                const message = err instanceof Error ? err.message : String(err);
                return { status: 0, headers: {}, error: { message } };
            }
            return {
                status: head.status,
                statusText: head.statusText,
                headers: head.headers ?? {},
                error: head.error,
            };
        },
    };
}

