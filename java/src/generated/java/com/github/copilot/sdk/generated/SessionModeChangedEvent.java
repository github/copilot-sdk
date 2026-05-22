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
 * Session event "session.mode_changed". Agent mode change details including previous and new modes
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
        /** The session mode the agent is operating in */
        @JsonProperty("previousMode") SessionMode previousMode,
        /** The session mode the agent is operating in */
        @JsonProperty("newMode") SessionMode newMode
    ) {
    }
}
