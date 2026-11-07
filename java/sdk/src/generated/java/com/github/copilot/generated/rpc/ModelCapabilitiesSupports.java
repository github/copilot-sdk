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
 * Feature flags indicating what the model supports
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelCapabilitiesSupports(
    /** Whether the model supports provider-native thinking. Independent of configurable reasoning effort; omission means unknown. */
    @JsonProperty("thinking") Boolean thinking,
    /** Whether this model supports vision/image input */
    @JsonProperty("vision") Boolean vision,
    /** Whether this model supports canonical tool calling */
    @JsonProperty("toolCalls") Boolean toolCalls,
    /** Whether this model supports reasoning effort configuration */
    @JsonProperty("reasoningEffort") Boolean reasoningEffort,
    /** Resolved Anthropic adaptive-thinking capability — unsupported / optional / required / adaptive_only. 'required' models reject thinking.type='enabled' with HTTP 400 but still accept 'disabled' (e.g. opus-4.7/4.8/5, sonnet-5); 'adaptive_only' models accept nothing but 'adaptive' (e.g. fable, mythos). */
    @JsonProperty("adaptive_thinking") AdaptiveThinkingSupport adaptiveThinking
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ModelCapabilitiesSupports {}

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
        private Boolean thinking;
        private Boolean vision;
        private Boolean toolCalls;
        private Boolean reasoningEffort;
        private AdaptiveThinkingSupport adaptiveThinking;

        /**
         * Sets the {@code thinking} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder thinking(Boolean value) {
            this.thinking = value;
            return this;
        }

        /**
         * Sets the {@code vision} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder vision(Boolean value) {
            this.vision = value;
            return this;
        }

        /**
         * Sets the {@code toolCalls} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder toolCalls(Boolean value) {
            this.toolCalls = value;
            return this;
        }

        /**
         * Sets the {@code reasoningEffort} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder reasoningEffort(Boolean value) {
            this.reasoningEffort = value;
            return this;
        }

        /**
         * Sets the {@code adaptive_thinking} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder adaptiveThinking(AdaptiveThinkingSupport value) {
            this.adaptiveThinking = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ModelCapabilitiesSupports build() {
            return new ModelCapabilitiesSupports(thinking, vision, toolCalls, reasoningEffort, adaptiveThinking);
        }
    }
}
