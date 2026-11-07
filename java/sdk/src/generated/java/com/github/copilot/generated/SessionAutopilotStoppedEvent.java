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
 * Session event "session.autopilot_stopped". The runtime has stopped continuing one Autopilot request. Attached background work may remain active; this is not session-wide idle.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionAutopilotStoppedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.autopilot_stopped"; }

    @JsonProperty("data")
    private SessionAutopilotStoppedEventData data;

    public SessionAutopilotStoppedEventData getData() { return data; }
    public void setData(SessionAutopilotStoppedEventData data) { this.data = data; }

    /** Data payload for {@link SessionAutopilotStoppedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionAutopilotStoppedEventData(
        /** The terminal policy reason, such as task_complete, aborted, error, or background_timeout. */
        @JsonProperty("reason") String reason,
        /** Original root user message ID for this request, absent when no root request is available. */
        @JsonProperty("requestId") String requestId
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionAutopilotStoppedEventData {}

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
            private String reason;
            private String requestId;

            /**
             * Sets the {@code reason} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder reason(String value) {
                this.reason = value;
                return this;
            }

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
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionAutopilotStoppedEventData build() {
                if (reason == null) throw new IllegalStateException("Missing required property: reason");
                return new SessionAutopilotStoppedEventData(reason, requestId);
            }
        }
    }
}
