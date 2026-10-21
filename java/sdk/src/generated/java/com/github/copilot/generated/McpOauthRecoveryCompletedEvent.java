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
 * Session event "mcp.oauth_recovery_completed". Terminal outcome of a runtime-owned MCP OAuth recovery accepted through a runtimeRefresh handoff
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class McpOauthRecoveryCompletedEvent extends SessionEvent {

    @Override
    public String getType() { return "mcp.oauth_recovery_completed"; }

    @JsonProperty("data")
    private McpOauthRecoveryCompletedEventData data;

    public McpOauthRecoveryCompletedEventData getData() { return data; }
    public void setData(McpOauthRecoveryCompletedEventData data) { this.data = data; }

    /** Data payload for {@link McpOauthRecoveryCompletedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record McpOauthRecoveryCompletedEventData(
        /** Request ID of the MCP OAuth request whose runtimeRefresh handoff was accepted */
        @JsonProperty("requestId") String requestId,
        /** Name of the MCP server whose authorization recovery completed */
        @JsonProperty("serverName") String serverName,
        /** Terminal outcome of the original authorization reservation, including reconnect */
        @JsonProperty("outcome") McpOauthRecoveryCompletedOutcome outcome
    ) {
    }
}
