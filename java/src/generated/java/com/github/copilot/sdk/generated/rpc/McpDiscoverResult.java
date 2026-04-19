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
 * Result for the {@code mcp.discover} RPC method.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpDiscoverResult(
    /** MCP servers discovered from all sources */
    @JsonProperty("servers") List<McpDiscoverResultServersItem> servers
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record McpDiscoverResultServersItem(
        /** Server name (config key) */
        @JsonProperty("name") String name,
        /** Server type: local, stdio, http, or sse */
        @JsonProperty("type") String type,
        /** Configuration source */
        @JsonProperty("source") McpDiscoverResultServersItemSource source,
        /** Whether the server is enabled (not in the disabled list) */
        @JsonProperty("enabled") Boolean enabled
    ) {

        /** Configuration source */
        public enum McpDiscoverResultServersItemSource {
            /** The {@code user} variant. */
            USER("user"),
            /** The {@code workspace} variant. */
            WORKSPACE("workspace"),
            /** The {@code plugin} variant. */
            PLUGIN("plugin"),
            /** The {@code builtin} variant. */
            BUILTIN("builtin");

            private final String value;
            McpDiscoverResultServersItemSource(String value) { this.value = value; }
            @com.fasterxml.jackson.annotation.JsonValue
            public String getValue() { return value; }
            @com.fasterxml.jackson.annotation.JsonCreator
            public static McpDiscoverResultServersItemSource fromValue(String value) {
                for (McpDiscoverResultServersItemSource v : values()) {
                    if (v.value.equals(value)) return v;
                }
                throw new IllegalArgumentException("Unknown McpDiscoverResultServersItemSource value: " + value);
            }
        }
    }
}
