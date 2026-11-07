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
     * Creates a record with the components it had before later optional fields were added.
     *
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
