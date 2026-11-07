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
 * Information about an installed plugin tracked in global state.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record InstalledPluginInfo(
    /** Soft assignment from effective managed settings. This is not a mandatory enabled-state restriction. */
    @JsonProperty("assignment") ManagedPluginAssignment assignment,
    /** Plugin name */
    @JsonProperty("name") String name,
    /** Marketplace the plugin came from. Empty string ("") for direct repo / URL / local installs. */
    @JsonProperty("marketplace") String marketplace,
    /** Opaque, stable hash identifying a direct (non-marketplace) install source. Present only for direct repo / URL / local installs; absent for marketplace plugins. Same source yields the same id; distinct sources never collide. */
    @JsonProperty("directSourceId") String directSourceId,
    /** Installed version (when reported by the plugin manifest) */
    @JsonProperty("version") String version,
    /** Whether the plugin is currently enabled for new sessions */
    @JsonProperty("enabled") Boolean enabled,
    /** Absolute local directory containing this plugin's installed content, including disabled installations. Absent when the runtime has no resolved content directory. This is the plugin root, not its marketplace directory; clients must not forward it back as an additional plugin. */
    @JsonProperty("rootPath") String rootPath,
    /** Absolute path of the marketplace directory a live plugin was resolved from. Present only on live, never-persisted records — a plugin belonging to a directory/local marketplace, which is loaded from its real directory on every pass instead of a copy under the installed-plugins cache. Its presence is what marks a listed plugin as live: such a plugin is always present on disk, so `enabled` is its only meaningful state and it is never "not installed". */
    @JsonProperty("installedFrom") String installedFrom,
    /** Runtime-reported plugin provenance. Currently set to "builtin" only for plugins registered through the trusted host built-in boundary; absent for installed, marketplace, direct, and live plugins. */
    @JsonProperty("source") String source,
    /** Whether enterprise managed settings control this plugin's enabled state. */
    @JsonProperty("managed") Boolean managed,
    /** The enabled state required by enterprise managed settings, when this plugin spec is managed. */
    @JsonProperty("managedDesiredEnabled") Boolean managedDesiredEnabled,
    /** Whether the managed desired plugin currently has an installed or live record. Set to false for a managed desired entry retained in the listing after installation or reconciliation failed. */
    @JsonProperty("installed") Boolean installed,
    /** Description from the manifest of the installed copy. Present only for a direct install pinned to one GitHub commit, such as a verified install from GitHub. */
    @JsonProperty("description") String description,
    /** The GitHub repository and commit a direct install is pinned to, from its install record. Present only for a direct install pinned to one GitHub commit, such as a verified install from GitHub; absent for marketplace, local and unpinned installs. */
    @JsonProperty("verifiedSource") InstalledPluginVerifiedSource verifiedSource,
    /** HTTPS URL of the plugin's icon on raw.githubusercontent.com, retained from the catalogue entry it was installed from. Untrusted external content: load it without credentials. Present only for a direct install pinned to one GitHub commit whose catalogue entry had an icon. */
    @JsonProperty("iconUrl") String iconUrl,
    /** Opaque plugin identity in this runtime's inventory. Matches MCP effectiveSource.plugin.id for the same plugin. Stable across enablement changes, updates and restarts while the installation source is unchanged; not a cross-host identifier or a mutation argument. Compare the complete value without parsing it. May be absent on older runtimes. */
    @JsonProperty("id") String id
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public InstalledPluginInfo {}

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
        private String directSourceId;
        private String version;
        private Boolean enabled;
        private String rootPath;
        private String installedFrom;
        private String source;
        private Boolean managed;
        private Boolean managedDesiredEnabled;
        private Boolean installed;
        private String description;
        private InstalledPluginVerifiedSource verifiedSource;
        private String iconUrl;
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
         * Sets the {@code description} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder description(String value) {
            this.description = value;
            return this;
        }

        /**
         * Sets the {@code verifiedSource} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder verifiedSource(InstalledPluginVerifiedSource value) {
            this.verifiedSource = value;
            return this;
        }

        /**
         * Sets the {@code iconUrl} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder iconUrl(String value) {
            this.iconUrl = value;
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
        public InstalledPluginInfo build() {
            if (name == null) throw new IllegalStateException("Missing required property: name");
            if (marketplace == null) throw new IllegalStateException("Missing required property: marketplace");
            if (enabled == null) throw new IllegalStateException("Missing required property: enabled");
            return new InstalledPluginInfo(assignment, name, marketplace, directSourceId, version, enabled, rootPath, installedFrom, source, managed, managedDesiredEnabled, installed, description, verifiedSource, iconUrl, id);
        }
    }

    /**
     * Creates a record with the components it had before later optional fields were added.
     * @deprecated Use named construction via {@link #builder()}.
     * @param name Plugin name
     * @param marketplace Marketplace the plugin came from. Empty string ("") for direct repo / URL / local installs.
     * @param directSourceId Opaque, stable hash identifying a direct (non-marketplace) install source. Present only for direct repo / URL / local installs; absent for marketplace plugins. Same source yields the same id; distinct sources never collide.
     * @param version Installed version (when reported by the plugin manifest)
     * @param enabled Whether the plugin is currently enabled for new sessions
     * @param installedFrom Absolute path of the marketplace directory a live plugin was resolved from. Present only on live, never-persisted records — a plugin belonging to a directory/local marketplace, which is loaded from its real directory on every pass instead of a copy under the installed-plugins cache. Its presence is what marks a listed plugin as live: such a plugin is always present on disk, so `enabled` is its only meaningful state and it is never "not installed".
     * @param source Runtime-reported plugin provenance. Currently set to "builtin" only for plugins registered through the trusted host built-in boundary; absent for installed, marketplace, direct, and live plugins.
     * @param managed Whether enterprise managed settings control this plugin's enabled state.
     * @param managedDesiredEnabled The enabled state required by enterprise managed settings, when this plugin spec is managed.
     * @param installed Whether the managed desired plugin currently has an installed or live record. Set to false for a managed desired entry retained in the listing after installation or reconciliation failed.
     * @param description Description from the manifest of the installed copy. Present only for a direct install pinned to one GitHub commit, such as a verified install from GitHub.
     * @param verifiedSource The GitHub repository and commit a direct install is pinned to, from its install record. Present only for a direct install pinned to one GitHub commit, such as a verified install from GitHub; absent for marketplace, local and unpinned installs.
     * @param iconUrl HTTPS URL of the plugin's icon on raw.githubusercontent.com, retained from the catalogue entry it was installed from. Untrusted external content: load it without credentials. Present only for a direct install pinned to one GitHub commit whose catalogue entry had an icon.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public InstalledPluginInfo(
        String name,
        String marketplace,
        String directSourceId,
        String version,
        Boolean enabled,
        String installedFrom,
        String source,
        Boolean managed,
        Boolean managedDesiredEnabled,
        Boolean installed,
        String description,
        InstalledPluginVerifiedSource verifiedSource,
        String iconUrl
    ) {
        this(null, name, marketplace, directSourceId, version, enabled, null, installedFrom, source, managed, managedDesiredEnabled, installed, description, verifiedSource, iconUrl, null);
    }
}
