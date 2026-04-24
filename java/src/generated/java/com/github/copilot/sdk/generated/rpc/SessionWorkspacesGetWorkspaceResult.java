/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.annotation.processing.Generated;

/**
 * Result for the {@code session.workspaces.getWorkspace} RPC method.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionWorkspacesGetWorkspaceResult(
    /** Current workspace metadata, or null if not available */
    @JsonProperty("workspace") SessionWorkspacesGetWorkspaceResultWorkspace workspace
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionWorkspacesGetWorkspaceResultWorkspace(
        @JsonProperty("id") UUID id,
        @JsonProperty("cwd") String cwd,
        @JsonProperty("git_root") String gitRoot,
        @JsonProperty("repository") String repository,
        @JsonProperty("host_type") SessionWorkspacesGetWorkspaceResultWorkspaceHostType hostType,
        @JsonProperty("branch") String branch,
        @JsonProperty("summary") String summary,
        @JsonProperty("name") String name,
        @JsonProperty("summary_count") Long summaryCount,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("updated_at") OffsetDateTime updatedAt,
        @JsonProperty("remote_steerable") Boolean remoteSteerable,
        @JsonProperty("mc_task_id") String mcTaskId,
        @JsonProperty("mc_session_id") String mcSessionId,
        @JsonProperty("mc_last_event_id") String mcLastEventId,
        @JsonProperty("session_sync_level") SessionWorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel sessionSyncLevel,
        @JsonProperty("chronicle_sync_dismissed") Boolean chronicleSyncDismissed
    ) {

        public enum SessionWorkspacesGetWorkspaceResultWorkspaceHostType {
            /** The {@code github} variant. */
            GITHUB("github"),
            /** The {@code ado} variant. */
            ADO("ado");

            private final String value;
            SessionWorkspacesGetWorkspaceResultWorkspaceHostType(String value) { this.value = value; }
            @com.fasterxml.jackson.annotation.JsonValue
            public String getValue() { return value; }
            @com.fasterxml.jackson.annotation.JsonCreator
            public static SessionWorkspacesGetWorkspaceResultWorkspaceHostType fromValue(String value) {
                for (SessionWorkspacesGetWorkspaceResultWorkspaceHostType v : values()) {
                    if (v.value.equals(value)) return v;
                }
                throw new IllegalArgumentException("Unknown SessionWorkspacesGetWorkspaceResultWorkspaceHostType value: " + value);
            }
        }

        public enum SessionWorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel {
            /** The {@code local} variant. */
            LOCAL("local"),
            /** The {@code user} variant. */
            USER("user"),
            /** The {@code repo_and_user} variant. */
            REPO_AND_USER("repo_and_user");

            private final String value;
            SessionWorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel(String value) { this.value = value; }
            @com.fasterxml.jackson.annotation.JsonValue
            public String getValue() { return value; }
            @com.fasterxml.jackson.annotation.JsonCreator
            public static SessionWorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel fromValue(String value) {
                for (SessionWorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel v : values()) {
                    if (v.value.equals(value)) return v;
                }
                throw new IllegalArgumentException("Unknown SessionWorkspacesGetWorkspaceResultWorkspaceSessionSyncLevel value: " + value);
            }
        }
    }
}
