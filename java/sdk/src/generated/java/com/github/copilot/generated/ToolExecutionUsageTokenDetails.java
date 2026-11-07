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
 * Provider-reported image and text token counts, which are subsets of the corresponding total rather than additional tokens.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolExecutionUsageTokenDetails(
    /** Image tokens included in the corresponding input or output total. */
    @JsonProperty("imageTokens") Long imageTokens,
    /** Text tokens included in the corresponding input or output total. */
    @JsonProperty("textTokens") Long textTokens
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ToolExecutionUsageTokenDetails {}

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
        private Long imageTokens;
        private Long textTokens;

        /**
         * Sets the {@code imageTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder imageTokens(Long value) {
            this.imageTokens = value;
            return this;
        }

        /**
         * Sets the {@code textTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder textTokens(Long value) {
            this.textTokens = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ToolExecutionUsageTokenDetails build() {
            if (imageTokens == null) throw new IllegalStateException("Missing required property: imageTokens");
            if (textTokens == null) throw new IllegalStateException("Missing required property: textTokens");
            return new ToolExecutionUsageTokenDetails(imageTokens, textTokens);
        }
    }
}
