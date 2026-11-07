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
 * Session event "mcp.oauth_recovery_completed". Terminal outcome of a runtime-owned MCP OAuth recovery accepted through a runtimeRefresh handoff
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class McpOauthRecoveryCompletedEvent extends SessionEvent {

    @Override
    public String getType() { return "mcp.oauth_recovery_completed"; }

    @JsonProperty("data")
    private McpOauthRecoveryCompletedEventData data;

    public McpOauthRecoveryCompletedEventData getData() { return data; }
    public void setData(McpOauthRecoveryCompletedEventData data) { this.data = data; }

    /** Data payload for {@link McpOauthRecoveryCompletedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record McpOauthRecoveryCompletedEventData(
        /** Request ID of the MCP OAuth request whose runtimeRefresh handoff was accepted */
        @JsonProperty("requestId") String requestId,
        /** Name of the MCP server whose authorization recovery completed */
        @JsonProperty("serverName") String serverName,
        /** Terminal outcome of the original authorization reservation, including reconnect */
        @JsonProperty("outcome") McpOauthRecoveryCompletedOutcome outcome
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public McpOauthRecoveryCompletedEventData {}

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
            private String serverName;
            private McpOauthRecoveryCompletedOutcome outcome;

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
             * Sets the {@code serverName} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder serverName(String value) {
                this.serverName = value;
                return this;
            }

            /**
             * Sets the {@code outcome} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder outcome(McpOauthRecoveryCompletedOutcome value) {
                this.outcome = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public McpOauthRecoveryCompletedEventData build() {
                if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
                if (serverName == null) throw new IllegalStateException("Missing required property: serverName");
                if (outcome == null) throw new IllegalStateException("Missing required property: outcome");
                return new McpOauthRecoveryCompletedEventData(requestId, serverName, outcome);
            }
        }
    }
}
