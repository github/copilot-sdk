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
 * Session event "session.session_limits_changed". Session limits update details. Null clears the limits.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionSessionLimitsChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.session_limits_changed"; }

    @JsonProperty("data")
    private SessionSessionLimitsChangedEventData data;

    public SessionSessionLimitsChangedEventData getData() { return data; }
    public void setData(SessionSessionLimitsChangedEventData data) { this.data = data; }

    /** Data payload for {@link SessionSessionLimitsChangedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionSessionLimitsChangedEventData(
        /** Current session limits, or null when no limits are active */
        @JsonProperty("sessionLimits") SessionLimitsConfig sessionLimits
    ) {
    }
}
