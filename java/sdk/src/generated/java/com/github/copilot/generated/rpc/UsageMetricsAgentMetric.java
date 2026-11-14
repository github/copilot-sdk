/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Usage attributed to one agent instance, including its identity, API duration, AI units, and per-model breakdown.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsageMetricsAgentMetric(
    /** Configured agent name, when this is a subagent */
    @JsonProperty("agentName") String agentName,
    /** Human-readable label for this subagent invocation, copied from the originating `subagent.started` event. For task-tool subagents this is the invocation's task description rather than the agent's configured display name, so group by `agentName` for stable per-agent labels. */
    @JsonProperty("agentDisplayName") String agentDisplayName,
    /** Time spent in model API calls by this agent, in milliseconds */
    @JsonProperty("totalApiDurationMs") Long totalApiDurationMs,
    /** Accumulated nano-AI units cost for this agent */
    @JsonProperty("totalNanoAiu") Double totalNanoAiu,
    /** Per-model usage for this agent, keyed by model identifier */
    @JsonProperty("modelMetrics") Map<String, UsageMetricsModelMetric> modelMetrics
) {
}
