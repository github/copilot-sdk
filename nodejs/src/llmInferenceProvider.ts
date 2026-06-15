/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type {
    LlmInferenceHandler,
    LlmInferenceHeaders,
    LlmInferenceHttpRequestRequest,
    LlmInferenceHttpRequestResult,
    LlmInferenceRequestMetadata,
} from "./generated/rpc.js";

/**
 * An outbound LLM HTTP request the runtime is asking the SDK consumer to
 * handle on its behalf.
 *
 * `body` is provided as both `bodyText` (when the runtime sent a text body)
 * and `bodyBase64` (when the runtime sent binary bytes) — exactly one is set,
 * mirroring the wire shape.
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
    /** Metadata describing the request (provider, endpoint kind, etc.). */
    metadata: LlmInferenceRequestMetadata;
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
}

/**
 * Adapt an {@link LlmInferenceProvider} into the generated
 * {@link LlmInferenceHandler} shape consumed by the SDK's RPC dispatcher.
 *
 * Errors thrown by the provider are caught and converted to a
 * transport-error response (`{ error: { message } }`). Returning the result
 * verbatim lets the consumer either throw idiomatically or return a
 * structured error.
 */
export function createLlmInferenceAdapter(provider: LlmInferenceProvider): LlmInferenceHandler {
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
                    metadata: params.metadata,
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
    };
}
