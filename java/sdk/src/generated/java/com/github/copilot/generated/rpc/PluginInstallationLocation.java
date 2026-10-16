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
 * Where a plugin is installed, without an absolute host path.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluginInstallationLocation(
    /** Path relative to the Copilot home, under installed-plugins/_direct like any direct plugin install. */
    @JsonProperty("relativePath") String relativePath,
    /** Safe display label, for example ~/.copilot/installed-plugins/_direct/owner--repo--plugins-work-hub. */
    @JsonProperty("displayLabel") String displayLabel
) {
}
