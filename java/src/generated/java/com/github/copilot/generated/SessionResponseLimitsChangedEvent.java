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
 * Session event "session.response_limits_changed". Response limits update details. Null clears the limits.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionResponseLimitsChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.response_limits_changed"; }

    @JsonProperty("data")
    private SessionResponseLimitsChangedEventData data;

    public SessionResponseLimitsChangedEventData getData() { return data; }
    public void setData(SessionResponseLimitsChangedEventData data) { this.data = data; }

    /** Data payload for {@link SessionResponseLimitsChangedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionResponseLimitsChangedEventData(
        /** Current response limits for the session, or null when no limits are active */
        @JsonProperty("responseLimits") ResponseLimitsConfig responseLimits
    ) {
    }
}
