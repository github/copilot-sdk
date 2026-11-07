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


    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    public static final class Builder {
        private Builder() {}
        private String message;
        private String host;
        private Long timeUntilRetryMs;

        /**
         * Sets the {@code message} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder message(String value) {
            this.message = value;
            return this;
        }

        /**
         * Sets the {@code host} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder host(String value) {
            this.host = value;
            return this;
        }

        /**
         * Sets the {@code timeUntilRetryMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder timeUntilRetryMs(Long value) {
            this.timeUntilRetryMs = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public GitHubFetchResultCircuitBreaker build() {
            if (message == null) throw new IllegalStateException("Missing required property: message");
            if (host == null) throw new IllegalStateException("Missing required property: host");
            if (timeUntilRetryMs == null) throw new IllegalStateException("Missing required property: timeUntilRetryMs");
            GitHubFetchResultCircuitBreaker instance = new GitHubFetchResultCircuitBreaker();
            instance.setMessage(message);
            instance.setHost(host);
            instance.setTimeUntilRetryMs(timeUntilRetryMs);
            return instance;
        }
    }
}
