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
 * Session event "assistant.fusion_phase_started". Experimental transient HydraFusion phase/model/role signal.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantFusionPhaseStartedEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.fusion_phase_started"; }

    @JsonProperty("data")
    private AssistantFusionPhaseStartedEventData data;

    public AssistantFusionPhaseStartedEventData getData() { return data; }
    public void setData(AssistantFusionPhaseStartedEventData data) { this.data = data; }

    /** Data payload for {@link AssistantFusionPhaseStartedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssistantFusionPhaseStartedEventData(
        /** Identifier of the HydraFusion turn containing the phase. */
        @JsonProperty("fusionId") String fusionId,
        /** Stable identifier for the concrete phase. */
        @JsonProperty("phaseId") String phaseId,
        /** Kind of phase being executed. */
        @JsonProperty("phaseKind") FusionPhaseKind phaseKind,
        /** HydraFusion orchestration pattern containing the phase. */
        @JsonProperty("pattern") FusionPattern pattern,
        /** Semantic role assigned to the phase. */
        @JsonProperty("role") String role,
        /** Conversation scope in which the phase executes. */
        @JsonProperty("conversationScope") FusionConversationScope conversationScope,
        /** Concrete model executing the phase. */
        @JsonProperty("model") String model
    ) {
    }
}
