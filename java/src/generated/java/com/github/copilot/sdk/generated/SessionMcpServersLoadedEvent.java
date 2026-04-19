/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * The {@code session.mcp_servers_loaded} session event.
 *
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
        @JsonProperty("servers") List<SessionMcpServersLoadedEventDataServersItem> servers
    ) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record SessionMcpServersLoadedEventDataServersItem(
            /** Server name (config key) */
            @JsonProperty("name") String name,
            /** Connection status: connected, failed, needs-auth, pending, disabled, or not_configured */
            @JsonProperty("status") SessionMcpServersLoadedEventDataServersItemStatus status,
            /** Configuration source: user, workspace, plugin, or builtin */
            @JsonProperty("source") String source,
            /** Error message if the server failed to connect */
            @JsonProperty("error") String error
        ) {

            /** Connection status: connected, failed, needs-auth, pending, disabled, or not_configured */
            public enum SessionMcpServersLoadedEventDataServersItemStatus {
                /** The {@code connected} variant. */
                CONNECTED("connected"),
                /** The {@code failed} variant. */
                FAILED("failed"),
                /** The {@code needs-auth} variant. */
                NEEDS_AUTH("needs-auth"),
                /** The {@code pending} variant. */
                PENDING("pending"),
                /** The {@code disabled} variant. */
                DISABLED("disabled"),
                /** The {@code not_configured} variant. */
                NOT_CONFIGURED("not_configured");

                private final String value;
                SessionMcpServersLoadedEventDataServersItemStatus(String value) { this.value = value; }
                @com.fasterxml.jackson.annotation.JsonValue
                public String getValue() { return value; }
                @com.fasterxml.jackson.annotation.JsonCreator
                public static SessionMcpServersLoadedEventDataServersItemStatus fromValue(String value) {
                    for (SessionMcpServersLoadedEventDataServersItemStatus v : values()) {
                        if (v.value.equals(value)) return v;
                    }
                    throw new IllegalArgumentException("Unknown SessionMcpServersLoadedEventDataServersItemStatus value: " + value);
                }
            }
        }
    }
}
