/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * A contiguous span of generated assistant text and the source references that support it.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CitationSpan(
    /** Start offset of the cited span within the final assistant message content (UTF-16 code units, zero-based, inclusive). */
    @JsonProperty("startIndex") Long startIndex,
    /** End offset of the cited span within the final assistant message content (UTF-16 code units, zero-based, exclusive). */
    @JsonProperty("endIndex") Long endIndex,
    /** The sources that support this span of generated text. */
    @JsonProperty("references") List<CitationReference> references
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CitationSpan {}

    /**
     * Returns a named builder.
     * @return a new builder
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static Builder builder() { return new Builder(); }

    /**
     * Named construction for this record.
     * @since 2.0.0
     */
    @com.github.copilot.CopilotExperimental
    public static final class Builder {
        private Builder() {}
        private Long startIndex;
        private Long endIndex;
        private List<CitationReference> references;

        /**
         * Sets the {@code startIndex} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder startIndex(Long value) {
            this.startIndex = value;
            return this;
        }

        /**
         * Sets the {@code endIndex} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder endIndex(Long value) {
            this.endIndex = value;
            return this;
        }

        /**
         * Sets the {@code references} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder references(List<CitationReference> value) {
            this.references = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CitationSpan build() {
            if (startIndex == null) throw new IllegalStateException("Missing required property: startIndex");
            if (endIndex == null) throw new IllegalStateException("Missing required property: endIndex");
            if (references == null) throw new IllegalStateException("Missing required property: references");
            return new CitationSpan(startIndex, endIndex, references);
        }
    }
}
