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
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("workspace") SessionWorkspacesGetWorkspaceResultWorkspace workspace,
    /** Absolute filesystem path to the workspace directory. Omitted when the session has no workspace (e.g. remote sessions). */
    @JsonProperty("path") String path
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionWorkspacesGetWorkspaceResultWorkspace(
        /** Stable workspace identifier. */
        @JsonProperty("id") String id,
        /** Current working directory associated with the workspace. */
        @JsonProperty("cwd") String cwd,
        /** Git repository root associated with the workspace. */
        @JsonProperty("git_root") String gitRoot,
        /** Repository identifier associated with the workspace. */
        @JsonProperty("repository") String repository,
        /** Allowed values for the `WorkspacesWorkspaceDetailsHostType` enumeration. */
        @JsonProperty("host_type") WorkspacesWorkspaceDetailsHostType hostType,
        /** Current Git branch. */
        @JsonProperty("branch") String branch,
        /** Workspace display name. */
        @JsonProperty("name") String name,
        /** Name of the client that created the workspace. */
        @JsonProperty("client_name") String clientName,
        /** Whether the workspace name was explicitly chosen by the user. */
        @JsonProperty("user_named") Boolean userNamed,
        /** Number of persisted summaries in the workspace. */
        @JsonProperty("summary_count") Long summaryCount,
        /** Timestamp when the workspace was created. */
        @JsonProperty("created_at") OffsetDateTime createdAt,
        /** Timestamp when the workspace was last updated. */
        @JsonProperty("updated_at") OffsetDateTime updatedAt,
        /** Whether the workspace session can be steered remotely. */
        @JsonProperty("remote_steerable") Boolean remoteSteerable,
        /** Mission Control task identifier associated with the workspace. */
        @JsonProperty("mc_task_id") String mcTaskId,
        /** Mission Control session identifier associated with the workspace. */
        @JsonProperty("mc_session_id") String mcSessionId,
        /** Most recent Mission Control event identifier observed for the workspace. */
        @JsonProperty("mc_last_event_id") String mcLastEventId,
        /** Whether the per-session Chronicle upgrade prompt was dismissed for the workspace. */
        @JsonProperty("chronicle_sync_dismissed") Boolean chronicleSyncDismissed
    ) {
    }
}
