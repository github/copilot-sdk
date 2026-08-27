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
 * Installed plugin record for a session, with marketplace, version, install time, enabled state, cache path, and source.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionInstalledPlugin(
    /** Plugin name */
    @JsonProperty("name") String name,
    /** Marketplace the plugin came from (empty string for direct repo installs) */
    @JsonProperty("marketplace") String marketplace,
    /** Installed version, if known */
    @JsonProperty("version") String version,
    /** Installation timestamp (ISO-8601) */
    @JsonProperty("installed_at") String installedAt,
    /** Whether the plugin is currently enabled */
    @JsonProperty("enabled") Boolean enabled,
    /** Path where the plugin is cached locally */
    @JsonProperty("cache_path") String cachePath,
    /** Source descriptor for direct repo installs (when marketplace is empty) */
    @JsonProperty("source") Object source,
    /** Per-plugin source fingerprint (a SHA-256 hash of the plugin's catalog source spec plus its resolved source subtree — NOT a Git commit SHA) captured at marketplace install/update time. Auto-update compares it against the freshly recomputed fingerprint to detect a content change that does not bump the version. Absent for pre-existing installs and for direct (non-marketplace) installs. */
    @JsonProperty("source_sha") String sourceSha,
    /** Absolute path of the marketplace directory a live plugin was resolved from. Present only on live, never-persisted records — those synthesized at session start for a directory/local marketplace, whose cache_path points at the real plugin directory on disk rather than a copy under the installed-plugins cache. Its presence is what marks a record as live, and no record carrying it is ever written to the persisted installedPlugins key. */
    @JsonProperty("installed_from") String installedFrom
) {
}
