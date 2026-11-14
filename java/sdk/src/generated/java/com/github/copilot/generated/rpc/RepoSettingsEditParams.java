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
 * One atomic edit of an allowed top-level repository setting.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record RepoSettingsEditParams(
    /** Absolute path of the repository root. */
    @JsonProperty("repoRoot") String repoRoot,
    /** Shared or local repository settings file. */
    @JsonProperty("scope") RepoSettingsEditScope scope,
    /** One canonical top-level repository setting that the allowlist permits. */
    @JsonProperty("key") String key,
    /** Set a scalar, add or remove a string list item, or remove the setting. The handler rejects a missing or incompatible value. */
    @JsonProperty("operation") SettingsEditOperation operation,
    /** Required boolean, string or number for set, required string for add-item and remove-item, and forbidden for remove. The runtime rejects all other JSON values. */
    @JsonProperty("value") Object value
) {
}
