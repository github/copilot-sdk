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
 * The {@code command.execute} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CommandExecuteEvent extends SessionEvent {

    @Override
    public String getType() { return "command.execute"; }

    @JsonProperty("data")
    private CommandExecuteEventData data;

    public CommandExecuteEventData getData() { return data; }
    public void setData(CommandExecuteEventData data) { this.data = data; }

    /** Data payload for {@link CommandExecuteEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CommandExecuteEventData(
        /** Unique identifier; used to respond via session.commands.handlePendingCommand() */
        @JsonProperty("requestId") String requestId,
        /** The full command text (e.g., /deploy production) */
        @JsonProperty("command") String command,
        /** Command name without leading / */
        @JsonProperty("commandName") String commandName,
        /** Raw argument string after the command name */
        @JsonProperty("args") String args
    ) {
    }
}
