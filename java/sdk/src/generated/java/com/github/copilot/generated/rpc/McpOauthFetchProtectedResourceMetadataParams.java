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
 * An OAuth challenge from a remote MCP server whose protected-resource metadata should be read.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record McpOauthFetchProtectedResourceMetadataParams(
    /** URL of the remote MCP server that answered 401. */
    @JsonProperty("requestUrl") String requestUrl,
    /** Request headers the configured server sends, as a serialized JSON object. Omit to send none. */
    @JsonProperty("headersJson") String headersJson,
    /** Metadata URL named in the server's `WWW-Authenticate` challenge. Omit to try conventional metadata endpoints derived from the request URL. It is server-controlled, so it passes the same egress policy check as the request URL. */
    @JsonProperty("resourceMetadataUrl") String resourceMetadataUrl,
    /** User agent to send. Omit to use the runtime's own. */
    @JsonProperty("userAgent") String userAgent,
    /** Proxy the request must travel through. Omit to use the session's own egress settings. */
    @JsonProperty("sandboxProxy") String sandboxProxy,
    /** Locally owned session whose egress policy and sandbox proxy constrain the request. Omitted or nonlocal session IDs provide no local session egress state. */
    @JsonProperty("sessionId") String sessionId
) {
}
