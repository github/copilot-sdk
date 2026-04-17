/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * The {@code assistant.reasoning_delta} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantReasoningDeltaEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.reasoning_delta"; }

    @JsonProperty("data")
    private AssistantReasoningDeltaEventData data;

    public AssistantReasoningDeltaEventData getData() { return data; }
    public void setData(AssistantReasoningDeltaEventData data) { this.data = data; }

    /** Data payload for {@link AssistantReasoningDeltaEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssistantReasoningDeltaEventData(
        /** Reasoning block ID this delta belongs to, matching the corresponding assistant.reasoning event */
        @JsonProperty("reasoningId") String reasoningId,
        /** Incremental text chunk to append to the reasoning content */
        @JsonProperty("deltaContent") String deltaContent
    ) {
    }
}
