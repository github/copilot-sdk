/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Result of installing a plugin.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluginsInstallResult(
    /** The newly installed plugin's metadata */
    @JsonProperty("plugin") InstalledPluginInfo plugin,
    /** Number of skills discovered and installed from the plugin */
    @JsonProperty("skillsInstalled") Long skillsInstalled,
    /** Optional post-install message provided by the plugin (e.g. setup instructions) */
    @JsonProperty("postInstallMessage") String postInstallMessage,
    /** Set when the install path is deprecated (e.g. direct repo / URL / local installs). Callers should surface this to end users. */
    @JsonProperty("deprecationWarning") String deprecationWarning
) {
}
