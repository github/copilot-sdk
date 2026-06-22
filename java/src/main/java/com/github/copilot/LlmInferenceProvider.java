/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

/**
 * The low-level registration seam for servicing LLM inference requests.
 * <p>
 * The SDK consumer implements {@link #onLlmRequest}; the same callback handles
 * both buffered and streaming responses by calling the response sink's write
 * methods zero or more times before ending it. Most consumers should subclass
 * {@link LlmRequestHandler} instead, which exposes idiomatic
 * {@code java.net.http} request/response seams.
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface LlmInferenceProvider {

    /**
     * Called once per outbound model-layer request the consumer has opted to
     * handle. The consumer must eventually finalise the response by calling
     * {@link LlmInferenceResponseSink#end()} or
     * {@link LlmInferenceResponseSink#error}; throwing surfaces a transport-level
     * failure to the runtime (equivalent to calling {@code error} when the response
     * has not yet been started).
     *
     * @param request
     *            the request to service
     * @throws Exception
     *             to surface a transport-level failure to the runtime
     */
    void onLlmRequest(LlmInferenceRequest request) throws Exception;
}
