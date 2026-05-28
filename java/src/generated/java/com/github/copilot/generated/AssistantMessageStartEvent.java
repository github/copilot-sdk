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
 * Session event "assistant.message_start". Streaming assistant message start metadata
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantMessageStartEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.message_start"; }

    @JsonProperty("data")
    private AssistantMessageStartEventData data;

    public AssistantMessageStartEventData getData() { return data; }
    public void setData(AssistantMessageStartEventData data) { this.data = data; }

    /** Data payload for {@link AssistantMessageStartEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssistantMessageStartEventData(
        /** Message ID this start event belongs to, matching subsequent deltas and assistant.message */
        @JsonProperty("messageId") String messageId,
        /** Generation phase this message belongs to for phased-output models */
        @JsonProperty("phase") String phase
    ) {
    }
}
