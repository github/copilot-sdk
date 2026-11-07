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
 * Session event "human_response.recorded". Durable request-correlated evidence for a typed response to a runtime-owned question or plan review.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class HumanResponseRecordedEvent extends SessionEvent {

    @Override
    public String getType() { return "human_response.recorded"; }

    @JsonProperty("data")
    private HumanResponseRecordedEventData data;

    public HumanResponseRecordedEventData getData() { return data; }
    public void setData(HumanResponseRecordedEventData data) { this.data = data; }

    /** Data payload for {@link HumanResponseRecordedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record HumanResponseRecordedEventData(
        /** Request ID of the runtime-owned question or plan review. */
        @JsonProperty("requestId") String requestId,
        /** Tool call ID that opened the request, when present. */
        @JsonProperty("toolCallId") String toolCallId,
        /** Controlled actor provenance established at response ingress. */
        @JsonProperty("actor") HumanResponseActor actor,
        /** Typed request and response payload. */
        @JsonProperty("response") HumanResponseRecordedResponse response
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public HumanResponseRecordedEventData {}

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
            private String toolCallId;
            private HumanResponseActor actor;
            private HumanResponseRecordedResponse response;

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
             * Sets the {@code actor} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder actor(HumanResponseActor value) {
                this.actor = value;
                return this;
            }

            /**
             * Sets the {@code response} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder response(HumanResponseRecordedResponse value) {
                this.response = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public HumanResponseRecordedEventData build() {
                if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
                if (actor == null) throw new IllegalStateException("Missing required property: actor");
                if (response == null) throw new IllegalStateException("Missing required property: response");
                return new HumanResponseRecordedEventData(requestId, toolCallId, actor, response);
            }
        }
    }
}
