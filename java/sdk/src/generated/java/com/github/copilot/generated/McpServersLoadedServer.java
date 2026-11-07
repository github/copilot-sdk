/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * A single MCP server status summary in `session.mcp_servers_loaded`, including name, status, source, transport, and plugin metadata.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpServersLoadedServer(
    /** Server name (config key) */
    @JsonProperty("name") String name,
    /** Connection status: connected, failed, needs-auth, pending, disabled, stopped, or not_configured */
    @JsonProperty("status") McpServerStatus status,
    /** Configuration source: user, workspace, plugin, builtin, or managed */
    @JsonProperty("source") McpServerSource source,
    /** Human-readable display name supplied by a managed server catalog. */
    @JsonProperty("displayName") String displayName,
    /** Error message if the server failed to connect */
    @JsonProperty("error") String error,
    /** Server-advertised metadata for a connected server. Omitted when no live connection metadata is available, including while pending or when failed, disabled, stopped, or not configured. */
    @JsonProperty("serverMetadata") McpServerMetadata serverMetadata,
    /** Transport mechanism: stdio, http, sse (deprecated), or memory (in-process MCP server) */
    @JsonProperty("transport") McpServerTransport transport,
    /** Name of the plugin that supplied the effective MCP server config, only when source is plugin */
    @JsonProperty("pluginName") String pluginName,
    /** Version of the plugin that supplied the effective MCP server config, only when source is plugin */
    @JsonProperty("pluginVersion") String pluginVersion
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpServersLoadedServer {}

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
        private String name;
        private McpServerStatus status;
        private McpServerSource source;
        private String displayName;
        private String error;
        private McpServerMetadata serverMetadata;
        private McpServerTransport transport;
        private String pluginName;
        private String pluginVersion;

        /**
         * Sets the {@code name} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder name(String value) {
            this.name = value;
            return this;
        }

        /**
         * Sets the {@code status} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder status(McpServerStatus value) {
            this.status = value;
            return this;
        }

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder source(McpServerSource value) {
            this.source = value;
            return this;
        }

        /**
         * Sets the {@code displayName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder displayName(String value) {
            this.displayName = value;
            return this;
        }

        /**
         * Sets the {@code error} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder error(String value) {
            this.error = value;
            return this;
        }

        /**
         * Sets the {@code serverMetadata} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder serverMetadata(McpServerMetadata value) {
            this.serverMetadata = value;
            return this;
        }

        /**
         * Sets the {@code transport} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder transport(McpServerTransport value) {
            this.transport = value;
            return this;
        }

        /**
         * Sets the {@code pluginName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder pluginName(String value) {
            this.pluginName = value;
            return this;
        }

        /**
         * Sets the {@code pluginVersion} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder pluginVersion(String value) {
            this.pluginVersion = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpServersLoadedServer build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (status == null) throw new IllegalStateException("Missing required property: status");
            return new McpServersLoadedServer(name, status, source, displayName, error, serverMetadata, transport, pluginName, pluginVersion);
        }
    }
}
