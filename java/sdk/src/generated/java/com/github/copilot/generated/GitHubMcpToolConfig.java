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
 * Per-session configuration for the built-in GitHub MCP server
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubMcpToolConfig(
    /** Whether to use the read-write endpoint and request all toolsets */
    @JsonProperty("enableAllTools") Boolean enableAllTools,
    /** Additional GitHub MCP toolsets requested by the session */
    @JsonProperty("additionalToolsets") List<String> additionalToolsets,
    /** Additional GitHub MCP tools requested by the session */
    @JsonProperty("additionalTools") List<String> additionalTools,
    /** Whether to request the GitHub MCP insiders build */
    @JsonProperty("enableInsidersMode") Boolean enableInsidersMode
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public GitHubMcpToolConfig {}

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
        private Boolean enableAllTools;
        private List<String> additionalToolsets;
        private List<String> additionalTools;
        private Boolean enableInsidersMode;

        /**
         * Sets the {@code enableAllTools} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder enableAllTools(Boolean value) {
            this.enableAllTools = value;
            return this;
        }

        /**
         * Sets the {@code additionalToolsets} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder additionalToolsets(List<String> value) {
            this.additionalToolsets = value;
            return this;
        }

        /**
         * Sets the {@code additionalTools} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder additionalTools(List<String> value) {
            this.additionalTools = value;
            return this;
        }

        /**
         * Sets the {@code enableInsidersMode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder enableInsidersMode(Boolean value) {
            this.enableInsidersMode = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public GitHubMcpToolConfig build() {
            return new GitHubMcpToolConfig(enableAllTools, additionalToolsets, additionalTools, enableInsidersMode);
        }
    }
}
