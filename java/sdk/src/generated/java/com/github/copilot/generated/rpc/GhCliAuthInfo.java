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
 * Authentication-info input variant for GitHub CLI credentials, carrying host, login, and the `gh auth token` value.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class GhCliAuthInfo extends AuthInfo {

    @JsonProperty("type")
    private final String type = "gh-cli";

    @Override
    public String getType() { return type; }

    /** Authentication host. */
    @JsonProperty("host")
    private String host;

    /** User login as reported by `gh auth status`. */
    @JsonProperty("login")
    private String login;

    /** The token returned by `gh auth token`. Treat as a secret. */
    @JsonProperty("token")
    private String token;

    /** Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set. */
    @JsonProperty("copilotUser")
    private CopilotUserResponse copilotUser;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public CopilotUserResponse getCopilotUser() { return copilotUser; }
    public void setCopilotUser(CopilotUserResponse copilotUser) { this.copilotUser = copilotUser; }


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
        private String host;
        private String login;
        private String token;
        private CopilotUserResponse copilotUser;

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
         * Sets the {@code token} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder token(String value) {
            this.token = value;
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
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public GhCliAuthInfo build() {
            if (host == null) throw new IllegalStateException("Missing required property: host");
            if (login == null) throw new IllegalStateException("Missing required property: login");
            if (token == null) throw new IllegalStateException("Missing required property: token");
            GhCliAuthInfo instance = new GhCliAuthInfo();
            instance.setHost(host);
            instance.setLogin(login);
            instance.setToken(token);
            instance.setCopilotUser(copilotUser);
            return instance;
        }
    }
}
