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
 * Remote MCP server name and optional overrides controlling reauthentication, OAuth client display name, callback success-page copy, and static OAuth client selection.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMcpOauthLoginParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Name of the remote MCP server to authenticate */
    @JsonProperty("serverName") String serverName,
    /** When true, clears any cached OAuth token for the server and runs a full new authorization. Use when the user explicitly wants to switch accounts or believes their session is stuck. */
    @JsonProperty("forceReauth") Boolean forceReauth,
    /** Optional override for the OAuth client display name shown on the consent screen. Applies to newly registered dynamic clients only — existing registrations keep the name they were created with. When omitted, the runtime applies a neutral fallback; callers driving interactive auth should pass their own surface-specific label so the consent screen matches the product the user sees. */
    @JsonProperty("clientName") String clientName,
    /** Optional override for the body text shown on the OAuth loopback callback success page. When omitted, the runtime applies a neutral fallback; callers driving interactive auth should pass surface-specific copy telling the user where to return. */
    @JsonProperty("callbackSuccessMessage") String callbackSuccessMessage,
    /** Optional OAuth client ID override for this login. When set, the runtime uses this pre-registered static client instead of dynamic client registration. */
    @JsonProperty("clientId") String clientId,
    /** Optional OAuth client secret override for this login. The runtime treats this as an ephemeral host-owned secret, uses it for this authentication attempt and does not persist it. */
    @JsonProperty("clientSecret") String clientSecret,
    /** Optional override indicating whether the static OAuth client is public. When false, the runtime treats it as confidential and uses the per-login clientSecret if provided, otherwise retrieving the client secret from the MCP OAuth secret store. */
    @JsonProperty("publicClient") Boolean publicClient,
    /** Optional OAuth grant type override for this login. Defaults to the server configuration, or authorization_code when no grant type is specified. */
    @JsonProperty("grantType") McpOauthLoginGrantType grantType
) {
}
