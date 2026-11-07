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
 * MCP Apps tool `_meta.ui` resource URI and visibility captured on `tool.execution_complete`.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolExecutionCompleteToolDescriptionMetaUI(
    /** URI of the UI resource */
    @JsonProperty("resourceUri") String resourceUri,
    /** Who can access this tool */
    @JsonProperty("visibility") List<ToolExecutionCompleteToolDescriptionMetaUIVisibility> visibility
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ToolExecutionCompleteToolDescriptionMetaUI {}

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
        private List<ToolExecutionCompleteToolDescriptionMetaUIVisibility> visibility;

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
        public Builder visibility(List<ToolExecutionCompleteToolDescriptionMetaUIVisibility> value) {
            this.visibility = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ToolExecutionCompleteToolDescriptionMetaUI build() {
            return new ToolExecutionCompleteToolDescriptionMetaUI(resourceUri, visibility);
        }
    }
}
