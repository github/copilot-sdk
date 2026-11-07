/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Session event "elicitation.completed". Elicitation request completion with the user's response
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class ElicitationCompletedEvent extends SessionEvent {

    @Override
    public String getType() { return "elicitation.completed"; }

    @JsonProperty("data")
    private ElicitationCompletedEventData data;

    public ElicitationCompletedEventData getData() { return data; }
    public void setData(ElicitationCompletedEventData data) { this.data = data; }

    /** Data payload for {@link ElicitationCompletedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ElicitationCompletedEventData(
        /** Request ID of the resolved elicitation request; clients should dismiss any UI for this request */
        @JsonProperty("requestId") String requestId,
        /** The user action: "accept" (submitted form), "decline" (explicitly refused), or "cancel" (dismissed) */
        @JsonProperty("action") ElicitationCompletedAction action,
        /** The submitted form data when action is 'accept'; keys match the requested schema fields */
        @JsonProperty("content") Map<String, Object> content
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public ElicitationCompletedEventData {}

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
            private ElicitationCompletedAction action;
            private Map<String, Object> content;

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
             * Sets the {@code action} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder action(ElicitationCompletedAction value) {
                this.action = value;
                return this;
            }

            /**
             * Sets the {@code content} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder content(Map<String, Object> value) {
                this.content = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public ElicitationCompletedEventData build() {
                if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
                return new ElicitationCompletedEventData(requestId, action, content);
            }
        }
    }
}
