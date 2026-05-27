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
 * Schema for the `InstalledPlugin` type.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record InstalledPlugin(
    /** Plugin name */
    @JsonProperty("name") String name,
    /** Marketplace the plugin came from (empty string for direct repo installs) */
    @JsonProperty("marketplace") String marketplace,
    /** Version installed (if available) */
    @JsonProperty("version") String version,
    /** Installation timestamp */
    @JsonProperty("installed_at") String installedAt,
    /** Whether the plugin is currently enabled */
    @JsonProperty("enabled") Boolean enabled,
    /** Path where the plugin is cached locally */
    @JsonProperty("cache_path") String cachePath,
    /** Source for direct repo installs (when marketplace is empty) */
    @JsonProperty("source") Object source
) {
}
