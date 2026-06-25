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
 * Schema for the `PluginUpdateAllEntry` type.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PluginUpdateAllEntry(
    /** Plugin name that was updated */
    @JsonProperty("name") String name,
    /** Marketplace the plugin came from. Empty string ("") for direct installs. */
    @JsonProperty("marketplace") String marketplace,
    /** Whether the update succeeded for this plugin */
    @JsonProperty("success") Boolean success,
    /** Previously installed version, when available */
    @JsonProperty("previousVersion") String previousVersion,
    /** Version after the update, when available */
    @JsonProperty("newVersion") String newVersion,
    /** Number of skills installed after the update (success only) */
    @JsonProperty("skillsInstalled") Long skillsInstalled,
    /** Error message (failure only) */
    @JsonProperty("error") String error
) {
}
