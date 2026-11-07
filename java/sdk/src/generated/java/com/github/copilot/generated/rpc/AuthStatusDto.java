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
 * Neutral authentication status summary.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthStatusDto(
    /** Whether the session has resolved authentication. */
    @JsonProperty("isAuthenticated") Boolean isAuthenticated,
    /** Active account login, if authenticated. */
    @JsonProperty("activeLogin") String activeLogin,
    /** Active account host, if authenticated. */
    @JsonProperty("activeHost") String activeHost,
    /** Copilot plan tier of the active account, if known. */
    @JsonProperty("copilotPlan") String copilotPlan,
    /** Number of signed-in accounts in the roster. */
    @JsonProperty("accountCount") Long accountCount
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AuthStatusDto {}

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
        private Boolean isAuthenticated;
        private String activeLogin;
        private String activeHost;
        private String copilotPlan;
        private Long accountCount;

        /**
         * Sets the {@code isAuthenticated} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder isAuthenticated(Boolean value) {
            this.isAuthenticated = value;
            return this;
        }

        /**
         * Sets the {@code activeLogin} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder activeLogin(String value) {
            this.activeLogin = value;
            return this;
        }

        /**
         * Sets the {@code activeHost} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder activeHost(String value) {
            this.activeHost = value;
            return this;
        }

        /**
         * Sets the {@code copilotPlan} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder copilotPlan(String value) {
            this.copilotPlan = value;
            return this;
        }

        /**
         * Sets the {@code accountCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder accountCount(Long value) {
            this.accountCount = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AuthStatusDto build() {
            if (isAuthenticated == null) throw new IllegalStateException("Missing required property: isAuthenticated");
            if (accountCount == null) throw new IllegalStateException("Missing required property: accountCount");
            return new AuthStatusDto(isAuthenticated, activeLogin, activeHost, copilotPlan, accountCount);
        }
    }
}
