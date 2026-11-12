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
    /** Whether the requested diff fell back to unstaged changes, either because branch diff failed or session diff was unavailable. */
    @JsonProperty("isFallback") Boolean isFallback,
    /** Why the session diff could not be produced, when applicable. Set only when `session` mode was requested and `isFallback` is true, so a client can tell the permanent `file-change-tracking-disabled` apart from the transient `session-busy`, which the same request answers once the session settles. Never set for `unstaged` or `branch` mode, and never `unsupported-remote-session`: a remote session's captures live on its own host, so a `session`-mode diff is rejected for one rather than answered with a controller-side fallback. */
    @JsonProperty("unavailableReason") HistoryRewindUnavailableReason unavailableReason
) {
}
