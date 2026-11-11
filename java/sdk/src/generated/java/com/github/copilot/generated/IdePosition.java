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
 * A zero-based location in an IDE document.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record IdePosition(
    /** Zero-based line number. */
    @JsonProperty("line") Double line,
    /** Zero-based character offset. */
    @JsonProperty("character") Double character
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public IdePosition {}

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
        private Double line;
        private Double character;

        /**
         * Sets the {@code line} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder line(Double value) {
            this.line = value;
            return this;
        }

        /**
         * Sets the {@code character} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder character(Double value) {
            this.character = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public IdePosition build() {
            if (line == null) throw new IllegalStateException("Missing required property: line");
            if (character == null) throw new IllegalStateException("Missing required property: character");
            return new IdePosition(line, character);
        }
    }
}
