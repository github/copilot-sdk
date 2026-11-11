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
 * Session event "ide.connection_changed". Live IDE state. This state never restores a connection from session history.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class IdeConnectionChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "ide.connection_changed"; }

    @JsonProperty("data")
    private IdeConnectionChangedEventData data;

    public IdeConnectionChangedEventData getData() { return data; }
    public void setData(IdeConnectionChangedEventData data) { this.data = data; }

    /** Data payload for {@link IdeConnectionChangedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record IdeConnectionChangedEventData(
        /** Currently available IDE, absent during an outage. */
        @JsonProperty("connectedIde") IdeConnectedInfo connectedIde,
        /** IDE whose tool definitions remain registered, including during an outage. */
        @JsonProperty("retainedIde") IdeConnectedInfo retainedIde,
        /** Latest observed selection, absent after disconnect. */
        @JsonProperty("selection") IdeSelection selection
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public IdeConnectionChangedEventData {}

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
            private IdeConnectedInfo connectedIde;
            private IdeConnectedInfo retainedIde;
            private IdeSelection selection;

            /**
             * Sets the {@code connectedIde} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder connectedIde(IdeConnectedInfo value) {
                this.connectedIde = value;
                return this;
            }

            /**
             * Sets the {@code retainedIde} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder retainedIde(IdeConnectedInfo value) {
                this.retainedIde = value;
                return this;
            }

            /**
             * Sets the {@code selection} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder selection(IdeSelection value) {
                this.selection = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public IdeConnectionChangedEventData build() {
                return new IdeConnectionChangedEventData(connectedIde, retainedIde, selection);
            }
        }
    }
}
