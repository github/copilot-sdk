/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Updated working directory and git context. Emitted as the new payload of `session.context_changed`.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionWorkingDirectoryContext(
    /** Current working directory path */
    @JsonProperty("cwd") String cwd,
    /** Root directory of the git repository, resolved via git rev-parse */
    @JsonProperty("gitRoot") String gitRoot,
    /** Repository identifier derived from the git remote URL ("owner/name" for GitHub, "org/project/repo" for Azure DevOps) */
    @JsonProperty("repository") String repository,
    /** Hosting platform type of the repository */
    @JsonProperty("hostType") SessionWorkingDirectoryContextHostType hostType,
    /** Raw host string from the git remote URL (e.g. "github.com", "dev.azure.com") */
    @JsonProperty("repositoryHost") String repositoryHost,
    /** Current git branch name */
    @JsonProperty("branch") String branch,
    /** Head commit of the current git branch */
    @JsonProperty("headCommit") String headCommit,
    /** Merge-base commit SHA (fork point from the remote default branch) */
    @JsonProperty("baseCommit") String baseCommit
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionWorkingDirectoryContext {}

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
        private String cwd;
        private String gitRoot;
        private String repository;
        private SessionWorkingDirectoryContextHostType hostType;
        private String repositoryHost;
        private String branch;
        private String headCommit;
        private String baseCommit;

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
         * Sets the {@code gitRoot} property.
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
         * Sets the {@code hostType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
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
        public Builder branch(String value) {
            this.branch = value;
            return this;
        }

        /**
         * Sets the {@code headCommit} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
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
        public SessionWorkingDirectoryContext build() {
            if (cwd == null) throw new IllegalStateException("Missing required property: cwd");
            return new SessionWorkingDirectoryContext(cwd, gitRoot, repository, hostType, repositoryHost, branch, headCommit, baseCommit);
        }
    }
}
