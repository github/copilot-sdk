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
 * Session event "ide.selection_changed". A validated selection from the live IDE, or an explicit clear.
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class IdeSelectionChangedEvent extends SessionEvent {

    @Override
    public String getType() { return "ide.selection_changed"; }

    @JsonProperty("data")
    private IdeSelectionChangedEventData data;

    public IdeSelectionChangedEventData getData() { return data; }
    public void setData(IdeSelectionChangedEventData data) { this.data = data; }

    /** Data payload for {@link IdeSelectionChangedEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record IdeSelectionChangedEventData(
        /** Selection path, text, and source range, or null when the selection is cleared. */
        @JsonInclude(JsonInclude.Include.ALWAYS)
        @JsonProperty("selection") IdeSelection selection
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public IdeSelectionChangedEventData {}

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
            private IdeSelection selection;
            private boolean $presence0;

            /**
             * Sets the {@code selection} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder selection(IdeSelection value) {
                this.selection = value;
                this.$presence0 = true;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public IdeSelectionChangedEventData build() {
                if (!$presence0) throw new IllegalStateException("Missing required property: selection");
                return new IdeSelectionChangedEventData(selection);
            }
        }
    }
}
