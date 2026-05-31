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
 * Schema for the `EnvAuthInfo` type.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class EnvAuthInfo extends AuthInfo {

    @JsonProperty("type")
    private final String type = "env";

    @Override
    public String getType() { return type; }

    /** Authentication host (e.g. https://github.com or a GHES host). */
    @JsonProperty("host")
    private String host;

    /** User login associated with the token. Undefined for server-to-server tokens (those starting with `ghs_`). */
    @JsonProperty("login")
    private String login;

    /** The token value itself. Treat as a secret. */
    @JsonProperty("token")
    private String token;

    /** Name of the environment variable the token was sourced from. */
    @JsonProperty("envVar")
    private String envVar;

    /** Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set. */
    @JsonProperty("copilotUser")
    private CopilotUserResponse copilotUser;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getEnvVar() { return envVar; }
    public void setEnvVar(String envVar) { this.envVar = envVar; }

    public CopilotUserResponse getCopilotUser() { return copilotUser; }
    public void setCopilotUser(CopilotUserResponse copilotUser) { this.copilotUser = copilotUser; }
}
