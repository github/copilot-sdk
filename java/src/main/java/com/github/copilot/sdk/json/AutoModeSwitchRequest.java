/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request to switch to auto mode after an eligible rate limit.
 * <p>
 * This is sent by the server when the agent encounters a rate limit and wants
 * to switch to an alternative model automatically.
 *
 * @since 1.0.8
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AutoModeSwitchRequest {

    @JsonProperty("errorCode")
    private String errorCode;

    @JsonProperty("retryAfterSeconds")
    private Double retryAfterSeconds;

    /**
     * Gets the rate-limit error code that triggered the request.
     *
     * @return the error code, or {@code null}
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Sets the rate-limit error code.
     *
     * @param errorCode
     *            the error code
     * @return this instance for method chaining
     */
    public AutoModeSwitchRequest setErrorCode(String errorCode) {
        this.errorCode = errorCode;
        return this;
    }

    /**
     * Gets the seconds until the rate limit resets, when known.
     *
     * @return the retry-after seconds, or {@code null}
     */
    public Double getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    /**
     * Sets the seconds until the rate limit resets.
     *
     * @param retryAfterSeconds
     *            the retry-after seconds
     * @return this instance for method chaining
     */
    public AutoModeSwitchRequest setRetryAfterSeconds(Double retryAfterSeconds) {
        this.retryAfterSeconds = retryAfterSeconds;
        return this;
    }
}
