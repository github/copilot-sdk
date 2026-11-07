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
 * Session event "session.mcp_server_status_changed". Payload of `session.mcp_server_status_changed` for one MCP server's status and optional failure error.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionMcpServerStatusChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.mcp_server_status_changed"; }

    @JsonProperty("data")
    private SessionMcpServerStatusChangedEventData data;

    public SessionMcpServerStatusChangedEventData getData() { return data; }
    public void setData(SessionMcpServerStatusChangedEventData data) { this.data = data; }

    /** Data payload for {@link SessionMcpServerStatusChangedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionMcpServerStatusChangedEventData(
        /** Name of the MCP server whose status changed */
        @JsonProperty("serverName") String serverName,
        /** Connection status: connected, failed, needs-auth, pending, disabled, stopped, or not_configured */
        @JsonProperty("status") McpServerStatus status,
        /** Error message if the server entered a failed state */
        @JsonProperty("error") String error,
        /** Runtime-produced classification for the final failed connection; unclassified means no classification was supplied. Additional string values may be introduced. */
        @JsonProperty("errorClassification") String errorClassification,
        /** Runtime configuration provenance for a connected or failed server, or unknown when unavailable. Additional string values may be introduced. */
        @JsonProperty("configSource") String configSource
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionMcpServerStatusChangedEventData {}

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
            private String serverName;
            private McpServerStatus status;
            private String error;
            private String errorClassification;
            private String configSource;

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
             * Sets the {@code status} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder status(McpServerStatus value) {
                this.status = value;
                return this;
            }

            /**
             * Sets the {@code error} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder error(String value) {
                this.error = value;
                return this;
            }

            /**
             * Sets the {@code errorClassification} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder errorClassification(String value) {
                this.errorClassification = value;
                return this;
            }

            /**
             * Sets the {@code configSource} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder configSource(String value) {
                this.configSource = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionMcpServerStatusChangedEventData build() {
                if (serverName == null) throw new IllegalStateException("Missing required property: serverName");
                if (status == null) throw new IllegalStateException("Missing required property: status");
                return new SessionMcpServerStatusChangedEventData(serverName, status, error, errorClassification, configSource);
            }
        }
    }
}
