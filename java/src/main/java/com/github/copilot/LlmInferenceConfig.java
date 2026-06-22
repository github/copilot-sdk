/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.util.Objects;

/**
 * Configures a connection-level LLM inference callback.
 * <p>
 * When set on {@link com.github.copilot.rpc.CopilotClientOptions}, the client
 * registers as the inference provider on connect, and the runtime routes its
 * model-layer HTTP and WebSocket traffic through the configured handler instead
 * of issuing the calls itself. This applies to both BYOK and CAPI traffic.
 *
 * @since 1.0.0
 */
public final class LlmInferenceConfig {

    private LlmInferenceProvider handler;

    /**
     * Creates an empty configuration.
     */
    public LlmInferenceConfig() {
    }

    /**
     * Creates a configuration wrapping the given handler.
     *
     * @param handler
     *            the handler that services intercepted requests
     */
    public LlmInferenceConfig(LlmInferenceProvider handler) {
        this.handler = handler;
    }

    /**
     * Gets the handler that services intercepted requests.
     *
     * @return the handler, or {@code null} if not set
     */
    public LlmInferenceProvider getHandler() {
        return handler;
    }

    /**
     * Sets the handler that services intercepted requests. Use an
     * {@link LlmRequestHandler} for the idiomatic {@code java.net.http} view, or
     * any {@link LlmInferenceProvider} for full low-level control.
     *
     * @param handler
     *            the handler (must not be {@code null})
     * @return this instance for method chaining
     */
    public LlmInferenceConfig setHandler(LlmInferenceProvider handler) {
        this.handler = Objects.requireNonNull(handler, "handler must not be null");
        return this;
    }
}
