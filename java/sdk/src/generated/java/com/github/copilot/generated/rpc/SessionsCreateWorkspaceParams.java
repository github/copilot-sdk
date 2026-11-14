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
 * Identity, state location and starting context for a workspace record.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionsCreateWorkspaceParams(
    /** Session ID the workspace record belongs to */
    @JsonProperty("sessionId") String sessionId,
    /** Directory the session's state is written under when no session filesystem provider is configured. Ignored when a provider is configured; the provider's session state path is used instead. */
    @JsonProperty("sessionStatePath") String sessionStatePath,
    /** `windows` (any letter case) selects Windows path rules. Any other value selects POSIX path rules. */
    @JsonProperty("convention") String convention,
    /** Starting working-directory context. The record keeps `cwd`, `gitRoot`, `repository`, `hostType`, `branch`, and `clientName`. Other fields, including `repositoryHost`, `headCommit`, and `baseCommit`, are ignored. `hostType` must be `github` or `ado`. */
    @JsonProperty("context") SessionWorkingDirectoryContextWithClient context,
    /** User-supplied display name for the workspace */
    @JsonProperty("name") String name
) {
}
