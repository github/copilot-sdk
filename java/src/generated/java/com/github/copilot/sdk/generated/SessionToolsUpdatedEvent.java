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
 * The {@code session.tools_updated} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionToolsUpdatedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.tools_updated"; }

    @JsonProperty("data")
    private SessionToolsUpdatedEventData data;

    public SessionToolsUpdatedEventData getData() { return data; }
    public void setData(SessionToolsUpdatedEventData data) { this.data = data; }

    /** Data payload for {@link SessionToolsUpdatedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionToolsUpdatedEventData(
        @JsonProperty("model") String model
    ) {
    }
}
