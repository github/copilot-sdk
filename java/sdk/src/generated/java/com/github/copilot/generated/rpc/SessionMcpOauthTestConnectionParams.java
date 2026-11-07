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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Tokenless initialize handshake for a possibly unsaved remote MCP server.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionMcpOauthTestConnectionParams(
    /** Target session identifier */
    @JsonProperty("sessionId") String sessionId,
    /** Remote endpoint to initialize without an OAuth token. */
    @JsonProperty("url") String url,
    /** Transport. Omission selects Streamable HTTP. */
    @JsonProperty("type") McpOauthTestConnectionTransport type,
    /** Headers sent during the handshake. */
    @JsonProperty("headers") Map<String, String> headers,
    /** Proxy fallback when no session egress entry exists. */
    @JsonProperty("sandboxProxy") String sandboxProxy
) {
}
