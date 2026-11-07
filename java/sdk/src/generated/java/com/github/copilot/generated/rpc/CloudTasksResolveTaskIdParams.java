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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Identifies one cloud coding-agent task to resolve to an openable session, the credential to resolve it under, and the local candidates the caller has already rejected.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record CloudTasksResolveTaskIdParams(
    /** Identifier of the cloud task to resolve. */
    @JsonProperty("taskId") String taskId,
    /** The credential the lookup is performed under, carried opaquely because its shape is the host's own and the runtime only resolves a token and an API endpoint from it. Required: the task endpoint is per-account, so there is no unauthenticated form of this call. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("authInfo") Object authInfo,
    /** Selects which stored credential to resolve when the host holds more than one. Omit it to resolve the default for the supplied `authInfo`. */
    @JsonProperty("authContextId") String authContextId,
    /** Name of the embedding client, reported to Mission Control so the service can attribute the request. */
    @JsonProperty("integrationId") String integrationId,
    /** Overrides the GitHub API base URL the task lookup goes to. Intended for testing against a non-production service; omit it in normal use. */
    @JsonProperty("debugGithubApiUrl") String debugGitHubApiUrl,
    /** Directory holding persisted session state to scan for a local match. Omit it to use the default session home. */
    @JsonProperty("sessionsHome") String sessionsHome,
    /** Whether a task carrying no recorded CLI run still counts as a match. Set it when the caller wants the task itself, not only a run that already produced a session. */
    @JsonProperty("allowEmptyCliTask") Boolean allowEmptyCliTask,
    /** Whether the caller can open a cloud session. When false, a task that resolves only to a remote run is reported as unresumable instead, because the caller could not act on it. */
    @JsonProperty("hasRemoteSessionManager") Boolean hasRemoteSessionManager,
    /** Local session identifiers the caller has already rejected, so resolution skips them and returns the next candidate. Pass the identifiers accumulated across earlier calls, not only the last one. */
    @JsonProperty("ignoredLocalSessionIds") List<String> ignoredLocalSessionIds
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    CloudTasksResolveTaskIdParams {}

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
        private String taskId;
        private Object authInfo;
        private boolean $presence1;
        private String authContextId;
        private String integrationId;
        private String debugGitHubApiUrl;
        private String sessionsHome;
        private Boolean allowEmptyCliTask;
        private Boolean hasRemoteSessionManager;
        private List<String> ignoredLocalSessionIds;

        /**
         * Sets the {@code taskId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder taskId(String value) {
            this.taskId = value;
            return this;
        }

        /**
         * Sets the {@code authInfo} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder authInfo(Object value) {
            this.authInfo = value;
            this.$presence1 = true;
            return this;
        }

        /**
         * Sets the {@code authContextId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder authContextId(String value) {
            this.authContextId = value;
            return this;
        }

        /**
         * Sets the {@code integrationId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder integrationId(String value) {
            this.integrationId = value;
            return this;
        }

        /**
         * Sets the {@code debugGithubApiUrl} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder debugGitHubApiUrl(String value) {
            this.debugGitHubApiUrl = value;
            return this;
        }

        /**
         * Sets the {@code sessionsHome} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder sessionsHome(String value) {
            this.sessionsHome = value;
            return this;
        }

        /**
         * Sets the {@code allowEmptyCliTask} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder allowEmptyCliTask(Boolean value) {
            this.allowEmptyCliTask = value;
            return this;
        }

        /**
         * Sets the {@code hasRemoteSessionManager} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder hasRemoteSessionManager(Boolean value) {
            this.hasRemoteSessionManager = value;
            return this;
        }

        /**
         * Sets the {@code ignoredLocalSessionIds} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder ignoredLocalSessionIds(List<String> value) {
            this.ignoredLocalSessionIds = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CloudTasksResolveTaskIdParams build() {
            if (taskId == null) throw new IllegalStateException("Missing required property: taskId");
            if (!$presence1) throw new IllegalStateException("Missing required property: authInfo");
            if (integrationId == null) throw new IllegalStateException("Missing required property: integrationId");
            if (allowEmptyCliTask == null) throw new IllegalStateException("Missing required property: allowEmptyCliTask");
            if (hasRemoteSessionManager == null) throw new IllegalStateException("Missing required property: hasRemoteSessionManager");
            if (ignoredLocalSessionIds == null) throw new IllegalStateException("Missing required property: ignoredLocalSessionIds");
            return new CloudTasksResolveTaskIdParams(taskId, authInfo, authContextId, integrationId, debugGitHubApiUrl, sessionsHome, allowEmptyCliTask, hasRemoteSessionManager, ignoredLocalSessionIds);
        }
    }
}
