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
 * Session event "assistant.fusion_phase_completed". Experimental durable HydraFusion phase output and lossless replay checkpoint.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantFusionPhaseCompletedEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.fusion_phase_completed"; }

    @JsonProperty("data")
    private AssistantFusionPhaseCompletedEventData data;

    public AssistantFusionPhaseCompletedEventData getData() { return data; }
    public void setData(AssistantFusionPhaseCompletedEventData data) { this.data = data; }

    /** Data payload for {@link AssistantFusionPhaseCompletedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssistantFusionPhaseCompletedEventData(
        /** Identifier of the HydraFusion turn containing the phase. */
        @JsonProperty("fusionId") String fusionId,
        /** Stable identifier for the completed phase. */
        @JsonProperty("phaseId") String phaseId,
        /** Kind of phase that completed. */
        @JsonProperty("phaseKind") FusionPhaseKind phaseKind,
        /** Semantic role assigned to the completed phase. */
        @JsonProperty("role") String role,
        /** Conversation scope in which the phase executed. */
        @JsonProperty("conversationScope") FusionConversationScope conversationScope,
        /** Concrete model that executed the phase. */
        @JsonProperty("model") String model,
        /** Durable outcome status of the phase. */
        @JsonProperty("status") FusionPhaseStatus status,
        /** Provider-normalized textual output produced by the phase. */
        @JsonProperty("content") String content,
        /** Structured judge or critic verdict, when the phase produces one. */
        @JsonProperty("verdict") String verdict,
        /** Elapsed execution time for the phase in milliseconds. */
        @JsonProperty("durationMs") Double durationMs,
        /** Aggregate concrete-model usage consumed by the phase. */
        @JsonProperty("usage") FusionPhaseUsage usage,
        /** Exact provider-normalized message used to reconstruct canonical model history. */
        @JsonProperty("projectionMessage") Object projectionMessage,
        /** Projection action for the exact internal message. */
        @JsonProperty("projectionMode") FusionProjectionMode projectionMode,
        /** Terminal request held outside canonical state until selected by the final commit. */
        @JsonProperty("stagedTerminal") FusionStagedTerminal stagedTerminal
    ) {
    }
}
