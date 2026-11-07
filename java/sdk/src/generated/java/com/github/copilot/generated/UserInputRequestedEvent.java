/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Session event "user_input.requested". User input request notification with question and optional predefined choices
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class UserInputRequestedEvent extends SessionEvent {

    @Override
    public String getType() { return "user_input.requested"; }

    @JsonProperty("data")
    private UserInputRequestedEventData data;

    public UserInputRequestedEventData getData() { return data; }
    public void setData(UserInputRequestedEventData data) { this.data = data; }

    /** Data payload for {@link UserInputRequestedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UserInputRequestedEventData(
        /** Unique identifier for this input request; used to respond via session.respondToUserInput() */
        @JsonProperty("requestId") String requestId,
        /** The question or prompt to present to the user */
        @JsonProperty("question") String question,
        /** Predefined choices for the user to select from, if applicable */
        @JsonProperty("choices") List<String> choices,
        /** Whether the user can provide a free-form text response in addition to predefined choices */
        @JsonProperty("allowFreeform") Boolean allowFreeform,
        /** The LLM-assigned tool call ID that triggered this request; used by remote UIs to correlate responses */
        @JsonProperty("toolCallId") String toolCallId
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public UserInputRequestedEventData {}

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
            private String question;
            private List<String> choices;
            private Boolean allowFreeform;
            private String toolCallId;

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
             * Sets the {@code question} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder question(String value) {
                this.question = value;
                return this;
            }

            /**
             * Sets the {@code choices} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder choices(List<String> value) {
                this.choices = value;
                return this;
            }

            /**
             * Sets the {@code allowFreeform} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder allowFreeform(Boolean value) {
                this.allowFreeform = value;
                return this;
            }

            /**
             * Sets the {@code toolCallId} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolCallId(String value) {
                this.toolCallId = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public UserInputRequestedEventData build() {
                if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
                if (question == null) throw new IllegalStateException("Missing required property: question");
                return new UserInputRequestedEventData(requestId, question, choices, allowFreeform, toolCallId);
            }
        }
    }
}
