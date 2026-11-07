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
 * MCP server discovered by `mcp.discover`, with config source, optional plugin source, transport type, and enabled state.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DiscoveredMcpServer(
    /** Server name (config key) */
    @JsonProperty("name") String name,
    /** Server transport type: stdio, http, sse (deprecated), or memory */
    @JsonProperty("type") DiscoveredMcpServerType type,
    /** Configuration source: user, workspace, plugin, or builtin */
    @JsonProperty("source") McpServerSource source,
    /** Plugin name that provided this server, when source is plugin. */
    @JsonProperty("sourcePlugin") String sourcePlugin,
    /** Plugin version that provided this server, when source is plugin. */
    @JsonProperty("sourcePluginVersion") String sourcePluginVersion,
    /** Canonical identity and location of the effective server declaration. */
    @JsonProperty("effectiveSource") McpSourceRef effectiveSource,
    /** Whether the server is enabled (not in the disabled list) */
    @JsonProperty("enabled") Boolean enabled
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public DiscoveredMcpServer {}

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
        private DiscoveredMcpServerType type;
        private McpServerSource source;
        private String sourcePlugin;
        private String sourcePluginVersion;
        private McpSourceRef effectiveSource;
        private Boolean enabled;

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
         * Sets the {@code type} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder type(DiscoveredMcpServerType value) {
            this.type = value;
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
         * Sets the {@code effectiveSource} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder effectiveSource(McpSourceRef value) {
            this.effectiveSource = value;
            return this;
        }

        /**
         * Sets the {@code enabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder enabled(Boolean value) {
            this.enabled = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public DiscoveredMcpServer build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (source == null) throw new IllegalStateException("Missing required property: source");
            if (enabled == null) throw new IllegalStateException("Missing required property: enabled");
            return new DiscoveredMcpServer(name, type, source, sourcePlugin, sourcePluginVersion, effectiveSource, enabled);
        }
    }
}
