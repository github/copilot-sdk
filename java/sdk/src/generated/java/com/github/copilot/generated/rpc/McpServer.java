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
 * MCP server status entry, including config source/plugin source and any connection error.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record McpServer(
    /** Server name (config key) */
    @JsonProperty("name") String name,
    /** Connection status: connected, failed, needs-auth, pending, disabled, stopped, or not_configured */
    @JsonProperty("status") McpServerStatus status,
    /** Configuration source: user, workspace, plugin, builtin, or managed */
    @JsonProperty("source") McpServerSource source,
    /** Configured URL for an HTTP/SSE server, regardless of configuration source. Omitted for local and in-memory servers. */
    @JsonProperty("url") String url,
    /** Plugin name that provided this server, when source is plugin. */
    @JsonProperty("sourcePlugin") String sourcePlugin,
    /** Plugin version that provided this server, when source is plugin. */
    @JsonProperty("sourcePluginVersion") String sourcePluginVersion,
    /** Human-readable display name supplied by a managed server catalog. */
    @JsonProperty("displayName") String displayName,
    /** Error message if the server failed to connect */
    @JsonProperty("error") String error,
    /** Server-advertised metadata for a connected server. Omitted when no live connection metadata is available, including while pending or when failed, disabled, stopped, or not configured. */
    @JsonProperty("serverMetadata") McpServerMetadata serverMetadata,
    /** Owned installation this entry's live configuration came from. Absent for manual, workspace, plugin, builtin and same-name servers, and on runtimes without owned installations. */
    @JsonProperty("owned") McpServerOwnership owned
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpServer {}

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
        private String url;
        private String sourcePlugin;
        private String sourcePluginVersion;
        private String displayName;
        private String error;
        private McpServerMetadata serverMetadata;
        private McpServerOwnership owned;

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
         * Sets the {@code url} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder url(String value) {
            this.url = value;
            return this;
        }

        /**
         * Sets the {@code sourcePlugin} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sourcePlugin(String value) {
            this.sourcePlugin = value;
            return this;
        }

        /**
         * Sets the {@code sourcePluginVersion} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sourcePluginVersion(String value) {
            this.sourcePluginVersion = value;
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
         * Sets the {@code owned} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder owned(McpServerOwnership value) {
            this.owned = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public McpServer build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (status == null) throw new IllegalStateException("Missing required property: status");
            return new McpServer(name, status, source, url, sourcePlugin, sourcePluginVersion, displayName, error, serverMetadata, owned);
        }
    }

    /**
     * Creates a record with the components it had before later optional fields were added.
     * @deprecated Use named construction via {@link #builder()}.
     * @param name Server name (config key)
     * @param status Connection status: connected, failed, needs-auth, pending, disabled, stopped, or not_configured
     * @param source Configuration source: user, workspace, plugin, builtin, or managed
     * @param sourcePlugin Plugin name that provided this server, when source is plugin.
     * @param sourcePluginVersion Plugin version that provided this server, when source is plugin.
     * @param displayName Human-readable display name supplied by a managed server catalog.
     * @param error Error message if the server failed to connect
     * @param serverMetadata Server-advertised metadata for a connected server. Omitted when no live connection metadata is available, including while pending or when failed, disabled, stopped, or not configured.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public McpServer(
        String name,
        McpServerStatus status,
        McpServerSource source,
        String sourcePlugin,
        String sourcePluginVersion,
        String displayName,
        String error,
        McpServerMetadata serverMetadata
    ) {
        this(name, status, source, null, sourcePlugin, sourcePluginVersion, displayName, error, serverMetadata, null);
    }
}
