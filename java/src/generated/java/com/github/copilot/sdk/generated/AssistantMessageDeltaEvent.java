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
 * The {@code assistant.message_delta} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantMessageDeltaEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.message_delta"; }

    @JsonProperty("data")
    private AssistantMessageDeltaEventData data;

    public AssistantMessageDeltaEventData getData() { return data; }
    public void setData(AssistantMessageDeltaEventData data) { this.data = data; }

    /** Data payload for {@link AssistantMessageDeltaEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssistantMessageDeltaEventData(
        /** Message ID this delta belongs to, matching the corresponding assistant.message event */
        @JsonProperty("messageId") String messageId,
        /** Incremental text chunk to append to the message content */
        @JsonProperty("deltaContent") String deltaContent,
        /** Tool call ID of the parent tool invocation when this event originates from a sub-agent */
        @JsonProperty("parentToolCallId") String parentToolCallId
    ) {
    }
}
