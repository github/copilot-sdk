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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Context tokens one MCP server's tool definitions cost.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpServerToolTokenBreakdown(
    /** Server name the tools are namespaced under. */
    @JsonProperty("serverName") String serverName,
    /** Tokens the server's non-deferred tool definitions cost together for the model the breakdown was counted with. */
    @JsonProperty("totalTokens") Long totalTokens,
    /** Per-tool breakdown, in the session's tool order. */
    @JsonProperty("tools") List<McpToolTokenCount> tools
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpServerToolTokenBreakdown {}

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
        private String serverName;
        private Long totalTokens;
        private List<McpToolTokenCount> tools;

        /**
         * Sets the {@code serverName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder serverName(String value) {
            this.serverName = value;
            return this;
        }

        /**
         * Sets the {@code totalTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder totalTokens(Long value) {
            this.totalTokens = value;
            return this;
        }

        /**
         * Sets the {@code tools} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder tools(List<McpToolTokenCount> value) {
            this.tools = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpServerToolTokenBreakdown build() {
            if (serverName == null) throw new IllegalStateException("Missing required property: serverName");
            if (totalTokens == null) throw new IllegalStateException("Missing required property: totalTokens");
            if (tools == null) throw new IllegalStateException("Missing required property: tools");
            return new McpServerToolTokenBreakdown(serverName, totalTokens, tools);
        }
    }
}
