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
 * Token usage detail for a single billing category
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CompactionCompleteCompactionTokensUsedCopilotUsageTokenDetail(
    /** Number of tokens in this billing batch */
    @JsonProperty("batchSize") Long batchSize,
    /** Cost per batch of tokens */
    @JsonProperty("costPerBatch") Long costPerBatch,
    /** Model responsible for this billing entry */
    @JsonProperty("model") String model,
    /** Total token count for this entry */
    @JsonProperty("tokenCount") Long tokenCount,
    /** Token category (e.g., "input", "output") */
    @JsonProperty("tokenType") String tokenType
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CompactionCompleteCompactionTokensUsedCopilotUsageTokenDetail {}

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
        private Long batchSize;
        private Long costPerBatch;
        private String model;
        private Long tokenCount;
        private String tokenType;

        /**
         * Sets the {@code batchSize} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder batchSize(Long value) {
            this.batchSize = value;
            return this;
        }

        /**
         * Sets the {@code costPerBatch} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder costPerBatch(Long value) {
            this.costPerBatch = value;
            return this;
        }

        /**
         * Sets the {@code model} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder model(String value) {
            this.model = value;
            return this;
        }

        /**
         * Sets the {@code tokenCount} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder tokenCount(Long value) {
            this.tokenCount = value;
            return this;
        }

        /**
         * Sets the {@code tokenType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder tokenType(String value) {
            this.tokenType = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CompactionCompleteCompactionTokensUsedCopilotUsageTokenDetail build() {
            if (batchSize == null) throw new IllegalStateException("Missing required property: batchSize");
            if (costPerBatch == null) throw new IllegalStateException("Missing required property: costPerBatch");
            if (tokenCount == null) throw new IllegalStateException("Missing required property: tokenCount");
            if (tokenType == null) throw new IllegalStateException("Missing required property: tokenType");
            return new CompactionCompleteCompactionTokensUsedCopilotUsageTokenDetail(batchSize, costPerBatch, model, tokenCount, tokenType);
        }
    }
}
