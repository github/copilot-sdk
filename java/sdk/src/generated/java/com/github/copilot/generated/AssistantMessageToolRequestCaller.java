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
 * Hosted program that requested this client tool call
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AssistantMessageToolRequestCaller(
    /** Kind of hosted caller that requested the client tool call. */
    @JsonProperty("type") AssistantMessageToolRequestCallerType type,
    /** Provider-assigned identifier for the hosted caller. */
    @JsonProperty("callerId") String callerId
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AssistantMessageToolRequestCaller {}

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
        private AssistantMessageToolRequestCallerType type;
        private String callerId;

        /**
         * Sets the {@code type} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder type(AssistantMessageToolRequestCallerType value) {
            this.type = value;
            return this;
        }

        /**
         * Sets the {@code callerId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder callerId(String value) {
            this.callerId = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AssistantMessageToolRequestCaller build() {
            if (type == null) throw new IllegalStateException("Missing required property: type");
            if (callerId == null) throw new IllegalStateException("Missing required property: callerId");
            return new AssistantMessageToolRequestCaller(type, callerId);
        }
    }
}
