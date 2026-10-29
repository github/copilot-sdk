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
 * Session event "session.mode_notice_delivered". Records that a mode transition notice reached the model so cache-stable mode tools can remain offered across resume.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionModeNoticeDeliveredEvent extends SessionEvent {

    @Override
    public String getType() { return "session.mode_notice_delivered"; }

    @JsonProperty("data")
    private SessionModeNoticeDeliveredEventData data;

    public SessionModeNoticeDeliveredEventData getData() { return data; }
    public void setData(SessionModeNoticeDeliveredEventData data) { this.data = data; }

    /** Data payload for {@link SessionModeNoticeDeliveredEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionModeNoticeDeliveredEventData(
        /** Mode established by the delivered transition notice */
        @JsonProperty("mode") SessionMode mode,
        /** Model-visible transition notice persisted for a mid-turn delivery */
        @JsonProperty("content") String content
    ) {
    }
}
