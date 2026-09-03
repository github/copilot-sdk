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
 * Session event "subagent.failed". Sub-agent failure details including error message and agent information
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SubagentFailedEvent extends SessionEvent {

    @Override
    public String getType() { return "subagent.failed"; }

    @JsonProperty("data")
    private SubagentFailedEventData data;

    public SubagentFailedEventData getData() { return data; }
    public void setData(SubagentFailedEventData data) { this.data = data; }

    /** Data payload for {@link SubagentFailedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SubagentFailedEventData(
        /** Tool call ID of the parent tool invocation that spawned this sub-agent */
        @JsonProperty("toolCallId") String toolCallId,
        /** Internal name of the sub-agent */
        @JsonProperty("agentName") String agentName,
        /** Human-readable display name of the sub-agent */
        @JsonProperty("agentDisplayName") String agentDisplayName,
        /** Error message describing why the sub-agent failed */
        @JsonProperty("error") String error,
        /** Model selected for the sub-agent, when known */
        @JsonProperty("model") String model,
        /** First model for which the sub-agent started an inference request, when one was dispatched */
        @JsonProperty("firstDispatchedModel") String firstDispatchedModel,
        /** Concrete model the user configured for this sub-agent via `/subagents`, when present */
        @JsonProperty("configuredModelPreference") String configuredModelPreference,
        /** Explicit model supplied by the parent agent on the task call, when present */
        @JsonProperty("explicitModelOverride") String explicitModelOverride,
        /** Whether the explicit task-call model matched the user's configured preference */
        @JsonProperty("explicitModelMatchesPreference") Boolean explicitModelMatchesPreference,
        /** Why an explicit task-call model did not become the effective model */
        @JsonProperty("modelOverrideReason") String modelOverrideReason,
        /** Whether the first model actually dispatched matched the user's configured preference */
        @JsonProperty("configuredModelMatchesActual") Boolean configuredModelMatchesActual,
        /** Total number of tool calls made before the sub-agent failed */
        @JsonProperty("totalToolCalls") Long totalToolCalls,
        /** Total tokens (input + output) consumed before the sub-agent failed */
        @JsonProperty("totalTokens") Long totalTokens,
        /** Wall-clock duration of the sub-agent execution in milliseconds */
        @JsonProperty("durationMs") Long durationMs
    ) {
    }
}
