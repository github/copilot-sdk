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
 * MCP OAuth request id and optional provider response.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionMcpOauthRespondParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** OAuth request identifier from mcp.oauth_required */
    @JsonProperty("requestId") String requestId,
    /** In-process OAuthClientProvider instance, or omitted to deny. Marked internal: cannot be serialized across the JSON-RPC boundary. */
    @JsonProperty("provider") Object provider
) {
}
