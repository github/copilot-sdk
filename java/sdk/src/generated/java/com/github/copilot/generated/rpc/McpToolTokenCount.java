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
import javax.annotation.processing.Generated;

/**
 * Context tokens one MCP tool definition costs.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpToolTokenCount(
    /** Tool name as the model sees it, copied verbatim from the session's tool metadata. Usually namespaced with the server, but that is not guaranteed: use `serverName` rather than parsing this name. */
    @JsonProperty("name") String name,
    /** Tool name as the MCP server reports it. */
    @JsonProperty("mcpToolName") String mcpToolName,
    /** Share of the server's total tokens attributed to this tool; 0 when deferred. The tools of one server sum to that server's `totalTokens`. */
    @JsonProperty("tokens") Long tokens,
    /** True when the tool definition is deferred and not sent to the model up front. */
    @JsonProperty("deferred") Boolean deferred
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpToolTokenCount {}

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
        private String name;
        private String mcpToolName;
        private Long tokens;
        private Boolean deferred;

        /**
         * Sets the {@code name} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder name(String value) {
            this.name = value;
            return this;
        }

        /**
         * Sets the {@code mcpToolName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder mcpToolName(String value) {
            this.mcpToolName = value;
            return this;
        }

        /**
         * Sets the {@code tokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder tokens(Long value) {
            this.tokens = value;
            return this;
        }

        /**
         * Sets the {@code deferred} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder deferred(Boolean value) {
            this.deferred = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpToolTokenCount build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (mcpToolName == null) throw new IllegalStateException("Missing required property: mcpToolName");
            if (tokens == null) throw new IllegalStateException("Missing required property: tokens");
            if (deferred == null) throw new IllegalStateException("Missing required property: deferred");
            return new McpToolTokenCount(name, mcpToolName, tokens, deferred);
        }
    }
}
