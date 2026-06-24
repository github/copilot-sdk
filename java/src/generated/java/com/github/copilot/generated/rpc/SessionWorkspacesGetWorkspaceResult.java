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
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * Current workspace metadata for the session, including its absolute filesystem path when available.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionWorkspacesGetWorkspaceResult(
    /** Current workspace metadata, or null if not available */
    @JsonProperty("workspace") SessionWorkspacesGetWorkspaceResultWorkspace workspace,
    /** Absolute filesystem path to the workspace directory. Omitted when the session has no workspace (e.g. remote sessions). */
    @JsonProperty("path") String path
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionWorkspacesGetWorkspaceResultWorkspace(
        @JsonProperty("id") String id,
        @JsonProperty("cwd") String cwd,
        @JsonProperty("git_root") String gitRoot,
        @JsonProperty("repository") String repository,
        /** Allowed values for the `WorkspacesWorkspaceDetailsHostType` enumeration. */
        @JsonProperty("host_type") WorkspacesWorkspaceDetailsHostType hostType,
        @JsonProperty("branch") String branch,
        @JsonProperty("name") String name,
        @JsonProperty("client_name") String clientName,
        @JsonProperty("user_named") Boolean userNamed,
        @JsonProperty("summary_count") Long summaryCount,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("updated_at") OffsetDateTime updatedAt,
        @JsonProperty("remote_steerable") Boolean remoteSteerable,
        @JsonProperty("mc_task_id") String mcTaskId,
        @JsonProperty("mc_session_id") String mcSessionId,
        @JsonProperty("mc_last_event_id") String mcLastEventId,
        @JsonProperty("chronicle_sync_dismissed") Boolean chronicleSyncDismissed
    ) {
    }
}
