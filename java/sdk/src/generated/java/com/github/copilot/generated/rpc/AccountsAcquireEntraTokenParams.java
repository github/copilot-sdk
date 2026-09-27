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
 * OneAuth token request supplied by a trusted host application.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountsAcquireEntraTokenParams(
    /** Public client application id. */
    @JsonProperty("clientId") String clientId,
    /** Tenant id or tenant selector, such as common or organizations. */
    @JsonProperty("tenantId") String tenantId,
    /** Broker redirect URI registered for the client. Required: the OneAuth broker validates a non-empty, registered redirect URI for the public client (MSAL broker registration), so this is not a browser-flow vestige and cannot be omitted. */
    @JsonProperty("redirectUri") String redirectUri,
    /** Exact delegated scopes to request. */
    @JsonProperty("scopes") List<String> scopes,
    /** Whether the broker may show interaction. */
    @JsonProperty("interaction") EntraTokenInteraction interaction,
    /** Previously rejected token that OneAuth must bypass during renewal. */
    @JsonProperty("accessTokenToRenew") String accessTokenToRenew
) {
}
