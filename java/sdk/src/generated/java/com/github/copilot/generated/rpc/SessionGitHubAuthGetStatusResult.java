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
 * Authentication status and account metadata for the session.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionGitHubAuthGetStatusResult(
    /** Whether the session has resolved authentication */
    @JsonProperty("isAuthenticated") Boolean isAuthenticated,
    /** Authentication type */
    @JsonProperty("authType") AuthInfoType authType,
    /** Authentication host URL */
    @JsonProperty("host") String host,
    /** Authenticated login/username, if available */
    @JsonProperty("login") String login,
    /** Human-readable authentication status description */
    @JsonProperty("statusMessage") String statusMessage,
    /** Copilot plan tier (e.g., individual_pro, business) */
    @JsonProperty("copilotPlan") String copilotPlan
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionGitHubAuthGetStatusResult {}

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
        private Boolean isAuthenticated;
        private AuthInfoType authType;
        private String host;
        private String login;
        private String statusMessage;
        private String copilotPlan;

        /**
         * Sets the {@code isAuthenticated} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder isAuthenticated(Boolean value) {
            this.isAuthenticated = value;
            return this;
        }

        /**
         * Sets the {@code authType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder authType(AuthInfoType value) {
            this.authType = value;
            return this;
        }

        /**
         * Sets the {@code host} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder host(String value) {
            this.host = value;
            return this;
        }

        /**
         * Sets the {@code login} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder login(String value) {
            this.login = value;
            return this;
        }

        /**
         * Sets the {@code statusMessage} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder statusMessage(String value) {
            this.statusMessage = value;
            return this;
        }

        /**
         * Sets the {@code copilotPlan} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder copilotPlan(String value) {
            this.copilotPlan = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionGitHubAuthGetStatusResult build() {
            if (isAuthenticated == null) throw new IllegalStateException("Missing required property: isAuthenticated");
            return new SessionGitHubAuthGetStatusResult(isAuthenticated, authType, host, login, statusMessage, copilotPlan);
        }
    }
}
