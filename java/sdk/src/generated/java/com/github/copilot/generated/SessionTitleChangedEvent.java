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
 * Session event "session.title_changed". Session title change payload containing the new display title
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionTitleChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.title_changed"; }

    @JsonProperty("data")
    private SessionTitleChangedEventData data;

    public SessionTitleChangedEventData getData() { return data; }
    public void setData(SessionTitleChangedEventData data) { this.data = data; }

    /** Data payload for {@link SessionTitleChangedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionTitleChangedEventData(
        /** The new display title for the session */
        @JsonProperty("title") String title
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionTitleChangedEventData {}

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
            private String title;

            /**
             * Sets the {@code title} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder title(String value) {
                this.title = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionTitleChangedEventData build() {
                if (title == null) throw new IllegalStateException("Missing required property: title");
                return new SessionTitleChangedEventData(title);
            }
        }
    }
}
