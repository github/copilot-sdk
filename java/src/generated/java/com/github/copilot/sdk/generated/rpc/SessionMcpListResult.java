/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Result for the {@code session.mcp.list} RPC method.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMcpListResult(
    /** Configured MCP servers */
    @JsonProperty("servers") List<SessionMcpListResultServersItem> servers
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionMcpListResultServersItem(
        /** Server name (config key) */
        @JsonProperty("name") String name,
        /** Connection status: connected, failed, needs-auth, pending, disabled, or not_configured */
        @JsonProperty("status") SessionMcpListResultServersItemStatus status,
        /** Configuration source: user, workspace, plugin, or builtin */
        @JsonProperty("source") String source,
        /** Error message if the server failed to connect */
        @JsonProperty("error") String error
    ) {

        /** Connection status: connected, failed, needs-auth, pending, disabled, or not_configured */
        public enum SessionMcpListResultServersItemStatus {
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
            SessionMcpListResultServersItemStatus(String value) { this.value = value; }
            @com.fasterxml.jackson.annotation.JsonValue
            public String getValue() { return value; }
            @com.fasterxml.jackson.annotation.JsonCreator
            public static SessionMcpListResultServersItemStatus fromValue(String value) {
                for (SessionMcpListResultServersItemStatus v : values()) {
                    if (v.value.equals(value)) return v;
                }
                throw new IllegalArgumentException("Unknown SessionMcpListResultServersItemStatus value: " + value);
            }
        }
    }
}
