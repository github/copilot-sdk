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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Variant {@code token} of {@link EntraTokenAcquireRequest}.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class EntraTokenAcquireRequestToken extends EntraTokenAcquireRequest {

    @JsonProperty("kind")
    private final String kind = "token";

    @Override
    public String getKind() { return kind; }

    /** Effective application client ID, including any resource-specific override. */
    @JsonProperty("clientId")
    private String clientId;

    /** Interaction policy. Renewal must remain silent. */
    @JsonProperty("reason")
    private EntraTokenAcquireReason reason;

    /** Pinned opaque host account ID. The response must identify this exact account. */
    @JsonProperty("accountId")
    private String accountId;

    /** Scopes required for this resource token. */
    @JsonProperty("scopes")
    private List<String> scopes;

    /** Target resource audience, when known. */
    @JsonProperty("audience")
    private String audience;

    /** Target Entra tenant, when different from the host's default. */
    @JsonProperty("tenant")
    private String tenant;

    /** Bypass cached access tokens without changing account selection or interaction policy. */
    @JsonProperty("skipCachedAccessToken")
    private Boolean skipCachedAccessToken;

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public EntraTokenAcquireReason getReason() { return reason; }
    public void setReason(EntraTokenAcquireReason reason) { this.reason = reason; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public List<String> getScopes() { return scopes; }
    public void setScopes(List<String> scopes) { this.scopes = scopes; }

    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }

    public String getTenant() { return tenant; }
    public void setTenant(String tenant) { this.tenant = tenant; }

    public Boolean getSkipCachedAccessToken() { return skipCachedAccessToken; }
    public void setSkipCachedAccessToken(Boolean skipCachedAccessToken) { this.skipCachedAccessToken = skipCachedAccessToken; }
}
