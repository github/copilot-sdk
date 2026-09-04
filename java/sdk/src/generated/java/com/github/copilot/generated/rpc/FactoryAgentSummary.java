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
 * Prompt-safe durable identity and live status for a direct factory agent.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FactoryAgentSummary(
    /** Stable direct-agent identifier. */
    @JsonProperty("agentId") String agentId,
    /** Tool-call identifier that launched the agent. */
    @JsonProperty("toolCallId") String toolCallId,
    /** Owning factory run identifier. */
    @JsonProperty("runId") String runId,
    /** Phase identifier active when the agent was launched, or null. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("phaseId") String phaseId,
    /** Friendly, non-unique name intended for display */
    @JsonProperty("label") String label,
    /** Friendly, non-unique name intended for display */
    @JsonProperty("displayName") String displayName,
    /** Registered agent type. */
    @JsonProperty("agentType") String agentType,
    /** Current durable or live agent status. */
    @JsonProperty("status") String status,
    /** Model requested when the agent was launched. */
    @JsonProperty("requestedModel") String requestedModel,
    /** Concrete model resolved for the agent. */
    @JsonProperty("resolvedModel") String resolvedModel,
    /** Epoch milliseconds when the agent started. */
    @JsonProperty("startedAt") Long startedAt,
    /** Epoch milliseconds when the agent completed. */
    @JsonProperty("completedAt") Long completedAt,
    /** Accumulated active agent time in milliseconds. */
    @JsonProperty("activeMs") Long activeMs,
    /** Prompt-safe live activity text. */
    @JsonProperty("activity") String activity
) {
}
