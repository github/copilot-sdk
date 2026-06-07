/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * Point-in-time snapshot of slow-changing session identifier and state fields
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMetadataSnapshotResult(
    /** The unique identifier of the session */
    @JsonProperty("sessionId") String sessionId,
    /** ISO 8601 timestamp of when the session started */
    @JsonProperty("startTime") OffsetDateTime startTime,
    /** ISO 8601 timestamp of when the session's persisted state was last modified on disk. For new sessions, equals startTime. For resumed sessions, reflects the previous modification time at construction. */
    @JsonProperty("modifiedTime") OffsetDateTime modifiedTime,
    /** Whether this is a remote session (i.e., one whose runtime executes elsewhere and is steered through this process) */
    @JsonProperty("isRemote") Boolean isRemote,
    /** True when the session was detected to be in use by another process at construction time. Local consumers may surface a confirmation prompt before fully attaching. Always false for new sessions. */
    @JsonProperty("alreadyInUse") Boolean alreadyInUse,
    /** Absolute path to the session's workspace directory on disk, or null if the session has no associated workspace */
    @JsonProperty("workspacePath") String workspacePath,
    /** User-provided name supplied at session construction (via `--name`), if any. Immutable after construction. */
    @JsonProperty("initialName") String initialName,
    /** Runtime client name associated with the session (telemetry identifier). */
    @JsonProperty("clientName") String clientName,
    /** Remote-session-specific metadata. Populated only when `isRemote` is true. Fields are immutable for the lifetime of the session. */
    @JsonProperty("remoteMetadata") MetadataSnapshotRemoteMetadata remoteMetadata,
    /** Short human-readable summary of the session, if known. Omitted when no summary has been generated. */
    @JsonProperty("summary") String summary,
    /** Absolute path to the session's current working directory */
    @JsonProperty("workingDirectory") String workingDirectory,
    /** The current agent mode for this session (e.g., 'interactive', 'plan', 'autopilot') */
    @JsonProperty("currentMode") MetadataSnapshotCurrentMode currentMode,
    /** Currently selected model identifier, if any */
    @JsonProperty("selectedModel") String selectedModel,
    /** Public-facing workspace metadata for this session, or null if the session has no associated workspace. Excludes runtime-internal fields (GitHub IDs, summary count, internal flags). */
    @JsonProperty("workspace") SessionMetadataSnapshotResultWorkspace workspace
) {

    /** Public-facing projection of workspace metadata for SDK / TUI consumers */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionMetadataSnapshotResultWorkspace(
        /** Workspace identifier (1:1 with sessionId) */
        @JsonProperty("id") String id,
        /** Current working directory at session start */
        @JsonProperty("cwd") String cwd,
        /** Resolved git root for cwd, if any */
        @JsonProperty("git_root") String gitRoot,
        /** Repository identifier in 'owner/repo' or 'org/project/repo' format, if any */
        @JsonProperty("repository") String repository,
        /** Repository host type, if known */
        @JsonProperty("host_type") WorkspaceSummaryHostType hostType,
        /** Branch checked out at session start, if any */
        @JsonProperty("branch") String branch,
        /** Display name for the session, if set */
        @JsonProperty("name") String name,
        /** Whether the display name was explicitly set by the user */
        @JsonProperty("user_named") Boolean userNamed,
        /** ISO 8601 timestamp when the workspace was created */
        @JsonProperty("created_at") OffsetDateTime createdAt,
        /** ISO 8601 timestamp when the workspace was last updated */
        @JsonProperty("updated_at") OffsetDateTime updatedAt
    ) {
    }
}
