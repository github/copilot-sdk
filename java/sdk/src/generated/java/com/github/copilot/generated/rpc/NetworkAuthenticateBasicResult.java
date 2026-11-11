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
 * Credentials for a proxy's Basic authentication challenge.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record NetworkAuthenticateBasicResult(
    /** User name; omitted or empty when no credentials are available, which fails the request with the proxy's challenge. Must not contain a colon or control characters. Never log or persist it with the password. */
    @JsonProperty("username") String username,
    /** Password; omitted for an empty password. Must not contain control characters. Never log or persist it. */
    @JsonProperty("password") String password
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public NetworkAuthenticateBasicResult {}

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
        private String username;
        private String password;

        /**
         * Sets the {@code username} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder username(String value) {
            this.username = value;
            return this;
        }

        /**
         * Sets the {@code password} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder password(String value) {
            this.password = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public NetworkAuthenticateBasicResult build() {
            return new NetworkAuthenticateBasicResult(username, password);
        }
    }

    @Override
    public String toString() {
        return "NetworkAuthenticateBasicResult[username=" + username + ", password=" + (password == null ? null : "<redacted>") + "]";
    }
}
