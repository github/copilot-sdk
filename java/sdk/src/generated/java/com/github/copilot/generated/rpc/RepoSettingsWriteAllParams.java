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
 * The complete set of repository-overridable settings to store.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record RepoSettingsWriteAllParams(
    /** Absolute path of the repository root. */
    @JsonProperty("repoRoot") String repoRoot,
    /** Use repo for shared settings or local for settings.local.json. Other scopes are rejected. */
    @JsonProperty("scope") String scope,
    /** Object of allowed settings to store. Missing allowlisted keys are removed. Other input keys are ignored, while unknown stored keys are preserved. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("settings") Object settings
) {
}
