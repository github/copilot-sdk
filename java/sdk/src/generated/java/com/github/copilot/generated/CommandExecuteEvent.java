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
 * Session event "command.execute". Registered command dispatch request routed to the owning client
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

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public CommandExecuteEventData {}

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
            private String requestId;
            private String command;
            private String commandName;
            private String args;

            /**
             * Sets the {@code requestId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder requestId(String value) {
                this.requestId = value;
                return this;
            }

            /**
             * Sets the {@code command} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder command(String value) {
                this.command = value;
                return this;
            }

            /**
             * Sets the {@code commandName} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder commandName(String value) {
                this.commandName = value;
                return this;
            }

            /**
             * Sets the {@code args} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder args(String value) {
                this.args = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public CommandExecuteEventData build() {
                if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
                if (command == null) throw new IllegalStateException("Missing required property: command");
                if (commandName == null) throw new IllegalStateException("Missing required property: commandName");
                if (args == null) throw new IllegalStateException("Missing required property: args");
                return new CommandExecuteEventData(requestId, command, commandName, args);
            }
        }
    }
}
