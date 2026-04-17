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
 * The {@code assistant.intent} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantIntentEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.intent"; }

    @JsonProperty("data")
    private AssistantIntentEventData data;

    public AssistantIntentEventData getData() { return data; }
    public void setData(AssistantIntentEventData data) { this.data = data; }

    /** Data payload for {@link AssistantIntentEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssistantIntentEventData(
        /** Short description of what the agent is currently doing or planning to do */
        @JsonProperty("intent") String intent
    ) {
    }
}
