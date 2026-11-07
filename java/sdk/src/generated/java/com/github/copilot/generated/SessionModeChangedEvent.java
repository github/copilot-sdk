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
 * Session event "session.mode_changed". Agent mode change details including previous and new modes
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionModeChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.mode_changed"; }

    @JsonProperty("data")
    private SessionModeChangedEventData data;

    public SessionModeChangedEventData getData() { return data; }
    public void setData(SessionModeChangedEventData data) { this.data = data; }

    /** Data payload for {@link SessionModeChangedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionModeChangedEventData(
        /** The session mode the agent is operating in */
        @JsonProperty("previousMode") SessionMode previousMode,
        /** The session mode the agent is operating in */
        @JsonProperty("newMode") SessionMode newMode
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionModeChangedEventData {}

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
            private SessionMode previousMode;
            private SessionMode newMode;

            /**
             * Sets the {@code previousMode} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder previousMode(SessionMode value) {
                this.previousMode = value;
                return this;
            }

            /**
             * Sets the {@code newMode} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder newMode(SessionMode value) {
                this.newMode = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionModeChangedEventData build() {
                if (previousMode == null) throw new IllegalStateException("Missing required property: previousMode");
                if (newMode == null) throw new IllegalStateException("Missing required property: newMode");
                return new SessionModeChangedEventData(previousMode, newMode);
            }
        }
    }
}
