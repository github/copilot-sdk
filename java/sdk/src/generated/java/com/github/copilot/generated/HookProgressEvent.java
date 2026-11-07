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
 * Session event "hook.progress". Ephemeral progress update from a running hook process
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class HookProgressEvent extends SessionEvent {

    @Override
    public String getType() { return "hook.progress"; }

    @JsonProperty("data")
    private HookProgressEventData data;

    public HookProgressEventData getData() { return data; }
    public void setData(HookProgressEventData data) { this.data = data; }

    /** Data payload for {@link HookProgressEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record HookProgressEventData(
        /** Human-readable progress message from the hook process */
        @JsonProperty("message") String message,
        /** When true, this status message replaces the previous temporary one instead of accumulating */
        @JsonProperty("temporary") Boolean temporary
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public HookProgressEventData {}

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
            private String message;
            private Boolean temporary;

            /**
             * Sets the {@code message} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder message(String value) {
                this.message = value;
                return this;
            }

            /**
             * Sets the {@code temporary} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder temporary(Boolean value) {
                this.temporary = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public HookProgressEventData build() {
                if (message == null) throw new IllegalStateException("Missing required property: message");
                return new HookProgressEventData(message, temporary);
            }
        }
    }
}
