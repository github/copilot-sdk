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
public record SessionWorkspacesEnsureResult(
    /** Current workspace metadata, or null if not available */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("workspace") SessionWorkspacesEnsureResultWorkspace workspace,
    /** Absolute filesystem path to the workspace directory. Omitted when the session has no workspace (e.g. remote sessions). */
    @JsonProperty("path") String path
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionWorkspacesEnsureResultWorkspace(
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

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionWorkspacesEnsureResultWorkspace {}

        /**
         * Returns a named builder.
         * @return a new builder
         * @since 2.0.0
         */
        public static Builder builder() { return new Builder(); }

        /**
         * Named construction for this record.
         * @since 2.0.0
         */
        public static final class Builder {
            private Builder() {}
            private String id;
            private String cwd;
            private String gitRoot;
            private String repository;
            private WorkspacesWorkspaceDetailsHostType hostType;
            private String branch;
            private String name;
            private String clientName;
            private Boolean userNamed;
            private Long summaryCount;
            private OffsetDateTime createdAt;
            private OffsetDateTime updatedAt;
            private Boolean remoteSteerable;
            private String mcTaskId;
            private String mcSessionId;
            private String mcLastEventId;
            private Boolean chronicleSyncDismissed;

            /**
             * Sets the {@code id} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder id(String value) {
                this.id = value;
                return this;
            }

            /**
             * Sets the {@code cwd} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder cwd(String value) {
                this.cwd = value;
                return this;
            }

            /**
             * Sets the {@code git_root} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder gitRoot(String value) {
                this.gitRoot = value;
                return this;
            }

            /**
             * Sets the {@code repository} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder repository(String value) {
                this.repository = value;
                return this;
            }

            /**
             * Sets the {@code host_type} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder hostType(WorkspacesWorkspaceDetailsHostType value) {
                this.hostType = value;
                return this;
            }

            /**
             * Sets the {@code branch} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder branch(String value) {
                this.branch = value;
                return this;
            }

            /**
             * Sets the {@code name} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder name(String value) {
                this.name = value;
                return this;
            }

            /**
             * Sets the {@code client_name} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder clientName(String value) {
                this.clientName = value;
                return this;
            }

            /**
             * Sets the {@code user_named} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder userNamed(Boolean value) {
                this.userNamed = value;
                return this;
            }

            /**
             * Sets the {@code summary_count} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder summaryCount(Long value) {
                this.summaryCount = value;
                return this;
            }

            /**
             * Sets the {@code created_at} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder createdAt(OffsetDateTime value) {
                this.createdAt = value;
                return this;
            }

            /**
             * Sets the {@code updated_at} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder updatedAt(OffsetDateTime value) {
                this.updatedAt = value;
                return this;
            }

            /**
             * Sets the {@code remote_steerable} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder remoteSteerable(Boolean value) {
                this.remoteSteerable = value;
                return this;
            }

            /**
             * Sets the {@code mc_task_id} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder mcTaskId(String value) {
                this.mcTaskId = value;
                return this;
            }

            /**
             * Sets the {@code mc_session_id} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder mcSessionId(String value) {
                this.mcSessionId = value;
                return this;
            }

            /**
             * Sets the {@code mc_last_event_id} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder mcLastEventId(String value) {
                this.mcLastEventId = value;
                return this;
            }

            /**
             * Sets the {@code chronicle_sync_dismissed} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder chronicleSyncDismissed(Boolean value) {
                this.chronicleSyncDismissed = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionWorkspacesEnsureResultWorkspace build() {
                if (id == null) throw new IllegalStateException("Missing required property: id");
                return new SessionWorkspacesEnsureResultWorkspace(id, cwd, gitRoot, repository, hostType, branch, name, clientName, userNamed, summaryCount, createdAt, updatedAt, remoteSteerable, mcTaskId, mcSessionId, mcLastEventId, chronicleSyncDismissed);
            }
        }
    }

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionWorkspacesEnsureResult {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    public static final class Builder {
        private Builder() {}
        private SessionWorkspacesEnsureResultWorkspace workspace;
        private boolean $presence0;
        private String path;

        /**
         * Sets the {@code workspace} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder workspace(SessionWorkspacesEnsureResultWorkspace value) {
            this.workspace = value;
            this.$presence0 = true;
            return this;
        }

        /**
         * Sets the {@code path} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder path(String value) {
            this.path = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionWorkspacesEnsureResult build() {
            if (!$presence0) throw new IllegalStateException("Missing required property: workspace");
            return new SessionWorkspacesEnsureResult(workspace, path);
        }
    }
}
