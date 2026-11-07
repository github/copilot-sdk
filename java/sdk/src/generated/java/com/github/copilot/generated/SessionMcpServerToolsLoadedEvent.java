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
 * Session event "session.mcp_server_tools_loaded". Payload of `session.mcp_server_tools_loaded`, emitted once per connection when an MCP server's tools first become available from tools/list or a cached snapshot. A connected server is still loading tools if this event has not fired since the server last left connected; it can precede connected when the tools come from a cached snapshot.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionMcpServerToolsLoadedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.mcp_server_tools_loaded"; }

    @JsonProperty("data")
    private SessionMcpServerToolsLoadedEventData data;

    public SessionMcpServerToolsLoadedEventData getData() { return data; }
    public void setData(SessionMcpServerToolsLoadedEventData data) { this.data = data; }

    /** Data payload for {@link SessionMcpServerToolsLoadedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionMcpServerToolsLoadedEventData(
        /** Name of the MCP server whose tools loaded */
        @JsonProperty("serverName") String serverName,
        /** Number of tools the server listed */
        @JsonProperty("toolCount") Long toolCount
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionMcpServerToolsLoadedEventData {}

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
            private Long toolCount;

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
             * Sets the {@code toolCount} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolCount(Long value) {
                this.toolCount = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionMcpServerToolsLoadedEventData build() {
                if (serverName == null) throw new IllegalStateException("Missing required property: serverName");
                if (toolCount == null) throw new IllegalStateException("Missing required property: toolCount");
                return new SessionMcpServerToolsLoadedEventData(serverName, toolCount);
            }
        }
    }
}
