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
 * Session event "session.plan_changed". Plan file operation details indicating what changed
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionPlanChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.plan_changed"; }

    @JsonProperty("data")
    private SessionPlanChangedEventData data;

    public SessionPlanChangedEventData getData() { return data; }
    public void setData(SessionPlanChangedEventData data) { this.data = data; }

    /** Data payload for {@link SessionPlanChangedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionPlanChangedEventData(
        /** The type of operation performed on the plan file */
        @JsonProperty("operation") PlanChangedOperation operation
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionPlanChangedEventData {}

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
            private PlanChangedOperation operation;

            /**
             * Sets the {@code operation} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder operation(PlanChangedOperation value) {
                this.operation = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionPlanChangedEventData build() {
                if (operation == null) throw new IllegalStateException("Missing required property: operation");
                return new SessionPlanChangedEventData(operation);
            }
        }
    }
}
