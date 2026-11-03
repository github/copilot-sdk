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
 * Identifies one cloud coding-agent task to resolve to an openable session, the credential to resolve it under, and the local candidates the caller has already rejected.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record CloudTasksResolveTaskIdParams(
    /** Identifier of the cloud task to resolve. */
    @JsonProperty("taskId") String taskId,
    /** The credential the lookup is performed under, carried opaquely because its shape is the host's own and the runtime only resolves a token and an API endpoint from it. Required: the task endpoint is per-account, so there is no unauthenticated form of this call. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("authInfo") Object authInfo,
    /** Selects which stored credential to resolve when the host holds more than one. Omit it to resolve the default for the supplied `authInfo`. */
    @JsonProperty("authContextId") String authContextId,
    /** Name of the embedding client, reported to Mission Control so the service can attribute the request. */
    @JsonProperty("integrationId") String integrationId,
    /** Overrides the GitHub API base URL the task lookup goes to. Intended for testing against a non-production service; omit it in normal use. */
    @JsonProperty("debugGithubApiUrl") String debugGitHubApiUrl,
    /** Directory holding persisted session state to scan for a local match. Omit it to use the default session home. */
    @JsonProperty("sessionsHome") String sessionsHome,
    /** Whether a task carrying no recorded CLI run still counts as a match. Set it when the caller wants the task itself, not only a run that already produced a session. */
    @JsonProperty("allowEmptyCliTask") Boolean allowEmptyCliTask,
    /** Whether the caller can open a cloud session. When false, a task that resolves only to a remote run is reported as unresumable instead, because the caller could not act on it. */
    @JsonProperty("hasRemoteSessionManager") Boolean hasRemoteSessionManager,
    /** Local session identifiers the caller has already rejected, so resolution skips them and returns the next candidate. Pass the identifiers accumulated across earlier calls, not only the last one. */
    @JsonProperty("ignoredLocalSessionIds") List<String> ignoredLocalSessionIds
) {
}
