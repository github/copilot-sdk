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
 * Session event "session.remote_steerable_changed". Notifies that the session's remote steering capability has changed
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionRemoteSteerableChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.remote_steerable_changed"; }

    @JsonProperty("data")
    private SessionRemoteSteerableChangedEventData data;

    public SessionRemoteSteerableChangedEventData getData() { return data; }
    public void setData(SessionRemoteSteerableChangedEventData data) { this.data = data; }

    /** Data payload for {@link SessionRemoteSteerableChangedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionRemoteSteerableChangedEventData(
        /** Whether this session now supports remote steering via GitHub */
        @JsonProperty("remoteSteerable") Boolean remoteSteerable
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionRemoteSteerableChangedEventData {}

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
            private Boolean remoteSteerable;

            /**
             * Sets the {@code remoteSteerable} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder remoteSteerable(Boolean value) {
                this.remoteSteerable = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionRemoteSteerableChangedEventData build() {
                if (remoteSteerable == null) throw new IllegalStateException("Missing required property: remoteSteerable");
                return new SessionRemoteSteerableChangedEventData(remoteSteerable);
            }
        }
    }
}
