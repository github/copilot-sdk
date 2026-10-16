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
 * Session event "human_response.recorded". Durable request-correlated evidence for a typed response to a runtime-owned question or plan review.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class HumanResponseRecordedEvent extends SessionEvent {

    @Override
    public String getType() { return "human_response.recorded"; }

    @JsonProperty("data")
    private HumanResponseRecordedEventData data;

    public HumanResponseRecordedEventData getData() { return data; }
    public void setData(HumanResponseRecordedEventData data) { this.data = data; }

    /** Data payload for {@link HumanResponseRecordedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record HumanResponseRecordedEventData(
        /** Request ID of the runtime-owned question or plan review. */
        @JsonProperty("requestId") String requestId,
        /** Tool call ID that opened the request, when present. */
        @JsonProperty("toolCallId") String toolCallId,
        /** Controlled actor provenance established at response ingress. */
        @JsonProperty("actor") HumanResponseActor actor,
        /** Typed request and response payload. */
        @JsonProperty("response") HumanResponseRecordedResponse response
    ) {
    }
}
