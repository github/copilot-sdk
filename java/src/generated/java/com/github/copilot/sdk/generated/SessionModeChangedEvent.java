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
 * The {@code session.mode_changed} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionModeChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.mode_changed"; }

    @JsonProperty("data")
    private SessionModeChangedEventData data;

    public SessionModeChangedEventData getData() { return data; }
    public void setData(SessionModeChangedEventData data) { this.data = data; }

    /** Data payload for {@link SessionModeChangedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionModeChangedEventData(
        /** Agent mode before the change (e.g., "interactive", "plan", "autopilot") */
        @JsonProperty("previousMode") String previousMode,
        /** Agent mode after the change (e.g., "interactive", "plan", "autopilot") */
        @JsonProperty("newMode") String newMode
    ) {
    }
}
