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
 * Session event "capabilities.changed". Session capability change notification
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class CapabilitiesChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "capabilities.changed"; }

    @JsonProperty("data")
    private CapabilitiesChangedEventData data;

    public CapabilitiesChangedEventData getData() { return data; }
    public void setData(CapabilitiesChangedEventData data) { this.data = data; }

    /** Data payload for {@link CapabilitiesChangedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CapabilitiesChangedEventData(
        /** UI capability changes */
        @JsonProperty("ui") CapabilitiesChangedUI ui
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public CapabilitiesChangedEventData {}

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
            private CapabilitiesChangedUI ui;

            /**
             * Sets the {@code ui} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder ui(CapabilitiesChangedUI value) {
                this.ui = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public CapabilitiesChangedEventData build() {
                return new CapabilitiesChangedEventData(ui);
            }
        }
    }
}
