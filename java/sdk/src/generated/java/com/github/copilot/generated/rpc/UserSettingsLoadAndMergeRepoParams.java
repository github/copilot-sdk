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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * A repository to read settings from, and the user layer they are merged over.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record UserSettingsLoadAndMergeRepoParams(
    /** Absolute path of the repository root whose settings to read. When this is the user's home directory, the Claude-compatible configuration is skipped, so a home-directory session does not inherit a personal config as if it were repository policy. */
    @JsonProperty("repoRoot") String repoRoot,
    /** The user's own settings, keyed by setting name. The repository layer is merged over this; a repository may only add to the collection settings it is allowed to influence, so a user setting it does not name survives unchanged. */
    @JsonProperty("userSettings") Map<String, Object> userSettings
) {
}
