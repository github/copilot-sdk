/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Variant {@code circuit-breaker} of {@link GitHubFetchResult}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class GitHubFetchResultCircuitBreaker extends GitHubFetchResult {

    @JsonProperty("kind")
    private final String kind = "circuit-breaker";

    @Override
    public String getKind() { return kind; }

    /** Complete circuit-breaker message. */
    @JsonProperty("message")
    private String message;

    /** Host whose breaker is open. */
    @JsonProperty("host")
    private String host;

    /** Remaining backoff in milliseconds. */
    @JsonProperty("timeUntilRetryMs")
    private Long timeUntilRetryMs;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public Long getTimeUntilRetryMs() { return timeUntilRetryMs; }
    public void setTimeUntilRetryMs(Long timeUntilRetryMs) { this.timeUntilRetryMs = timeUntilRetryMs; }
}
