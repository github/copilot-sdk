/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The response head passed to {@link LlmInferenceResponseSink#start}.
 * <p>
 * Carries the HTTP status, an optional reason phrase, and multi-valued response
 * headers. For a WebSocket upgrade the status is {@code 101}.
 *
 * @since 1.0.0
 */
public final class LlmInferenceResponseInit {

    private int status;
    private String statusText;
    private Map<String, List<String>> headers = new LinkedHashMap<>();

    /**
     * Creates an empty response head.
     */
    public LlmInferenceResponseInit() {
    }

    /**
     * Creates a response head with the given status.
     *
     * @param status
     *            the HTTP status code
     */
    public LlmInferenceResponseInit(int status) {
        this.status = status;
    }

    /**
     * Gets the HTTP status code.
     *
     * @return the status code
     */
    public int getStatus() {
        return status;
    }

    /**
     * Sets the HTTP status code.
     *
     * @param status
     *            the status code
     * @return this instance for method chaining
     */
    public LlmInferenceResponseInit setStatus(int status) {
        this.status = status;
        return this;
    }

    /**
     * Gets the optional HTTP reason phrase.
     *
     * @return the reason phrase, or {@code null} if not set
     */
    public String getStatusText() {
        return statusText;
    }

    /**
     * Sets the optional HTTP reason phrase.
     *
     * @param statusText
     *            the reason phrase
     * @return this instance for method chaining
     */
    public LlmInferenceResponseInit setStatusText(String statusText) {
        this.statusText = statusText;
        return this;
    }

    /**
     * Gets the multi-valued response headers.
     *
     * @return the headers (never {@code null})
     */
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /**
     * Sets the multi-valued response headers.
     *
     * @param headers
     *            the headers
     * @return this instance for method chaining
     */
    public LlmInferenceResponseInit setHeaders(Map<String, List<String>> headers) {
        this.headers = headers != null ? headers : new LinkedHashMap<>();
        return this;
    }
}
