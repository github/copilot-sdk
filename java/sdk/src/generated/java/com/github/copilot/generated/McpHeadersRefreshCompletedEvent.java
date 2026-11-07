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
 * Session event "mcp.headers_refresh_completed". MCP headers refresh request completion notification
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class McpHeadersRefreshCompletedEvent extends SessionEvent {

    @Override
    public String getType() { return "mcp.headers_refresh_completed"; }

    @JsonProperty("data")
    private McpHeadersRefreshCompletedEventData data;

    public McpHeadersRefreshCompletedEventData getData() { return data; }
    public void setData(McpHeadersRefreshCompletedEventData data) { this.data = data; }

    /** Data payload for {@link McpHeadersRefreshCompletedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record McpHeadersRefreshCompletedEventData(
        /** Request ID of the resolved headers refresh request */
        @JsonProperty("requestId") String requestId,
        /** How the pending MCP headers refresh request resolved. */
        @JsonProperty("outcome") McpHeadersRefreshCompletedOutcome outcome
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public McpHeadersRefreshCompletedEventData {}

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
            private McpHeadersRefreshCompletedOutcome outcome;

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
            public Builder outcome(McpHeadersRefreshCompletedOutcome value) {
                this.outcome = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public McpHeadersRefreshCompletedEventData build() {
                if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
                if (outcome == null) throw new IllegalStateException("Missing required property: outcome");
                return new McpHeadersRefreshCompletedEventData(requestId, outcome);
            }
        }
    }
}
