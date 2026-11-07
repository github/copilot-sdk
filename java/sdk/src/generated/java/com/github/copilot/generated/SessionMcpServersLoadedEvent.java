/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Session event "session.mcp_servers_loaded". Payload of `session.mcp_servers_loaded` listing MCP server status summaries.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionMcpServersLoadedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.mcp_servers_loaded"; }

    @JsonProperty("data")
    private SessionMcpServersLoadedEventData data;

    public SessionMcpServersLoadedEventData getData() { return data; }
    public void setData(SessionMcpServersLoadedEventData data) { this.data = data; }

    /** Data payload for {@link SessionMcpServersLoadedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionMcpServersLoadedEventData(
        /** Array of MCP server status summaries */
        @JsonProperty("servers") List<McpServersLoadedServer> servers
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionMcpServersLoadedEventData {}

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
            private List<McpServersLoadedServer> servers;

            /**
             * Sets the {@code servers} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder servers(List<McpServersLoadedServer> value) {
                this.servers = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionMcpServersLoadedEventData build() {
                if (servers == null) throw new IllegalStateException("Missing required property: servers");
                return new SessionMcpServersLoadedEventData(servers);
            }
        }
    }
}
