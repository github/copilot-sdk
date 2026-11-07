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
 * Session event "session.idle". Payload indicating the session is idle with no background agents or attached shell commands in flight
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionIdleEvent extends SessionEvent {

    @Override
    public String getType() { return "session.idle"; }

    @JsonProperty("data")
    private SessionIdleEventData data;

    public SessionIdleEventData getData() { return data; }
    public void setData(SessionIdleEventData data) { this.data = data; }

    /** Data payload for {@link SessionIdleEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionIdleEventData(
        /** True when the preceding agentic loop was cancelled via abort signal */
        @JsonProperty("aborted") Boolean aborted,
        /** The session mode the agent was operating in when it went idle, when the mode is known. Lets turn-scoped consumers distinguish an autopilot continuation boundary (where the agent keeps working after this idle) from a genuine turn completion. */
        @JsonProperty("mode") SessionMode mode
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionIdleEventData {}

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
            private Boolean aborted;
            private SessionMode mode;

            /**
             * Sets the {@code aborted} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder aborted(Boolean value) {
                this.aborted = value;
                return this;
            }

            /**
             * Sets the {@code mode} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder mode(SessionMode value) {
                this.mode = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionIdleEventData build() {
                return new SessionIdleEventData(aborted, mode);
            }
        }
    }
}
