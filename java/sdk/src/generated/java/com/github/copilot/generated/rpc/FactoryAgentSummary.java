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
    @JsonProperty("agentId") String agentId,
    @JsonProperty("toolCallId") String toolCallId,
    @JsonProperty("runId") String runId,
    @JsonProperty("phaseId") String phaseId,
    @JsonProperty("label") String label,
    @JsonProperty("agentType") String agentType,
    @JsonProperty("status") String status,
    @JsonProperty("requestedModel") String requestedModel,
    @JsonProperty("resolvedModel") String resolvedModel,
    @JsonProperty("startedAt") Long startedAt,
    @JsonProperty("completedAt") Long completedAt,
    @JsonProperty("activeMs") Long activeMs,
    @JsonProperty("activity") String activity
) {
}
