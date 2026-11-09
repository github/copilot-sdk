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
 * Session host's working directory and optional repository context.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMetadataGetWorkingDirectoryContextResult(
    /** Requested working directory. */
    @JsonProperty("cwd") String cwd,
    /** Repository working tree root. */
    @JsonProperty("gitRoot") String gitRoot,
    /** Repository identifier from the existing Git owner. */
    @JsonProperty("repository") String repository,
    /** Recognized repository host platform. */
    @JsonProperty("hostType") SessionWorkingDirectoryContextHostType hostType,
    /** Repository remote host. */
    @JsonProperty("repositoryHost") String repositoryHost,
    /** Current branch, when available. */
    @JsonProperty("branch") String branch,
    /** Default branch, only when requested and available. */
    @JsonProperty("defaultBranch") String defaultBranch,
    /** Current HEAD commit, when available. */
    @JsonProperty("headCommit") String headCommit,
    /** Merge-base commit, when available. */
    @JsonProperty("baseCommit") String baseCommit
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionMetadataGetWorkingDirectoryContextResult {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static final class Builder {
        private Builder() {}
        private String cwd;
        private String gitRoot;
        private String repository;
        private SessionWorkingDirectoryContextHostType hostType;
        private String repositoryHost;
        private String branch;
        private String defaultBranch;
        private String headCommit;
        private String baseCommit;

        /**
         * Sets the {@code cwd} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder cwd(String value) {
            this.cwd = value;
            return this;
        }

        /**
         * Sets the {@code gitRoot} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
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
        @com.github.copilot.CopilotExperimental
        public Builder repository(String value) {
            this.repository = value;
            return this;
        }

        /**
         * Sets the {@code hostType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder hostType(SessionWorkingDirectoryContextHostType value) {
            this.hostType = value;
            return this;
        }

        /**
         * Sets the {@code repositoryHost} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder repositoryHost(String value) {
            this.repositoryHost = value;
            return this;
        }

        /**
         * Sets the {@code branch} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder branch(String value) {
            this.branch = value;
            return this;
        }

        /**
         * Sets the {@code defaultBranch} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder defaultBranch(String value) {
            this.defaultBranch = value;
            return this;
        }

        /**
         * Sets the {@code headCommit} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder headCommit(String value) {
            this.headCommit = value;
            return this;
        }

        /**
         * Sets the {@code baseCommit} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder baseCommit(String value) {
            this.baseCommit = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionMetadataGetWorkingDirectoryContextResult build() {
            if (cwd == null) throw new IllegalStateException("Missing required property: cwd");
            return new SessionMetadataGetWorkingDirectoryContextResult(cwd, gitRoot, repository, hostType, repositoryHost, branch, defaultBranch, headCommit, baseCommit);
        }
    }
}
