/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * The {@code user_input.requested} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class UserInputRequestedEvent extends SessionEvent {

    @Override
    public String getType() { return "user_input.requested"; }

    @JsonProperty("data")
    private UserInputRequestedEventData data;

    public UserInputRequestedEventData getData() { return data; }
    public void setData(UserInputRequestedEventData data) { this.data = data; }

    /** Data payload for {@link UserInputRequestedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UserInputRequestedEventData(
        /** Unique identifier for this input request; used to respond via session.respondToUserInput() */
        @JsonProperty("requestId") String requestId,
        /** The question or prompt to present to the user */
        @JsonProperty("question") String question,
        /** Predefined choices for the user to select from, if applicable */
        @JsonProperty("choices") List<String> choices,
        /** Whether the user can provide a free-form text response in addition to predefined choices */
        @JsonProperty("allowFreeform") Boolean allowFreeform,
        /** The LLM-assigned tool call ID that triggered this request; used by remote UIs to correlate responses */
        @JsonProperty("toolCallId") String toolCallId
    ) {
    }
}
