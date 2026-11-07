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
 * Identity, state location and starting context for a workspace record.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionsCreateWorkspaceParams(
    /** Session ID the workspace record belongs to */
    @JsonProperty("sessionId") String sessionId,
    /** Directory the session's state is written under when no session filesystem provider is configured. Ignored when a provider is configured; the provider's session state path is used instead. */
    @JsonProperty("sessionStatePath") String sessionStatePath,
    /** `windows` (any letter case) selects Windows path rules. Any other value selects POSIX path rules. */
    @JsonProperty("convention") String convention,
    /** Starting working-directory context. The record keeps `cwd`, `gitRoot`, `repository`, `hostType`, `branch`, and `clientName`. Other fields, including `repositoryHost`, `headCommit`, and `baseCommit`, are ignored. `hostType` must be `github` or `ado`. */
    @JsonProperty("context") SessionWorkingDirectoryContextWithClient context,
    /** User-supplied display name for the workspace */
    @JsonProperty("name") String name
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SessionsCreateWorkspaceParams {}

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
        private String sessionStatePath;
        private String convention;
        private SessionWorkingDirectoryContextWithClient context;
        private String name;

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
         * Sets the {@code sessionStatePath} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessionStatePath(String value) {
            this.sessionStatePath = value;
            return this;
        }

        /**
         * Sets the {@code convention} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder convention(String value) {
            this.convention = value;
            return this;
        }

        /**
         * Sets the {@code context} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder context(SessionWorkingDirectoryContextWithClient value) {
            this.context = value;
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
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionsCreateWorkspaceParams build() {
            if (sessionId == null) throw new IllegalStateException("Missing required property: sessionId");
            if (sessionStatePath == null) throw new IllegalStateException("Missing required property: sessionStatePath");
            if (convention == null) throw new IllegalStateException("Missing required property: convention");
            return new SessionsCreateWorkspaceParams(sessionId, sessionStatePath, convention, context, name);
        }
    }
}
