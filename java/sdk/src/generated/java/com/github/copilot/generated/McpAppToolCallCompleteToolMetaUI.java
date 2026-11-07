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
 * MCP App tool `_meta.ui` resource URI and SEP-1865 visibility captured with an `mcp_app.tool_call_complete` result.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpAppToolCallCompleteToolMetaUI(
    /** `ui://` URI declared by the tool's `_meta.ui.resourceUri` */
    @JsonProperty("resourceUri") String resourceUri,
    /** Tool visibility per SEP-1865 (typically a subset of `["model","app"]`) */
    @JsonProperty("visibility") List<String> visibility
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpAppToolCallCompleteToolMetaUI {}

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
        private List<String> visibility;

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
        public Builder visibility(List<String> value) {
            this.visibility = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpAppToolCallCompleteToolMetaUI build() {
            return new McpAppToolCallCompleteToolMetaUI(resourceUri, visibility);
        }
    }
}
