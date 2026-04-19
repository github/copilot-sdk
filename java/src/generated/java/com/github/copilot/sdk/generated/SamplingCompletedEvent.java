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
 * The {@code sampling.completed} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SamplingCompletedEvent extends SessionEvent {

    @Override
    public String getType() { return "sampling.completed"; }

    @JsonProperty("data")
    private SamplingCompletedEventData data;

    public SamplingCompletedEventData getData() { return data; }
    public void setData(SamplingCompletedEventData data) { this.data = data; }

    /** Data payload for {@link SamplingCompletedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SamplingCompletedEventData(
        /** Request ID of the resolved sampling request; clients should dismiss any UI for this request */
        @JsonProperty("requestId") String requestId
    ) {
    }
}
