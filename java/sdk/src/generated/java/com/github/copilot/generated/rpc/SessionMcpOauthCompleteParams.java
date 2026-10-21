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
 * Host-delivered callback for a runtime-managed MCP OAuth login.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMcpOauthCompleteParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Opaque identifier returned by session.mcp.oauth.login for the pending external callback. */
    @JsonProperty("authorizationId") String authorizationId,
    /** Full externally visible HTTPS callback URL received by the host, including the authorization response query parameters. Applications behind a reverse proxy must reconstruct the public URL rather than passing an internal proxy URL. */
    @JsonProperty("callbackUrl") String callbackUrl
) {
}
