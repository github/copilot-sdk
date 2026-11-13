/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * External tool call of the session or one of its sub-agents that is still waiting for session.tools.handlePendingToolCall.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PendingExternalToolRequest(
    /** Request ID to pass to session.tools.handlePendingToolCall */
    @JsonProperty("requestId") String requestId,
    /** Tool call ID assigned to this external tool invocation */
    @JsonProperty("toolCallId") String toolCallId,
    /** Name of the external tool to invoke */
    @JsonProperty("toolName") String toolName,
    /** Arguments to pass to the external tool */
    @JsonProperty("arguments") Object arguments,
    /** Stable identity of the provider that offered the tool, for hosts that route extension-owned tools by provider */
    @JsonProperty("providerId") String providerId,
    /** Sub-agent instance identifier (the envelope agentId of its events) of the agent that issued the call; absent for calls issued by the root agent */
    @JsonProperty("agentId") String agentId
) {
}
