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
 * Schema for the `HMACAuthInfo` type.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class HMACAuthInfo extends AuthInfo {

    @JsonProperty("type")
    private final String type = "hmac";

    @Override
    public String getType() { return type; }

    /** Authentication host. HMAC auth always targets the public GitHub host. */
    @JsonProperty("host")
    private String host;

    /** HMAC secret used to sign requests. */
    @JsonProperty("hmac")
    private String hmac;

    /** Snapshot of the authenticated user's Copilot subscription info, if known. Mirrors the GitHub API `/copilot_internal/v2/token` user response shape — the runtime trusts this verbatim and does not re-fetch when set. */
    @JsonProperty("copilotUser")
    private CopilotUserResponse copilotUser;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public String getHmac() { return hmac; }
    public void setHmac(String hmac) { this.hmac = hmac; }

    public CopilotUserResponse getCopilotUser() { return copilotUser; }
    public void setCopilotUser(CopilotUserResponse copilotUser) { this.copilotUser = copilotUser; }
}
