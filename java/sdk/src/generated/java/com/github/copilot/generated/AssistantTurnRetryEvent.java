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
 * Session event "assistant.turn_retry". Metadata for an additional model inference attempt within an existing assistant turn
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistantTurnRetryEvent extends SessionEvent {

    @Override
    public String getType() { return "assistant.turn_retry"; }

    @JsonProperty("data")
    private AssistantTurnRetryEventData data;

    public AssistantTurnRetryEventData getData() { return data; }
    public void setData(AssistantTurnRetryEventData data) { this.data = data; }

    /** Data payload for {@link AssistantTurnRetryEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AssistantTurnRetryEventData(
        /** Identifier of the turn whose model inference is being retried */
        @JsonProperty("turnId") String turnId,
        /** Model identifier used for this retry, when known */
        @JsonProperty("model") String model,
        /** Provider or runtime classification that caused the retry, when known */
        @JsonProperty("reason") String reason
    ) {
    }
}
