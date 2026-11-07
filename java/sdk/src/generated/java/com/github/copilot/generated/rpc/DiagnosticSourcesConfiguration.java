/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Typed diagnostic source configuration. At least one source is required by diagnostics configuration methods.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DiagnosticSourcesConfiguration(
    /** MCP diagnostic capture threshold. Omit to leave the current threshold unchanged. */
    @JsonProperty("mcp") McpDiagnosticSourceConfiguration mcp
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public DiagnosticSourcesConfiguration {}

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
        private McpDiagnosticSourceConfiguration mcp;

        /**
         * Sets the {@code mcp} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder mcp(McpDiagnosticSourceConfiguration value) {
            this.mcp = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public DiagnosticSourcesConfiguration build() {
            return new DiagnosticSourcesConfiguration(mcp);
        }
    }
}
