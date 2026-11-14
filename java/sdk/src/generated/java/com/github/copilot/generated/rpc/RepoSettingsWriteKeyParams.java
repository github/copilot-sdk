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
 * One key to write or remove in a repository settings file.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record RepoSettingsWriteKeyParams(
    /** Absolute path of the repository root. */
    @JsonProperty("repoRoot") String repoRoot,
    /** Use repo for shared settings or local for settings.local.json. Other scopes are rejected. */
    @JsonProperty("scope") String scope,
    /** Canonical top-level key permitted by the repository settings allowlist. */
    @JsonProperty("key") String key,
    /** Value to validate and write. Required unless delete is true; a null value is validated, not treated as deletion. */
    @JsonProperty("value") Object value,
    /** True to remove the key instead of writing it. */
    @JsonProperty("delete") Boolean delete
) {
}
