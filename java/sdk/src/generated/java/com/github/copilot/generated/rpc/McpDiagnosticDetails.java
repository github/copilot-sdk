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
 * MCP-specific detail for a source-discriminated diagnostic entry.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpDiagnosticDetails(
    /** Diagnostic record category. */
    @JsonProperty("kind") McpDiagnosticKind kind,
    /** Configured MCP server name. */
    @JsonProperty("serverName") String serverName,
    /** Fresh identifier for the MCP connection attempt, including failed starts. */
    @JsonProperty("connectionId") String connectionId,
    /** Protocol-frame direction when kind is protocol. */
    @JsonProperty("direction") McpDiagnosticDirection direction,
    /** Serialized diagnostic detail. Protocol and HTTP records use JSON when detail is present. */
    @JsonProperty("data") String data
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpDiagnosticDetails {}

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
        private McpDiagnosticKind kind;
        private String serverName;
        private String connectionId;
        private McpDiagnosticDirection direction;
        private String data;

        /**
         * Sets the {@code kind} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder kind(McpDiagnosticKind value) {
            this.kind = value;
            return this;
        }

        /**
         * Sets the {@code serverName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder serverName(String value) {
            this.serverName = value;
            return this;
        }

        /**
         * Sets the {@code connectionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder connectionId(String value) {
            this.connectionId = value;
            return this;
        }

        /**
         * Sets the {@code direction} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder direction(McpDiagnosticDirection value) {
            this.direction = value;
            return this;
        }

        /**
         * Sets the {@code data} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder data(String value) {
            this.data = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpDiagnosticDetails build() {
            if (kind == null) throw new IllegalStateException("Missing required property: kind");
            if (serverName == null) throw new IllegalStateException("Missing required property: serverName");
            if (connectionId == null) throw new IllegalStateException("Missing required property: connectionId");
            return new McpDiagnosticDetails(kind, serverName, connectionId, direction, data);
        }
    }
}
