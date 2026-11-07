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
 * Session event "session.autopilot_objective_changed". Autopilot objective state file operation details indicating what changed
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionAutopilotObjectiveChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.autopilot_objective_changed"; }

    @JsonProperty("data")
    private SessionAutopilotObjectiveChangedEventData data;

    public SessionAutopilotObjectiveChangedEventData getData() { return data; }
    public void setData(SessionAutopilotObjectiveChangedEventData data) { this.data = data; }

    /** Data payload for {@link SessionAutopilotObjectiveChangedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionAutopilotObjectiveChangedEventData(
        /** The type of operation performed on the autopilot objective state file */
        @JsonProperty("operation") AutopilotObjectiveChangedOperation operation,
        /** Current autopilot objective id, if one exists */
        @JsonProperty("id") Long id,
        /** Current autopilot objective status, if one exists */
        @JsonProperty("status") AutopilotObjectiveChangedStatus status
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionAutopilotObjectiveChangedEventData {}

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
            private AutopilotObjectiveChangedOperation operation;
            private Long id;
            private AutopilotObjectiveChangedStatus status;

            /**
             * Sets the {@code operation} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder operation(AutopilotObjectiveChangedOperation value) {
                this.operation = value;
                return this;
            }

            /**
             * Sets the {@code id} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder id(Long value) {
                this.id = value;
                return this;
            }

            /**
             * Sets the {@code status} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder status(AutopilotObjectiveChangedStatus value) {
                this.status = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionAutopilotObjectiveChangedEventData build() {
                if (operation == null) throw new IllegalStateException("Missing required property: operation");
                return new SessionAutopilotObjectiveChangedEventData(operation, id, status);
            }
        }
    }
}
