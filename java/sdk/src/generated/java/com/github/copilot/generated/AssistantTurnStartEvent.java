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
 * Session event "assistant.turn_start". Turn initialization metadata including identifier and interaction tracking
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantTurnStartEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.turn_start"; }

    @JsonProperty("data")
    private AssistantTurnStartEventData data;

    public AssistantTurnStartEventData getData() { return data; }
    public void setData(AssistantTurnStartEventData data) { this.data = data; }

    /** Data payload for {@link AssistantTurnStartEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssistantTurnStartEventData(
        /** Optional bounded worker observations. Missing or invalid metadata is unavailable, not known-empty. */
        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.github.copilot.WorkerCausalityDeserializer.class)
        @JsonProperty("workerCausality") WorkerCausality workerCausality,
        /** Identifier for this turn within the agentic loop, typically a stringified turn number */
        @JsonProperty("turnId") String turnId,
        /** Model identifier used for this turn, when known */
        @JsonProperty("model") String model,
        /** CAPI interaction ID for correlating this turn with upstream telemetry */
        @JsonProperty("interactionId") String interactionId
    ) {
        /** Creates a value without optional worker diagnostics. */
        public AssistantTurnStartEventData(String turnId, String model, String interactionId) {
            this(null, turnId, model, interactionId);
        }
    }
}
