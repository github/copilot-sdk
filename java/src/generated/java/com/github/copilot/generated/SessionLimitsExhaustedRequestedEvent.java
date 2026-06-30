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
 * Session event "session_limits_exhausted.requested". Session limit exhaustion notification requiring user action.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionLimitsExhaustedRequestedEvent extends SessionEvent {

    @Override
    public String getType() { return "session_limits_exhausted.requested"; }

    @JsonProperty("data")
    private SessionLimitsExhaustedRequestedEventData data;

    public SessionLimitsExhaustedRequestedEventData getData() { return data; }
    public void setData(SessionLimitsExhaustedRequestedEventData data) { this.data = data; }

    /** Data payload for {@link SessionLimitsExhaustedRequestedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionLimitsExhaustedRequestedEventData(
        /** Unique identifier for this request; used to respond via session.ui.handlePendingSessionLimitsExhausted(). */
        @JsonProperty("requestId") String requestId,
        /** AI Credits already consumed in the current accounting window. */
        @JsonProperty("usedAiCredits") Double usedAiCredits,
        /** Configured max AI Credits for the current accounting window. */
        @JsonProperty("maxAiCredits") Double maxAiCredits
    ) {
    }
}
