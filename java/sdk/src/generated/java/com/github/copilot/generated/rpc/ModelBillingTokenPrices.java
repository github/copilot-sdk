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
 * Token-level pricing information for this model
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelBillingTokenPrices(
    /** Cost per billing batch of input tokens. The unit is reported by models.listTokenPriceUnits. */
    @JsonProperty("inputPrice") Double inputPrice,
    /** Cost per billing batch of output tokens. The unit is reported by models.listTokenPriceUnits. */
    @JsonProperty("outputPrice") Double outputPrice,
    /** Use cacheReadPrice when available. Cost per billing batch of cached tokens in the unit reported by models.listTokenPriceUnits. */
    @Deprecated
    @JsonProperty("cachePrice") Double cachePrice,
    /** AI Credits cost per billing batch of cached (read) tokens */
    @JsonProperty("cacheReadPrice") Double cacheReadPrice,
    /** AI Credits cost per billing batch of cache-write (cache creation) tokens. */
    @JsonProperty("cacheWritePrice") Double cacheWritePrice,
    /** AI Credits cost per billing batch of 1-hour cache-write (cache creation) tokens. */
    @JsonProperty("cacheWrite1hPrice") Double cacheWrite1hPrice,
    /** Number of tokens per standard billing batch */
    @JsonProperty("batchSize") Long batchSize,
    /** Use maxPromptTokens instead. Prompt token budget for the default tier. The total context window is this value plus the model's max_output_tokens. */
    @Deprecated
    @JsonProperty("contextMax") Long contextMax,
    /** Prompt token budget for the default tier. The total context window is this value plus the model's max_output_tokens. */
    @JsonProperty("maxPromptTokens") Long maxPromptTokens,
    /** Long context tier pricing (available for models with extended context windows) */
    @JsonProperty("longContext") ModelBillingTokenPricesLongContext longContext
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ModelBillingTokenPrices {}

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
        private Double inputPrice;
        private Double outputPrice;
        private Double cachePrice;
        private Double cacheReadPrice;
        private Double cacheWritePrice;
        private Double cacheWrite1hPrice;
        private Long batchSize;
        private Long contextMax;
        private Long maxPromptTokens;
        private ModelBillingTokenPricesLongContext longContext;

        /**
         * Sets the {@code inputPrice} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder inputPrice(Double value) {
            this.inputPrice = value;
            return this;
        }

        /**
         * Sets the {@code outputPrice} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder outputPrice(Double value) {
            this.outputPrice = value;
            return this;
        }

        /**
         * Sets the {@code cachePrice} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder cachePrice(Double value) {
            this.cachePrice = value;
            return this;
        }

        /**
         * Sets the {@code cacheReadPrice} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder cacheReadPrice(Double value) {
            this.cacheReadPrice = value;
            return this;
        }

        /**
         * Sets the {@code cacheWritePrice} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder cacheWritePrice(Double value) {
            this.cacheWritePrice = value;
            return this;
        }

        /**
         * Sets the {@code cacheWrite1hPrice} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder cacheWrite1hPrice(Double value) {
            this.cacheWrite1hPrice = value;
            return this;
        }

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
         * Sets the {@code contextMax} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder contextMax(Long value) {
            this.contextMax = value;
            return this;
        }

        /**
         * Sets the {@code maxPromptTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder maxPromptTokens(Long value) {
            this.maxPromptTokens = value;
            return this;
        }

        /**
         * Sets the {@code longContext} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder longContext(ModelBillingTokenPricesLongContext value) {
            this.longContext = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ModelBillingTokenPrices build() {
            return new ModelBillingTokenPrices(inputPrice, outputPrice, cachePrice, cacheReadPrice, cacheWritePrice, cacheWrite1hPrice, batchSize, contextMax, maxPromptTokens, longContext);
        }
    }
}
