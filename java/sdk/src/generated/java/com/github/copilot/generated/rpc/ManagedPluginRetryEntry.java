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
 * Retry outcome for one plugin required by managed settings.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ManagedPluginRetryEntry(
    /** Plugin spec in "plugin@marketplace" form. */
    @JsonProperty("spec") String spec,
    /** What the retry did for this plugin. */
    @JsonProperty("status") ManagedPluginRetryStatus status,
    /** Why the plugin was not prepared. Present when `status` is `failed` or `deferred`. */
    @JsonProperty("error") String error
) {
}
