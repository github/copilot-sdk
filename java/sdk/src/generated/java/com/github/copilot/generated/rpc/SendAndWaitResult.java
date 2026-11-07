/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.generated.SessionEvent;
import javax.annotation.processing.Generated;

/**
 * Internal request-correlated completion of one submitted message.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SendAndWaitResult(
    /** Identifier assigned to the submitted user message. */
    @JsonProperty("messageId") String messageId,
    /** The exact existing root assistant event owned by this request. Absent when the request completed without an assistant response. */
    @JsonProperty("response") SessionEvent response,
    /** Whether the request was aborted or removed before completing normally. */
    @JsonProperty("aborted") Boolean aborted,
    /** Existing event at the completion boundary. SDK helpers drain its synchronous subscriber delivery before returning. Absent for quiet queue removal. */
    @JsonProperty("completionEventId") String completionEventId
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SendAndWaitResult {}

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
        private String messageId;
        private SessionEvent response;
        private Boolean aborted;
        private String completionEventId;

        /**
         * Sets the {@code messageId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder messageId(String value) {
            this.messageId = value;
            return this;
        }

        /**
         * Sets the {@code response} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder response(SessionEvent value) {
            this.response = value;
            return this;
        }

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
         * Sets the {@code completionEventId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder completionEventId(String value) {
            this.completionEventId = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SendAndWaitResult build() {
            if (messageId == null) throw new IllegalStateException("Missing required property: messageId");
            if (aborted == null) throw new IllegalStateException("Missing required property: aborted");
            return new SendAndWaitResult(messageId, response, aborted, completionEventId);
        }
    }
}
