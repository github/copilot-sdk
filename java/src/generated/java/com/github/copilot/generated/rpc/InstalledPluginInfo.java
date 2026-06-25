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
    /** Installed version (when reported by the plugin manifest) */
    @JsonProperty("version") String version,
    /** Whether the plugin is currently enabled for new sessions */
    @JsonProperty("enabled") Boolean enabled
) {
}
