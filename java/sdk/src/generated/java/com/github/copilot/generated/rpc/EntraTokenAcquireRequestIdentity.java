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
 * Variant {@code identity} of {@link EntraTokenAcquireRequest}.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class EntraTokenAcquireRequestIdentity extends EntraTokenAcquireRequest {

    @JsonProperty("kind")
    private final String kind = "identity";

    @Override
    public String getKind() { return kind; }

    /** Effective application client ID. Treat it as opaque. */
    @JsonProperty("clientId")
    private String clientId;

    /** Interaction policy. Only a fresh explicit sign-in may select an unpinned account. */
    @JsonProperty("reason")
    private EntraTokenAcquireReason reason;

    /** Host account to revalidate. When supplied, the response must identify this exact account. */
    @JsonProperty("accountId")
    private String accountId;

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public EntraTokenAcquireReason getReason() { return reason; }
    public void setReason(EntraTokenAcquireReason reason) { this.reason = reason; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
}
