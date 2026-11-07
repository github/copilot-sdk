/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Optional overrides for the runtime retry policy.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record GitHubFetchRetryConfig(
    /** Maximum retries; the runtime default is five. */
    @JsonProperty("maxRetries") Long maxRetries,
    /** Initial retry delay; the runtime default is five seconds. */
    @JsonProperty("defaultRetryDelaySeconds") Double defaultRetryDelaySeconds,
    /** Delay multiplier; the runtime default is two. */
    @JsonProperty("backoffFactor") Double backoffFactor,
    /** Whether repeated failures open the runtime circuit breaker. Defaults to true. */
    @JsonProperty("enableCircuitBreaker") Boolean enableCircuitBreaker
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    GitHubFetchRetryConfig {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static final class Builder {
        private Builder() {}
        private Long maxRetries;
        private Double defaultRetryDelaySeconds;
        private Double backoffFactor;
        private Boolean enableCircuitBreaker;

        /**
         * Sets the {@code maxRetries} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder maxRetries(Long value) {
            this.maxRetries = value;
            return this;
        }

        /**
         * Sets the {@code defaultRetryDelaySeconds} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder defaultRetryDelaySeconds(Double value) {
            this.defaultRetryDelaySeconds = value;
            return this;
        }

        /**
         * Sets the {@code backoffFactor} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder backoffFactor(Double value) {
            this.backoffFactor = value;
            return this;
        }

        /**
         * Sets the {@code enableCircuitBreaker} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder enableCircuitBreaker(Boolean value) {
            this.enableCircuitBreaker = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public GitHubFetchRetryConfig build() {
            return new GitHubFetchRetryConfig(maxRetries, defaultRetryDelaySeconds, backoffFactor, enableCircuitBreaker);
        }
    }
}
