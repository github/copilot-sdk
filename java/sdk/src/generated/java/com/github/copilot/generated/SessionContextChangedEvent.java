/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Session event "session.context_changed". Updated working directory and git context after the change
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionContextChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.context_changed"; }

    @JsonProperty("data")
    private SessionContextChangedEventData data;

    public SessionContextChangedEventData getData() { return data; }
    public void setData(SessionContextChangedEventData data) { this.data = data; }

    /** Data payload for {@link SessionContextChangedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionContextChangedEventData(
        /** Current working directory path */
        @JsonProperty("cwd") String cwd,
        /** Root directory of the git repository, resolved via git rev-parse */
        @JsonProperty("gitRoot") String gitRoot,
        /** Repository identifier derived from the git remote URL ("owner/name" for GitHub, "org/project/repo" for Azure DevOps) */
        @JsonProperty("repository") String repository,
        /** Hosting platform type of the repository (github or ado) */
        @JsonProperty("hostType") WorkingDirectoryContextHostType hostType,
        /** Raw host string from the git remote URL (e.g. "github.com", "mycompany.ghe.com", "dev.azure.com") */
        @JsonProperty("repositoryHost") String repositoryHost,
        /** Current git branch name */
        @JsonProperty("branch") String branch,
        /** Head commit of current git branch at session start time */
        @JsonProperty("headCommit") String headCommit,
        /** Base commit of current git branch at session start time */
        @JsonProperty("baseCommit") String baseCommit,
        /** Set on the immediate preliminary event of a working-directory change, before the git context is resolved. A settled follow-up event (enriched with git context, or cwd-only for a non-repository) is always emitted afterward, so observers may defer to it. Absent on standalone/final events (e.g. relay context changes). */
        @JsonProperty("pendingGitContext") Boolean pendingGitContext
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionContextChangedEventData {}

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
            private WorkingDirectoryContextHostType hostType;
            private String repositoryHost;
            private String branch;
            private String headCommit;
            private String baseCommit;
            private Boolean pendingGitContext;

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
            public Builder hostType(WorkingDirectoryContextHostType value) {
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
             * Sets the {@code pendingGitContext} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder pendingGitContext(Boolean value) {
                this.pendingGitContext = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionContextChangedEventData build() {
                if (cwd == null) throw new IllegalStateException("Missing required property: cwd");
                return new SessionContextChangedEventData(cwd, gitRoot, repository, hostType, repositoryHost, branch, headCommit, baseCommit, pendingGitContext);
            }
        }
    }
}
