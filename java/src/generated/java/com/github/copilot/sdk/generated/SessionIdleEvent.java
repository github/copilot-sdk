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
 * The {@code session.idle} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionIdleEvent extends SessionEvent {

    @Override
    public String getType() { return "session.idle"; }

    @JsonProperty("data")
    private SessionIdleEventData data;

    public SessionIdleEventData getData() { return data; }
    public void setData(SessionIdleEventData data) { this.data = data; }

    /** Data payload for {@link SessionIdleEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionIdleEventData(
        /** True when the preceding agentic loop was cancelled via abort signal */
        @JsonProperty("aborted") Boolean aborted
    ) {
    }
}
