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
 * Session event "user_input.completed". User input request completion with the user's response
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class UserInputCompletedEvent extends SessionEvent {

    @Override
    public String getType() { return "user_input.completed"; }

    @JsonProperty("data")
    private UserInputCompletedEventData data;

    public UserInputCompletedEventData getData() { return data; }
    public void setData(UserInputCompletedEventData data) { this.data = data; }

    /** Data payload for {@link UserInputCompletedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UserInputCompletedEventData(
        /** Request ID of the resolved user input request; clients should dismiss any UI for this request */
        @JsonProperty("requestId") String requestId,
        /** The user's answer to the input request */
        @JsonProperty("answer") String answer,
        /** Whether the answer was typed as free-form text rather than selected from choices */
        @JsonProperty("wasFreeform") Boolean wasFreeform
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public UserInputCompletedEventData {}

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
            private String answer;
            private Boolean wasFreeform;

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
             * Sets the {@code answer} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder answer(String value) {
                this.answer = value;
                return this;
            }

            /**
             * Sets the {@code wasFreeform} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder wasFreeform(Boolean value) {
                this.wasFreeform = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public UserInputCompletedEventData build() {
                if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
                return new UserInputCompletedEventData(requestId, answer, wasFreeform);
            }
        }
    }
}
