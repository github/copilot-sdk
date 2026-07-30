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
 * Session event "session_limits_exhausted.completed". Session limit exhaustion prompt completion notification.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionLimitsExhaustedCompletedEvent extends SessionEvent {

    @Override
    public String getType() { return "session_limits_exhausted.completed"; }

    @JsonProperty("data")
    private SessionLimitsExhaustedCompletedEventData data;

    public SessionLimitsExhaustedCompletedEventData getData() { return data; }
    public void setData(SessionLimitsExhaustedCompletedEventData data) { this.data = data; }

    /** Data payload for {@link SessionLimitsExhaustedCompletedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionLimitsExhaustedCompletedEventData(
        /** Request ID of the resolved request; clients should dismiss any UI for this request. */
        @JsonProperty("requestId") String requestId,
        /** The user's selected session-limit action. */
        @JsonProperty("response") SessionLimitsExhaustedResponse response
    ) {
    }
}
