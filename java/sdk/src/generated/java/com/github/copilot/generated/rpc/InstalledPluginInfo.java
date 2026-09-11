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
    /** Absolute path of the marketplace directory a live plugin was resolved from. Present only on live, never-persisted records — a plugin belonging to a directory/local marketplace, which is loaded from its real directory on every pass instead of a copy under the installed-plugins cache. Its presence is what marks a listed plugin as live: such a plugin is always present on disk, so `enabled` is its only meaningful state and it is never "not installed". */
    @JsonProperty("installedFrom") String installedFrom
) {
}
