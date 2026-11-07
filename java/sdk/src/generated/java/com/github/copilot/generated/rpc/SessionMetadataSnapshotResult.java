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
 * Point-in-time snapshot of slow-changing session identifier and state fields
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
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
    @JsonInclude(JsonInclude.Include.ALWAYS)
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
    /** Current session limits, or null when no limits are active */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("sessionLimits") SessionLimitsConfig sessionLimits,
    /** Live indexed-search state for this session activation. Omitted by runtimes that do not expose indexed-search status; absence does not indicate enablement. */
    @JsonProperty("indexedSearch") IndexedSearchState indexedSearch,
    /** Public-facing workspace metadata for this session, or null if the session has no associated workspace. Excludes runtime-internal fields (GitHub IDs, summary count, internal flags). */
    @JsonInclude(JsonInclude.Include.ALWAYS)
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

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionMetadataSnapshotResultWorkspace {}

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
            private WorkspaceSummaryHostType hostType;
            private String branch;
            private String name;
            private Boolean userNamed;
            private OffsetDateTime createdAt;
            private OffsetDateTime updatedAt;

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
            public Builder hostType(WorkspaceSummaryHostType value) {
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
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionMetadataSnapshotResultWorkspace build() {
                if (id == null) throw new IllegalStateException("Missing required property: id");
                return new SessionMetadataSnapshotResultWorkspace(id, cwd, gitRoot, repository, hostType, branch, name, userNamed, createdAt, updatedAt);
            }
        }
    }

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionMetadataSnapshotResult {}

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
        private String sessionId;
        private OffsetDateTime startTime;
        private OffsetDateTime modifiedTime;
        private Boolean isRemote;
        private Boolean alreadyInUse;
        private String workspacePath;
        private boolean $presence5;
        private String initialName;
        private String clientName;
        private MetadataSnapshotRemoteMetadata remoteMetadata;
        private String summary;
        private String workingDirectory;
        private MetadataSnapshotCurrentMode currentMode;
        private String selectedModel;
        private SessionLimitsConfig sessionLimits;
        private boolean $presence13;
        private IndexedSearchState indexedSearch;
        private SessionMetadataSnapshotResultWorkspace workspace;
        private boolean $presence15;

        /**
         * Sets the {@code sessionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessionId(String value) {
            this.sessionId = value;
            return this;
        }

        /**
         * Sets the {@code startTime} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder startTime(OffsetDateTime value) {
            this.startTime = value;
            return this;
        }

        /**
         * Sets the {@code modifiedTime} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder modifiedTime(OffsetDateTime value) {
            this.modifiedTime = value;
            return this;
        }

        /**
         * Sets the {@code isRemote} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder isRemote(Boolean value) {
            this.isRemote = value;
            return this;
        }

        /**
         * Sets the {@code alreadyInUse} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder alreadyInUse(Boolean value) {
            this.alreadyInUse = value;
            return this;
        }

        /**
         * Sets the {@code workspacePath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder workspacePath(String value) {
            this.workspacePath = value;
            this.$presence5 = true;
            return this;
        }

        /**
         * Sets the {@code initialName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder initialName(String value) {
            this.initialName = value;
            return this;
        }

        /**
         * Sets the {@code clientName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder clientName(String value) {
            this.clientName = value;
            return this;
        }

        /**
         * Sets the {@code remoteMetadata} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder remoteMetadata(MetadataSnapshotRemoteMetadata value) {
            this.remoteMetadata = value;
            return this;
        }

        /**
         * Sets the {@code summary} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder summary(String value) {
            this.summary = value;
            return this;
        }

        /**
         * Sets the {@code workingDirectory} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder workingDirectory(String value) {
            this.workingDirectory = value;
            return this;
        }

        /**
         * Sets the {@code currentMode} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder currentMode(MetadataSnapshotCurrentMode value) {
            this.currentMode = value;
            return this;
        }

        /**
         * Sets the {@code selectedModel} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder selectedModel(String value) {
            this.selectedModel = value;
            return this;
        }

        /**
         * Sets the {@code sessionLimits} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessionLimits(SessionLimitsConfig value) {
            this.sessionLimits = value;
            this.$presence13 = true;
            return this;
        }

        /**
         * Sets the {@code indexedSearch} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder indexedSearch(IndexedSearchState value) {
            this.indexedSearch = value;
            return this;
        }

        /**
         * Sets the {@code workspace} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder workspace(SessionMetadataSnapshotResultWorkspace value) {
            this.workspace = value;
            this.$presence15 = true;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionMetadataSnapshotResult build() {
            if (sessionId == null) throw new IllegalStateException("Missing required property: sessionId");
            if (startTime == null) throw new IllegalStateException("Missing required property: startTime");
            if (modifiedTime == null) throw new IllegalStateException("Missing required property: modifiedTime");
            if (isRemote == null) throw new IllegalStateException("Missing required property: isRemote");
            if (alreadyInUse == null) throw new IllegalStateException("Missing required property: alreadyInUse");
            if (!$presence5) throw new IllegalStateException("Missing required property: workspacePath");
            if (workingDirectory == null) throw new IllegalStateException("Missing required property: workingDirectory");
            if (currentMode == null) throw new IllegalStateException("Missing required property: currentMode");
            if (!$presence13) throw new IllegalStateException("Missing required property: sessionLimits");
            if (!$presence15) throw new IllegalStateException("Missing required property: workspace");
            return new SessionMetadataSnapshotResult(sessionId, startTime, modifiedTime, isRemote, alreadyInUse, workspacePath, initialName, clientName, remoteMetadata, summary, workingDirectory, currentMode, selectedModel, sessionLimits, indexedSearch, workspace);
        }
    }
}
