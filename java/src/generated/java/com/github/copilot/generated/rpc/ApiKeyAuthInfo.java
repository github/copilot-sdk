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
 * Schema for the `ApiKeyAuthInfo` type.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ApiKeyAuthInfo extends AuthInfo {

    @JsonProperty("type")
    private final String type = "api-key";

    @Override
    public String getType() { return type; }

    /** The API key. Treat as a secret. */
    @JsonProperty("apiKey")
    private String apiKey;

    /** Authentication host. */
    @JsonProperty("host")
    private String host;

    /** Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set. */
    @JsonProperty("copilotUser")
    private CopilotUserResponse copilotUser;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public CopilotUserResponse getCopilotUser() { return copilotUser; }
    public void setCopilotUser(CopilotUserResponse copilotUser) { this.copilotUser = copilotUser; }
}
