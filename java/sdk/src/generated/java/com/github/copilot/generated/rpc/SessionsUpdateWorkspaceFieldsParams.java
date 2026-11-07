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
 * Where the session's state lives, plus workspace-schema fields to merge into its workspace record. Stored keys outside the schema are not preserved, and a stored `fork_count` is never replaced.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionsUpdateWorkspaceFieldsParams(
    /** Root directory every session's state directory sits under */
    @JsonProperty("sessionsHome") String sessionsHome,
    /** Session ID naming the state directory under the sessions home. Rejected when it is absolute or contains a parent component, so it cannot escape the sessions home. */
    @JsonProperty("sessionId") String sessionId,
    /** Workspace-schema fields to merge into the record, as a JSON object. Fields the object omits keep their stored values, except stored keys outside the schema are not preserved and a stored `fork_count` is never replaced. */
    @JsonProperty("fieldsJson") String fieldsJson
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SessionsUpdateWorkspaceFieldsParams {}

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
        private String sessionsHome;
        private String sessionId;
        private String fieldsJson;

        /**
         * Sets the {@code sessionsHome} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessionsHome(String value) {
            this.sessionsHome = value;
            return this;
        }

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
         * Sets the {@code fieldsJson} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder fieldsJson(String value) {
            this.fieldsJson = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionsUpdateWorkspaceFieldsParams build() {
            if (sessionsHome == null) throw new IllegalStateException("Missing required property: sessionsHome");
            if (sessionId == null) throw new IllegalStateException("Missing required property: sessionId");
            if (fieldsJson == null) throw new IllegalStateException("Missing required property: fieldsJson");
            return new SessionsUpdateWorkspaceFieldsParams(sessionsHome, sessionId, fieldsJson);
        }
    }
}
