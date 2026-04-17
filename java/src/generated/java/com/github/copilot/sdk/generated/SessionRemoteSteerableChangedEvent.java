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
 * The {@code session.remote_steerable_changed} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionRemoteSteerableChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.remote_steerable_changed"; }

    @JsonProperty("data")
    private SessionRemoteSteerableChangedEventData data;

    public SessionRemoteSteerableChangedEventData getData() { return data; }
    public void setData(SessionRemoteSteerableChangedEventData data) { this.data = data; }

    /** Data payload for {@link SessionRemoteSteerableChangedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionRemoteSteerableChangedEventData(
        /** Whether this session now supports remote steering via Mission Control */
        @JsonProperty("remoteSteerable") Boolean remoteSteerable
    ) {
    }
}
