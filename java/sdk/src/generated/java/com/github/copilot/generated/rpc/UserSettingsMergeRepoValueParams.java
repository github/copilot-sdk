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
 * The user layer and the repository layer to fold together.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsMergeRepoValueParams(
    /** The user's settings, already resolved against any managed layer. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("userSettings") Object userSettings,
    /** The repository's checked-in settings layer. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("repoSettings") Object repoSettings
) {
}
