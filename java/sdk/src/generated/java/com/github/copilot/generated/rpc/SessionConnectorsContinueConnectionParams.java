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
 * Explicitly bounded continuation of a pending Connector connection.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionConnectorsContinueConnectionParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Opaque continuation ID returned by connect, reconnect, or an earlier continuation. */
    @JsonProperty("continuationId") String continuationId,
    /** Maximum catalog requests made by this call. Must be between one and the capability limit. */
    @JsonProperty("maxAttempts") Long maxAttempts,
    /** Delay in milliseconds between attempts. Must not exceed the capability limit. */
    @JsonProperty("pollIntervalMs") Long pollIntervalMs,
    /** Maximum wall-clock duration in milliseconds for this call. Must be between one and the capability limit. */
    @JsonProperty("deadlineMs") Long deadlineMs
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionConnectorsContinueConnectionParams {}

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
        private String sessionId;
        private String continuationId;
        private Long maxAttempts;
        private Long pollIntervalMs;
        private Long deadlineMs;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code continuationId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder continuationId(String value) {
            this.continuationId = value;
            return this;
        }

        /**
         * Sets the {@code maxAttempts} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder maxAttempts(Long value) {
            this.maxAttempts = value;
            return this;
        }

        /**
         * Sets the {@code pollIntervalMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder pollIntervalMs(Long value) {
            this.pollIntervalMs = value;
            return this;
        }

        /**
         * Sets the {@code deadlineMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder deadlineMs(Long value) {
            this.deadlineMs = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionConnectorsContinueConnectionParams build() {
            if (continuationId == null) throw new IllegalStateException("Missing required property: continuationId");
            if (maxAttempts == null) throw new IllegalStateException("Missing required property: maxAttempts");
            if (pollIntervalMs == null) throw new IllegalStateException("Missing required property: pollIntervalMs");
            if (deadlineMs == null) throw new IllegalStateException("Missing required property: deadlineMs");
            return new SessionConnectorsContinueConnectionParams(sessionId, continuationId, maxAttempts, pollIntervalMs, deadlineMs);
        }
    }
}
