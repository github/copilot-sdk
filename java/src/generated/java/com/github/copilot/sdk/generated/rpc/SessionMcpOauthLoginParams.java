/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.sdk.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Request parameters for the {@code session.mcp.oauth.login} RPC method.
 *
 * @since 1.0.0
 */
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
    @JsonProperty("callbackSuccessMessage") String callbackSuccessMessage
) {
}
