/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Session event "mcp.oauth_required". OAuth authentication request for an MCP server
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class McpOauthRequiredEvent extends SessionEvent {

    @Override
    public String getType() { return "mcp.oauth_required"; }

    @JsonProperty("data")
    private McpOauthRequiredEventData data;

    public McpOauthRequiredEventData getData() { return data; }
    public void setData(McpOauthRequiredEventData data) { this.data = data; }

    /** Data payload for {@link McpOauthRequiredEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record McpOauthRequiredEventData(
        /** Unique identifier for this OAuth request; used to respond via session.mcp.oauth.handlePendingRequest */
        @JsonProperty("requestId") String requestId,
        /** Display name of the MCP server that requires OAuth */
        @JsonProperty("serverName") String serverName,
        /** URL of the MCP server that requires OAuth */
        @JsonProperty("serverUrl") String serverUrl,
        /** Static OAuth client configuration, if the server specifies one */
        @JsonProperty("staticClientConfig") McpOauthRequiredStaticClientConfig staticClientConfig,
        /** OAuth WWW-Authenticate parameters parsed from the auth challenge, if available */
        @JsonProperty("wwwAuthenticateParams") McpOauthWWWAuthenticateParams wwwAuthenticateParams,
        /** Raw OAuth protected-resource metadata document fetched for the MCP server, if available */
        @JsonProperty("resourceMetadata") String resourceMetadata
    ) {
    }
}
