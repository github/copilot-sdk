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
 * Session event "session.tools_updated". Payload of `session.tools_updated` identifying the model whose resolved tools were updated.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionToolsUpdatedEvent extends SessionEvent {

    @Override
    public String getType() { return "session.tools_updated"; }

    @JsonProperty("data")
    private SessionToolsUpdatedEventData data;

    public SessionToolsUpdatedEventData getData() { return data; }
    public void setData(SessionToolsUpdatedEventData data) { this.data = data; }

    /** Data payload for {@link SessionToolsUpdatedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionToolsUpdatedEventData(
        /** Identifier of the model the resolved tools apply to. */
        @JsonProperty("model") String model
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionToolsUpdatedEventData {}

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
            private String model;

            /**
             * Sets the {@code model} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder model(String value) {
                this.model = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionToolsUpdatedEventData build() {
                if (model == null) throw new IllegalStateException("Missing required property: model");
                return new SessionToolsUpdatedEventData(model);
            }
        }
    }
}
