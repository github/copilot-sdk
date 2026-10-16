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
 * One hook to disable or re-enable.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsSetHookDisabledParams(
    /** Path of the settings.json to rewrite. */
    @JsonProperty("settingsPath") String settingsPath,
    /** Durable content-hash key of the hook, as `hooks.loadResources` reports it. */
    @JsonProperty("disableKey") String disableKey,
    /** True adds the key to `disabledHooks`, false removes it. */
    @JsonProperty("disabled") Boolean disabled
) {
}
