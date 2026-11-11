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
 * The active selection range in an IDE document.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record IdeSelectionRange(
    /** Start position. */
    @JsonProperty("start") IdePosition start,
    /** End position. */
    @JsonProperty("end") IdePosition end,
    /** Whether the selection contains no text. */
    @JsonProperty("isEmpty") Boolean isEmpty
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public IdeSelectionRange {}

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
        private IdePosition start;
        private IdePosition end;
        private Boolean isEmpty;

        /**
         * Sets the {@code start} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder start(IdePosition value) {
            this.start = value;
            return this;
        }

        /**
         * Sets the {@code end} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder end(IdePosition value) {
            this.end = value;
            return this;
        }

        /**
         * Sets the {@code isEmpty} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder isEmpty(Boolean value) {
            this.isEmpty = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public IdeSelectionRange build() {
            if (start == null) throw new IllegalStateException("Missing required property: start");
            if (end == null) throw new IllegalStateException("Missing required property: end");
            if (isEmpty == null) throw new IllegalStateException("Missing required property: isEmpty");
            return new IdeSelectionRange(start, end, isEmpty);
        }
    }
}
