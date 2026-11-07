/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Session event "mcp.oauth_completed". MCP OAuth request completion notification
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class McpOauthCompletedEvent extends SessionEvent {

    @Override
    public String getType() { return "mcp.oauth_completed"; }

    @JsonProperty("data")
    private McpOauthCompletedEventData data;

    public McpOauthCompletedEventData getData() { return data; }
    public void setData(McpOauthCompletedEventData data) { this.data = data; }

    /** Data payload for {@link McpOauthCompletedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record McpOauthCompletedEventData(
        /** Request ID of the resolved OAuth request */
        @JsonProperty("requestId") String requestId,
        /** How the pending OAuth request was completed. An accepted runtimeRefresh handoff completes once at its authorization terminal: token when recovery succeeded, cancelled when it was cancelled or failed; see mcp.oauth_recovery_completed for the exact outcome. */
        @JsonProperty("outcome") McpOauthCompletionOutcome outcome
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public McpOauthCompletedEventData {}

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
            private String requestId;
            private McpOauthCompletionOutcome outcome;

            /**
             * Sets the {@code requestId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder requestId(String value) {
                this.requestId = value;
                return this;
            }

            /**
             * Sets the {@code outcome} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder outcome(McpOauthCompletionOutcome value) {
                this.outcome = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public McpOauthCompletedEventData build() {
                if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
                if (outcome == null) throw new IllegalStateException("Missing required property: outcome");
                return new McpOauthCompletedEventData(requestId, outcome);
            }
        }
    }
}
