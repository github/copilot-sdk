/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

/**
 * Token limits for prompts, outputs, and context window
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelCapabilitiesLimits(
    /** Maximum number of prompt/input tokens */
    @JsonProperty("max_prompt_tokens") Long maxPromptTokens,
    /** Maximum number of output/completion tokens */
    @JsonProperty("max_output_tokens") Long maxOutputTokens,
    /** Maximum total context window size in tokens */
    @JsonProperty("max_context_window_tokens") Long maxContextWindowTokens,
    /** Vision-specific limits */
    @JsonProperty("vision") ModelCapabilitiesLimitsVision vision
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ModelCapabilitiesLimits {}

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
        private Long maxPromptTokens;
        private Long maxOutputTokens;
        private Long maxContextWindowTokens;
        private ModelCapabilitiesLimitsVision vision;

        /**
         * Sets the {@code max_prompt_tokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder maxPromptTokens(Long value) {
            this.maxPromptTokens = value;
            return this;
        }

        /**
         * Sets the {@code max_output_tokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder maxOutputTokens(Long value) {
            this.maxOutputTokens = value;
            return this;
        }

        /**
         * Sets the {@code max_context_window_tokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder maxContextWindowTokens(Long value) {
            this.maxContextWindowTokens = value;
            return this;
        }

        /**
         * Sets the {@code vision} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder vision(ModelCapabilitiesLimitsVision value) {
            this.vision = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ModelCapabilitiesLimits build() {
            return new ModelCapabilitiesLimits(maxPromptTokens, maxOutputTokens, maxContextWindowTokens, vision);
        }
    }
}
