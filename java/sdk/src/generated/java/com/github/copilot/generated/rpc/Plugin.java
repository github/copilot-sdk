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
 * Session plugin metadata, with name, marketplace, optional version, and enabled state.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record Plugin(
    /** Soft assignment from effective managed settings. This does not mark the plugin as hard-managed. */
    @JsonProperty("assignment") ManagedPluginAssignment assignment,
    /** Plugin name */
    @JsonProperty("name") String name,
    /** Marketplace the plugin came from */
    @JsonProperty("marketplace") String marketplace,
    /** Installed version */
    @JsonProperty("version") String version,
    /** Whether the plugin is currently enabled */
    @JsonProperty("enabled") Boolean enabled,
    /** Opaque stable identity for a direct plugin source. */
    @JsonProperty("directSourceId") String directSourceId,
    /** Absolute local directory containing this plugin's resolved content, including disabled installations. Absent when no content directory is resolved. This is the plugin root, not its marketplace directory; clients must not forward it back as an additional plugin. */
    @JsonProperty("rootPath") String rootPath,
    /** Absolute marketplace directory for a live plugin. */
    @JsonProperty("installedFrom") String installedFrom,
    /** Runtime plugin provenance, such as "builtin". */
    @JsonProperty("source") String source,
    /** Whether enterprise managed settings control this plugin. */
    @JsonProperty("managed") Boolean managed,
    /** Enabled state required by enterprise managed settings. */
    @JsonProperty("managedDesiredEnabled") Boolean managedDesiredEnabled,
    /** Whether this managed desired plugin has an installed or live record. */
    @JsonProperty("installed") Boolean installed,
    /** Opaque plugin identity in this runtime's inventory. Matches global inventory id and MCP effectiveSource.plugin.id for the same plugin. Stable across enablement changes, updates and restarts while the source is unchanged. Session-only plugin identities are scoped to their session; not a cross-host identifier or a mutation argument. Compare the complete value without parsing it. May be absent on older runtimes. */
    @JsonProperty("id") String id
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public Plugin {}

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
        private ManagedPluginAssignment assignment;
        private String name;
        private String marketplace;
        private String version;
        private Boolean enabled;
        private String directSourceId;
        private String rootPath;
        private String installedFrom;
        private String source;
        private Boolean managed;
        private Boolean managedDesiredEnabled;
        private Boolean installed;
        private String id;

        /**
         * Sets the {@code assignment} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder assignment(ManagedPluginAssignment value) {
            this.assignment = value;
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
         * Sets the {@code marketplace} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder marketplace(String value) {
            this.marketplace = value;
            return this;
        }

        /**
         * Sets the {@code version} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder version(String value) {
            this.version = value;
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
         * Sets the {@code directSourceId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder directSourceId(String value) {
            this.directSourceId = value;
            return this;
        }

        /**
         * Sets the {@code rootPath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder rootPath(String value) {
            this.rootPath = value;
            return this;
        }

        /**
         * Sets the {@code installedFrom} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder installedFrom(String value) {
            this.installedFrom = value;
            return this;
        }

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder source(String value) {
            this.source = value;
            return this;
        }

        /**
         * Sets the {@code managed} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder managed(Boolean value) {
            this.managed = value;
            return this;
        }

        /**
         * Sets the {@code managedDesiredEnabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder managedDesiredEnabled(Boolean value) {
            this.managedDesiredEnabled = value;
            return this;
        }

        /**
         * Sets the {@code installed} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder installed(Boolean value) {
            this.installed = value;
            return this;
        }

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
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public Plugin build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (marketplace == null) throw new IllegalStateException("Missing required property: marketplace");
            if (enabled == null) throw new IllegalStateException("Missing required property: enabled");
            return new Plugin(assignment, name, marketplace, version, enabled, directSourceId, rootPath, installedFrom, source, managed, managedDesiredEnabled, installed, id);
        }
    }

    /**
     * Creates a record with the components it had before later optional fields were added.
     * @deprecated Use named construction via {@link #builder()}.
     * @param name Plugin name
     * @param marketplace Marketplace the plugin came from
     * @param version Installed version
     * @param enabled Whether the plugin is currently enabled
     * @param directSourceId Opaque stable identity for a direct plugin source.
     * @param installedFrom Absolute marketplace directory for a live plugin.
     * @param source Runtime plugin provenance, such as "builtin".
     * @param managed Whether enterprise managed settings control this plugin.
     * @param managedDesiredEnabled Enabled state required by enterprise managed settings.
     * @param installed Whether this managed desired plugin has an installed or live record.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public Plugin(
        String name,
        String marketplace,
        String version,
        Boolean enabled,
        String directSourceId,
        String installedFrom,
        String source,
        Boolean managed,
        Boolean managedDesiredEnabled,
        Boolean installed
    ) {
        this(null, name, marketplace, version, enabled, directSourceId, null, installedFrom, source, managed, managedDesiredEnabled, installed, null);
    }
}
