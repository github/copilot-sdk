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
 * The {@code command.queued} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CommandQueuedEvent extends SessionEvent {

    @Override
    public String getType() { return "command.queued"; }

    @JsonProperty("data")
    private CommandQueuedEventData data;

    public CommandQueuedEventData getData() { return data; }
    public void setData(CommandQueuedEventData data) { this.data = data; }

    /** Data payload for {@link CommandQueuedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CommandQueuedEventData(
        /** Unique identifier for this request; used to respond via session.respondToQueuedCommand() */
        @JsonProperty("requestId") String requestId,
        /** The slash command text to be executed (e.g., /help, /clear) */
        @JsonProperty("command") String command
    ) {
    }
}
