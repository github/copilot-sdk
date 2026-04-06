/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event: command.execute
 * <p>
 * Broadcast when the user executes a slash command registered by this client.
 * Clients that have a matching command handler should respond via
 * {@code session.commands.handlePendingCommand}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CommandExecuteEvent extends AbstractSessionEvent {

    @JsonProperty("data")
    private CommandExecuteData data;

    @Override
    public String getType() {
        return "command.execute";
    }

    public CommandExecuteData getData() {
        return data;
    }

    public void setData(CommandExecuteData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CommandExecuteData(@JsonProperty("requestId") String requestId,
            @JsonProperty("command") String command, @JsonProperty("commandName") String commandName,
            @JsonProperty("args") String args) {
    }
}
