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
 * Inclusive durable event range summarized by a completion receipt.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CompletionReceiptEventRange(
    /** Identifier of the user message that starts the covered exchange. */
    @JsonProperty("startEventId") String startEventId,
    /** Identifier of the assistant turn-end event that ends the covered exchange. Always equals the receipt's sourceEventId, so either field is a valid join key. */
    @JsonProperty("endEventId") String endEventId
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CompletionReceiptEventRange {}

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
        private String startEventId;
        private String endEventId;

        /**
         * Sets the {@code startEventId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder startEventId(String value) {
            this.startEventId = value;
            return this;
        }

        /**
         * Sets the {@code endEventId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder endEventId(String value) {
            this.endEventId = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CompletionReceiptEventRange build() {
            if (startEventId == null) throw new IllegalStateException("Missing required property: startEventId");
            if (endEventId == null) throw new IllegalStateException("Missing required property: endEventId");
            return new CompletionReceiptEventRange(startEventId, endEventId);
        }
    }
}
