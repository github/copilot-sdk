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
 * Session event "command.queued". Queued slash command dispatch request for client execution
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
        @JsonProperty("command") String command,
        /** Whether a schedule dispatched this command. Consumers must reject commands marked as not schedulable before executing a scheduled request. Absent for ordinary queued commands. */
        @JsonProperty("scheduled") Boolean scheduled
    ) {

        /**
         * Retains the constructor published in Java SDK 1.0.28.
         * @deprecated Use named construction via {@link #builder()}.
         * @param requestId Unique identifier for this request; used to respond via session.respondToQueuedCommand()
         * @param command The slash command text to be executed (e.g., /help, /clear)
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public CommandQueuedEventData(
            String requestId,
            String command
        ) {
            this(requestId, command, null);
        }

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public CommandQueuedEventData {}

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
            private Boolean scheduled;

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
             * Sets the {@code scheduled} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder scheduled(Boolean value) {
                this.scheduled = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public CommandQueuedEventData build() {
                if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
                if (command == null) throw new IllegalStateException("Missing required property: command");
                return new CommandQueuedEventData(requestId, command, scheduled);
            }
        }
    }
}
