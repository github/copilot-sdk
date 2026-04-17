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
 * The {@code assistant.streaming_delta} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantStreamingDeltaEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.streaming_delta"; }

    @JsonProperty("data")
    private AssistantStreamingDeltaEventData data;

    public AssistantStreamingDeltaEventData getData() { return data; }
    public void setData(AssistantStreamingDeltaEventData data) { this.data = data; }

    /** Data payload for {@link AssistantStreamingDeltaEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssistantStreamingDeltaEventData(
        /** Cumulative total bytes received from the streaming response so far */
        @JsonProperty("totalResponseSizeBytes") Double totalResponseSizeBytes
    ) {
    }
}
