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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Workspace diff result for the requested mode.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionWorkspacesDiffResult(
    /** Diff mode requested by the client. */
    @JsonProperty("requestedMode") WorkspaceDiffMode requestedMode,
    /** Effective mode used for the returned changes. */
    @JsonProperty("mode") WorkspaceDiffMode mode,
    /** Changed files and their unified diffs. */
    @JsonProperty("changes") List<WorkspaceDiffFileChange> changes,
    /** Default branch used for a branch diff, when branch mode was requested. */
    @JsonProperty("baseBranch") String baseBranch,
    /** Whether a requested branch diff fell back to unstaged changes because branch diff failed. */
    @JsonProperty("isFallback") Boolean isFallback
) {
}
