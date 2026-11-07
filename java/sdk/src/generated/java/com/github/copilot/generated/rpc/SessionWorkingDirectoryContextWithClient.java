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
 * A working-directory context together with the client that produced it.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionWorkingDirectoryContextWithClient(
    /** Current working directory path */
    @JsonProperty("cwd") String cwd,
    /** Root directory of the git repository */
    @JsonProperty("gitRoot") String gitRoot,
    /** Repository identifier derived from the git remote URL */
    @JsonProperty("repository") String repository,
    /** Hosting platform type of the repository */
    @JsonProperty("hostType") String hostType,
    /** Current git branch name */
    @JsonProperty("branch") String branch,
    /** Raw host string from the git remote URL */
    @JsonProperty("repositoryHost") String repositoryHost,
    /** Head commit of the current git branch */
    @JsonProperty("headCommit") String headCommit,
    /** Merge-base commit SHA */
    @JsonProperty("baseCommit") String baseCommit,
    /** Name of the client that created the session */
    @JsonProperty("clientName") String clientName
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SessionWorkingDirectoryContextWithClient {}

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
        private String hostType;
        private String branch;
        private String repositoryHost;
        private String headCommit;
        private String baseCommit;
        private String clientName;

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
        public Builder hostType(String value) {
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
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionWorkingDirectoryContextWithClient build() {
            if (cwd == null) throw new IllegalStateException("Missing required property: cwd");
            return new SessionWorkingDirectoryContextWithClient(cwd, gitRoot, repository, hostType, branch, repositoryHost, headCommit, baseCommit, clientName);
        }
    }
}
