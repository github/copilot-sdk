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
 * Discovered extension metadata and persistent enablement state.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DiscoveredExtension(
    /** Source-qualified ID accepted by both server and session extension enablement methods */
    @JsonProperty("id") String id,
    /** Human-readable extension name */
    @JsonProperty("name") String name,
    /** Absolute path to the extension entry module, suitable for revealing it in a file manager */
    @JsonProperty("path") String path,
    /** Discovery source */
    @JsonProperty("source") DiscoveredExtensionSource source,
    /** Whether this extension's persistent per-ID preference is enabled */
    @JsonProperty("enabled") Boolean enabled,
    /** Containing plugin metadata for plugin-contributed extensions */
    @JsonProperty("plugin") DiscoveredExtensionPlugin plugin
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public DiscoveredExtension {}

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
        private String id;
        private String name;
        private String path;
        private DiscoveredExtensionSource source;
        private Boolean enabled;
        private DiscoveredExtensionPlugin plugin;

        /**
         * Sets the {@code id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder id(String value) {
            this.id = value;
            return this;
        }

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
         * Sets the {@code path} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder path(String value) {
            this.path = value;
            return this;
        }

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder source(DiscoveredExtensionSource value) {
            this.source = value;
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
         * Sets the {@code plugin} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder plugin(DiscoveredExtensionPlugin value) {
            this.plugin = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public DiscoveredExtension build() {
            if (id == null) throw new IllegalStateException("Missing required property: id");
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (path == null) throw new IllegalStateException("Missing required property: path");
            if (source == null) throw new IllegalStateException("Missing required property: source");
            if (enabled == null) throw new IllegalStateException("Missing required property: enabled");
            return new DiscoveredExtension(id, name, path, source, enabled, plugin);
        }
    }
}
