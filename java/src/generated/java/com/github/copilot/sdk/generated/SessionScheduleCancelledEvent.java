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
 * Session event "session.schedule_cancelled". Scheduled prompt cancelled from the schedule manager dialog
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionScheduleCancelledEvent extends SessionEvent {

    @Override
    public String getType() { return "session.schedule_cancelled"; }

    @JsonProperty("data")
    private SessionScheduleCancelledEventData data;

    public SessionScheduleCancelledEventData getData() { return data; }
    public void setData(SessionScheduleCancelledEventData data) { this.data = data; }

    /** Data payload for {@link SessionScheduleCancelledEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionScheduleCancelledEventData(
        /** Id of the scheduled prompt that was cancelled */
        @JsonProperty("id") Long id
    ) {
    }
}
