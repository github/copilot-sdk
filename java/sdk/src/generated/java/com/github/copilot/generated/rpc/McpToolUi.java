/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Normalized MCP Apps discovery metadata from a tool's `_meta.ui` block.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpToolUi(
    /** URI of the tool's MCP App resource, typically a `ui://` resource identifier. Use `session.mcp.resources.read` to fetch its HTML and resource metadata. */
    @JsonProperty("resourceUri") String resourceUri,
    /** Tool visibility advertised by the server. When absent, MCP Apps defaults apply. */
    @JsonProperty("visibility") List<McpToolUiVisibility> visibility
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpToolUi {}

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
        private String resourceUri;
        private List<McpToolUiVisibility> visibility;

        /**
         * Sets the {@code resourceUri} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder resourceUri(String value) {
            this.resourceUri = value;
            return this;
        }

        /**
         * Sets the {@code visibility} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder visibility(List<McpToolUiVisibility> value) {
            this.visibility = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpToolUi build() {
            return new McpToolUi(resourceUri, visibility);
        }
    }
}
