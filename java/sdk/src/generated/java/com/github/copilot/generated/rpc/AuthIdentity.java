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
 * Credential-free authentication identity safe to expose to hosts and user interfaces.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthIdentity(
    /** Authentication type */
    @JsonProperty("type") AuthInfoType type,
    /** Authentication host */
    @JsonProperty("host") String host,
    /** Authenticated login, when available */
    @JsonProperty("login") String login,
    /** Name of the environment variable that supplied the credential, when applicable */
    @JsonProperty("envVar") String envVar,
    /** Opaque SDK GitHub credential registration backing this identity. Routing metadata only; never a credential. */
    @JsonProperty("registrationId") String registrationId,
    /** Snapshot of the authenticated user's Copilot subscription info, if known */
    @JsonProperty("copilotUser") CopilotUserResponse copilotUser
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AuthIdentity {}

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
        private AuthInfoType type;
        private String host;
        private String login;
        private String envVar;
        private String registrationId;
        private CopilotUserResponse copilotUser;

        /**
         * Sets the {@code type} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder type(AuthInfoType value) {
            this.type = value;
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
         * Sets the {@code envVar} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder envVar(String value) {
            this.envVar = value;
            return this;
        }

        /**
         * Sets the {@code registrationId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder registrationId(String value) {
            this.registrationId = value;
            return this;
        }

        /**
         * Sets the {@code copilotUser} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder copilotUser(CopilotUserResponse value) {
            this.copilotUser = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AuthIdentity build() {
            if (type == null) throw new IllegalStateException("Missing required property: type");
            if (host == null) throw new IllegalStateException("Missing required property: host");
            return new AuthIdentity(type, host, login, envVar, registrationId, copilotUser);
        }
    }
}
