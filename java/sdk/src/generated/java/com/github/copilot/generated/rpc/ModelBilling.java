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
 * Billing information
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelBilling(
    /** Billing cost multiplier relative to the base rate */
    @JsonProperty("multiplier") Double multiplier,
    /** Token-level pricing information for this model */
    @JsonProperty("tokenPrices") ModelBillingTokenPrices tokenPrices,
    /** Whole-number percentage discount (0-100) applied to usage billed through this model. Populated for the synthetic `auto` model, where requests routed by auto-mode are billed at a reduced rate; absent for concrete models. */
    @JsonProperty("discountPercent") Long discountPercent,
    /** Active server-driven promotion for this model, if any. Present when the model is being promoted with a discount, which may be time-boxed or open-ended. */
    @JsonProperty("promo") ModelBillingPromo promo
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ModelBilling {}

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
        private Double multiplier;
        private ModelBillingTokenPrices tokenPrices;
        private Long discountPercent;
        private ModelBillingPromo promo;

        /**
         * Sets the {@code multiplier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder multiplier(Double value) {
            this.multiplier = value;
            return this;
        }

        /**
         * Sets the {@code tokenPrices} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder tokenPrices(ModelBillingTokenPrices value) {
            this.tokenPrices = value;
            return this;
        }

        /**
         * Sets the {@code discountPercent} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder discountPercent(Long value) {
            this.discountPercent = value;
            return this;
        }

        /**
         * Sets the {@code promo} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder promo(ModelBillingPromo value) {
            this.promo = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ModelBilling build() {
            return new ModelBilling(multiplier, tokenPrices, discountPercent, promo);
        }
    }
}
