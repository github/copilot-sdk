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
 * Session event "assistant.fusion_phase_failed". Experimental durable typed HydraFusion phase failure and degradation transition.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantFusionPhaseFailedEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.fusion_phase_failed"; }

    @JsonProperty("data")
    private AssistantFusionPhaseFailedEventData data;

    public AssistantFusionPhaseFailedEventData getData() { return data; }
    public void setData(AssistantFusionPhaseFailedEventData data) { this.data = data; }

    /** Data payload for {@link AssistantFusionPhaseFailedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssistantFusionPhaseFailedEventData(
        /** Identifier of the HydraFusion turn containing the phase. */
        @JsonProperty("fusionId") String fusionId,
        /** Stable identifier for the failed phase. */
        @JsonProperty("phaseId") String phaseId,
        /** Kind of phase that failed. */
        @JsonProperty("phaseKind") FusionPhaseKind phaseKind,
        /** Semantic role assigned to the failed phase. */
        @JsonProperty("role") String role,
        /** Conversation scope in which the phase executed. */
        @JsonProperty("conversationScope") FusionConversationScope conversationScope,
        /** Concrete model that attempted the phase. */
        @JsonProperty("model") String model,
        /** Durable outcome status of the phase. */
        @JsonProperty("status") FusionPhaseStatus status,
        /** Stable machine-readable reason for the phase failure. */
        @JsonProperty("reason") String reason,
        /** Elapsed execution time before the phase failed, in milliseconds. */
        @JsonProperty("durationMs") Double durationMs,
        /** Aggregate concrete-model usage consumed before the failure. */
        @JsonProperty("usage") FusionPhaseUsage usage,
        /** Provider or execution error detail, when available. */
        @JsonProperty("errorMessage") String errorMessage,
        /** Identifier of the fallback phase used to continue the turn after degradation. */
        @JsonProperty("degradedToPhaseId") String degradedToPhaseId
    ) {
    }
}
