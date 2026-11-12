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
 * A user settings document and the effective managed policy whose controlled entries should be stamped onto it.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record ManagedSettingsApplyControlsParams(
    /** The user settings document to stamp. Entries the managed layer does not name are returned exactly as supplied. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("settings") Object settings,
    /** The effective managed policy, as composed by `managedSettings.resolveEffectiveValue`. Omit it on an unmanaged host: the settings then come back unchanged. */
    @JsonProperty("managed") Object managed
) {
}
