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
 * The {@code command.completed} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CommandCompletedEvent extends SessionEvent {

    @Override
    public String getType() { return "command.completed"; }

    @JsonProperty("data")
    private CommandCompletedEventData data;

    public CommandCompletedEventData getData() { return data; }
    public void setData(CommandCompletedEventData data) { this.data = data; }

    /** Data payload for {@link CommandCompletedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CommandCompletedEventData(
        /** Request ID of the resolved command request; clients should dismiss any UI for this request */
        @JsonProperty("requestId") String requestId
    ) {
    }
}
