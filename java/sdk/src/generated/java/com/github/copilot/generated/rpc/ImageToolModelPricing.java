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
 * Default-tier text and image token prices reported for an image model, in AI credits per billing batch. Missing rates remain absent; these are not per-image quotes or actual billed usage.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ImageToolModelPricing(
    /** Number of tokens per billing batch. */
    @JsonProperty("batchSize") Long batchSize,
    /** AI credits per batch of text-input tokens, including a reported zero. */
    @JsonProperty("textInputPrice") Double textInputPrice,
    /** AI credits per batch of text-output tokens, not generated image tokens. */
    @JsonProperty("textOutputPrice") Double textOutputPrice,
    /** Distinct AI credits per batch of image-input tokens, when reported, including zero. */
    @JsonProperty("imageInputPrice") Double imageInputPrice,
    /** Distinct AI credits per batch of image-output tokens, when reported, including zero. */
    @JsonProperty("imageOutputPrice") Double imageOutputPrice
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ImageToolModelPricing {}

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
        private Double textInputPrice;
        private Double textOutputPrice;
        private Double imageInputPrice;
        private Double imageOutputPrice;

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
         * Sets the {@code textInputPrice} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder textInputPrice(Double value) {
            this.textInputPrice = value;
            return this;
        }

        /**
         * Sets the {@code textOutputPrice} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder textOutputPrice(Double value) {
            this.textOutputPrice = value;
            return this;
        }

        /**
         * Sets the {@code imageInputPrice} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder imageInputPrice(Double value) {
            this.imageInputPrice = value;
            return this;
        }

        /**
         * Sets the {@code imageOutputPrice} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder imageOutputPrice(Double value) {
            this.imageOutputPrice = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ImageToolModelPricing build() {
            if (batchSize == null) throw new IllegalStateException("Missing required property: batchSize");
            return new ImageToolModelPricing(batchSize, textInputPrice, textOutputPrice, imageInputPrice, imageOutputPrice);
        }
    }
}
