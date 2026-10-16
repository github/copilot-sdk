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
 * A remote MCP server whose authorization server should be asked about Dynamic Client Registration.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record McpOauthCheckDcrSupportParams(
    /** URL of the remote MCP server that answered 401. */
    @JsonProperty("serverUrl") String serverUrl,
    /** Raw protected-resource metadata text from the server's `WWW-Authenticate` challenge. Omit to discover the authorization server from the server URL instead. */
    @JsonProperty("resourceMetadataText") String resourceMetadataText,
    /** Proxy the request must travel through. Omit to use the session's own egress settings. */
    @JsonProperty("sandboxProxy") String sandboxProxy,
    /** Locally owned session whose egress policy and sandbox proxy constrain the request. Omitted or nonlocal session IDs provide no local session egress state. */
    @JsonProperty("sessionId") String sessionId
) {
}
