/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request for a legacy Auto switch or eligible active-request recovery.
 *
 * @since 1.0.8
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AutoModeSwitchRequest {

    @JsonProperty("errorCode")
    private String errorCode;

    @JsonProperty("retryAfterSeconds")
    private Double retryAfterSeconds;

    @JsonProperty("autoModeRecovery")
    private AutoModeRecoveryContext autoModeRecovery;

    /**
     * Gets the context for active-request recovery.
     *
     * @return the context, or {@code null} for a legacy future-message switch
     */
    public AutoModeRecoveryContext getAutoModeRecovery() {
        return autoModeRecovery;
    }

    /**
     * Sets the active-request recovery context.
     *
     * @param autoModeRecovery
     *            the context, or {@code null}
     * @return this instance for method chaining
     */
    public AutoModeSwitchRequest setAutoModeRecovery(AutoModeRecoveryContext autoModeRecovery) {
        this.autoModeRecovery = autoModeRecovery;
        return this;
    }

    /**
     * Gets the upstream error code that triggered the request.
     *
     * @return the error code, or {@code null}
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Sets the upstream error code.
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
