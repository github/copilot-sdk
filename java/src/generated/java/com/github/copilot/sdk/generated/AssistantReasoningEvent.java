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
 * The {@code assistant.reasoning} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantReasoningEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.reasoning"; }

    @JsonProperty("data")
    private AssistantReasoningEventData data;

    public AssistantReasoningEventData getData() { return data; }
    public void setData(AssistantReasoningEventData data) { this.data = data; }

    /** Data payload for {@link AssistantReasoningEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssistantReasoningEventData(
        /** Unique identifier for this reasoning block */
        @JsonProperty("reasoningId") String reasoningId,
        /** The complete extended thinking text from the model */
        @JsonProperty("content") String content
    ) {
    }
}
