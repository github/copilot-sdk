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
 * The {@code user_input.completed} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class UserInputCompletedEvent extends SessionEvent {

    @Override
    public String getType() { return "user_input.completed"; }

    @JsonProperty("data")
    private UserInputCompletedEventData data;

    public UserInputCompletedEventData getData() { return data; }
    public void setData(UserInputCompletedEventData data) { this.data = data; }

    /** Data payload for {@link UserInputCompletedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UserInputCompletedEventData(
        /** Request ID of the resolved user input request; clients should dismiss any UI for this request */
        @JsonProperty("requestId") String requestId,
        /** The user's answer to the input request */
        @JsonProperty("answer") String answer,
        /** Whether the answer was typed as free-form text rather than selected from choices */
        @JsonProperty("wasFreeform") Boolean wasFreeform
    ) {
    }
}
