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
 * Schema for the `GhCliAuthInfo` type.
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
}
