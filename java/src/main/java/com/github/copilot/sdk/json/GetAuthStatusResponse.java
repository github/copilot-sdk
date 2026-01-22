/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from the auth.getStatus RPC call.
 * <p>
 * Contains information about the current authentication status.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetAuthStatusResponse {

    /**
     * Whether the user is authenticated.
     */
    @JsonProperty("isAuthenticated")
    private boolean isAuthenticated;

    /**
     * Authentication type (user, env, gh-cli, hmac, api-key, token).
     */
    @JsonProperty("authType")
    private String authType;

    /**
     * GitHub host URL.
     */
    @JsonProperty("host")
    private String host;

    /**
     * User login name.
     */
    @JsonProperty("login")
    private String login;

    /**
     * Human-readable status message.
     */
    @JsonProperty("statusMessage")
    private String statusMessage;

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public GetAuthStatusResponse setAuthenticated(boolean authenticated) {
        isAuthenticated = authenticated;
        return this;
    }

    public String getAuthType() {
        return authType;
    }

    public GetAuthStatusResponse setAuthType(String authType) {
        this.authType = authType;
        return this;
    }

    public String getHost() {
        return host;
    }

    public GetAuthStatusResponse setHost(String host) {
        this.host = host;
        return this;
    }

    public String getLogin() {
        return login;
    }

    public GetAuthStatusResponse setLogin(String login) {
        this.login = login;
        return this;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public GetAuthStatusResponse setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
        return this;
    }
}
