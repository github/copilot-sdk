/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Session event "commands.changed". Session command list change notification, for SDK command registration or built-in command availability; `session.commands.list` returns the full list
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

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public CommandsChangedEventData {}

        /**
         * Returns a named builder.
         * @return a new builder
         * @since 2.0.0
         */
        public static Builder builder() { return new Builder(); }

        /**
         * Named construction for this record.
         * @since 2.0.0
         */
        public static final class Builder {
            private Builder() {}
            private List<CommandsChangedCommand> commands;

            /**
             * Sets the {@code commands} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder commands(List<CommandsChangedCommand> value) {
                this.commands = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public CommandsChangedEventData build() {
                if (commands == null) throw new IllegalStateException("Missing required property: commands");
                return new CommandsChangedEventData(commands);
            }
        }
    }
}
