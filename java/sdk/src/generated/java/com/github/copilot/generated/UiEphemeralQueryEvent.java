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
 * Session event "ui.ephemeral_query". Ordered output and terminal state for a transient query that does not modify conversation history.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class UiEphemeralQueryEvent extends SessionEvent {

    @Override
    public String getType() { return "ui.ephemeral_query"; }

    @JsonProperty("data")
    private UiEphemeralQueryEventData data;

    public UiEphemeralQueryEventData getData() { return data; }
    public void setData(UiEphemeralQueryEventData data) { this.data = data; }

    /** Data payload for {@link UiEphemeralQueryEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UiEphemeralQueryEventData(
        /** Runtime-minted query identifier. */
        @JsonProperty("requestId") String requestId,
        /** Current query lifecycle phase. */
        @JsonProperty("phase") UIEphemeralQueryPhase phase,
        /** Ordered text delta, present for the `chunk` phase. */
        @JsonProperty("chunk") String chunk,
        /** Full response text, present for the `completed` phase. */
        @JsonProperty("answer") String answer,
        /** Model or transport failure message, present for the `failed` phase. */
        @JsonProperty("error") String error
    ) {
    }
}
