/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.sdk.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * The {@code commands.changed} session event.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CommandsChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "commands.changed"; }

    @JsonProperty("data")
    private CommandsChangedEventData data;

    public CommandsChangedEventData getData() { return data; }
    public void setData(CommandsChangedEventData data) { this.data = data; }

    /** Data payload for {@link CommandsChangedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CommandsChangedEventData(
        /** Current list of registered SDK commands */
        @JsonProperty("commands") List<CommandsChangedCommand> commands
    ) {
    }
}
