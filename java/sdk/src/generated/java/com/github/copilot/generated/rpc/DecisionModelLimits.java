/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Request limits for one decision model.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DecisionModelLimits(
    /** Maximum questions per request. */
    @JsonProperty("maxQuestions") Long maxQuestions,
    /** Minimum options per choice question. */
    @JsonProperty("minOptions") Long minOptions,
    /** Maximum options per choice question. */
    @JsonProperty("maxOptions") Long maxOptions,
    /** Minimum levels per score question. */
    @JsonProperty("minLevels") Long minLevels,
    /** Maximum levels per score question. */
    @JsonProperty("maxLevels") Long maxLevels,
    /** Maximum size of the serialized request, in bytes. */
    @JsonProperty("maxRequestBytes") Long maxRequestBytes,
    /** Maximum nesting depth of JSON context. */
    @JsonProperty("maxJsonDepth") Long maxJsonDepth,
    /** Token budget for the context plus any one question, when the model has one. */
    @JsonProperty("contextPlusQuestionTokens") Long contextPlusQuestionTokens
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public DecisionModelLimits {}

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
        private Long maxQuestions;
        private Long minOptions;
        private Long maxOptions;
        private Long minLevels;
        private Long maxLevels;
        private Long maxRequestBytes;
        private Long maxJsonDepth;
        private Long contextPlusQuestionTokens;

        /**
         * Sets the {@code maxQuestions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder maxQuestions(Long value) {
            this.maxQuestions = value;
            return this;
        }

        /**
         * Sets the {@code minOptions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder minOptions(Long value) {
            this.minOptions = value;
            return this;
        }

        /**
         * Sets the {@code maxOptions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder maxOptions(Long value) {
            this.maxOptions = value;
            return this;
        }

        /**
         * Sets the {@code minLevels} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder minLevels(Long value) {
            this.minLevels = value;
            return this;
        }

        /**
         * Sets the {@code maxLevels} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder maxLevels(Long value) {
            this.maxLevels = value;
            return this;
        }

        /**
         * Sets the {@code maxRequestBytes} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder maxRequestBytes(Long value) {
            this.maxRequestBytes = value;
            return this;
        }

        /**
         * Sets the {@code maxJsonDepth} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder maxJsonDepth(Long value) {
            this.maxJsonDepth = value;
            return this;
        }

        /**
         * Sets the {@code contextPlusQuestionTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder contextPlusQuestionTokens(Long value) {
            this.contextPlusQuestionTokens = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public DecisionModelLimits build() {
            if (maxQuestions == null) throw new IllegalStateException("Missing required property: maxQuestions");
            if (minOptions == null) throw new IllegalStateException("Missing required property: minOptions");
            if (maxOptions == null) throw new IllegalStateException("Missing required property: maxOptions");
            if (minLevels == null) throw new IllegalStateException("Missing required property: minLevels");
            if (maxLevels == null) throw new IllegalStateException("Missing required property: maxLevels");
            if (maxRequestBytes == null) throw new IllegalStateException("Missing required property: maxRequestBytes");
            if (maxJsonDepth == null) throw new IllegalStateException("Missing required property: maxJsonDepth");
            return new DecisionModelLimits(maxQuestions, minOptions, maxOptions, minLevels, maxLevels, maxRequestBytes, maxJsonDepth, contextPlusQuestionTokens);
        }
    }
}
