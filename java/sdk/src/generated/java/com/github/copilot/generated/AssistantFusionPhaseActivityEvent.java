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
 * Session event "assistant.fusion_phase_activity". Experimental content-safe activity signal for a running HydraFusion phase.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantFusionPhaseActivityEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.fusion_phase_activity"; }

    @JsonProperty("data")
    private AssistantFusionPhaseActivityEventData data;

    public AssistantFusionPhaseActivityEventData getData() { return data; }
    public void setData(AssistantFusionPhaseActivityEventData data) { this.data = data; }

    /** Data payload for {@link AssistantFusionPhaseActivityEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssistantFusionPhaseActivityEventData(
        /** Identifier of the HydraFusion turn containing the phase. */
        @JsonProperty("fusionId") String fusionId,
        /** Stable identifier for the concrete phase. */
        @JsonProperty("phaseId") String phaseId,
        /** Kind of phase currently executing. */
        @JsonProperty("phaseKind") FusionPhaseKind phaseKind,
        /** HydraFusion orchestration pattern containing the phase. */
        @JsonProperty("pattern") FusionPattern pattern,
        /** Semantic role assigned to the phase. */
        @JsonProperty("role") String role,
        /** Conversation scope in which the phase executes. */
        @JsonProperty("conversationScope") FusionConversationScope conversationScope,
        /** Kind of real activity observed. */
        @JsonProperty("activity") FusionPhaseActivityKind activity,
        /** Cumulative private response bytes observed for this model call. The event never includes response text. */
        @JsonProperty("totalResponseSizeBytes") Long totalResponseSizeBytes,
        /** Opaque hashed correlation token for matching tool-started and tool-completed activity within this Fusion activity stream. It is not the tool call identifier exposed by tool lifecycle events. */
        @JsonProperty("toolCallId") String toolCallId
    ) {
    }
}
