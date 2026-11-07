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
 * Authoritative budget measurements and policy metadata, independent of provider.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProviderQuotaBudgetMetadata(
    /** Whether the budget has unlimited entitlement. */
    @JsonProperty("unlimited") Boolean unlimited,
    /** Exact budget entitlement, retaining incumbent fractional-unit compatibility. */
    @JsonProperty("entitlement") Double entitlement,
    /** Exact measured consumption in the budget's unit. */
    @JsonProperty("consumed") Double consumed,
    /** Service-reported remaining percentage for authoritative budget presentation. */
    @JsonProperty("remainingPercentage") Double remainingPercentage,
    /** Whether service policy allows continued usage after exhaustion. */
    @JsonProperty("usageAllowedWhenExhausted") Boolean usageAllowedWhenExhausted,
    /** Usage beyond entitlement, in the budget's unit. */
    @JsonProperty("overage") Double overage,
    /** Whether additional usage is allowed when the budget is exhausted. */
    @JsonProperty("overageAllowedWhenExhausted") Boolean overageAllowedWhenExhausted,
    /** Reset instant in epoch milliseconds when this budget actually defines a window. */
    @JsonProperty("resetAtEpochMs") Double resetAtEpochMs,
    /** Whether the reset instant is an estimate. */
    @JsonProperty("resetEstimated") Boolean resetEstimated,
    /** Whether this budget uses token-based billing. */
    @JsonProperty("tokenBasedBilling") Boolean tokenBasedBilling,
    /** Optional additional-usage budget cap. */
    @JsonProperty("overageLimit") Double overageLimit
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ProviderQuotaBudgetMetadata {}

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
        private Boolean unlimited;
        private Double entitlement;
        private Double consumed;
        private Double remainingPercentage;
        private Boolean usageAllowedWhenExhausted;
        private Double overage;
        private Boolean overageAllowedWhenExhausted;
        private Double resetAtEpochMs;
        private Boolean resetEstimated;
        private Boolean tokenBasedBilling;
        private Double overageLimit;

        /**
         * Sets the {@code unlimited} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder unlimited(Boolean value) {
            this.unlimited = value;
            return this;
        }

        /**
         * Sets the {@code entitlement} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder entitlement(Double value) {
            this.entitlement = value;
            return this;
        }

        /**
         * Sets the {@code consumed} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder consumed(Double value) {
            this.consumed = value;
            return this;
        }

        /**
         * Sets the {@code remainingPercentage} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder remainingPercentage(Double value) {
            this.remainingPercentage = value;
            return this;
        }

        /**
         * Sets the {@code usageAllowedWhenExhausted} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder usageAllowedWhenExhausted(Boolean value) {
            this.usageAllowedWhenExhausted = value;
            return this;
        }

        /**
         * Sets the {@code overage} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder overage(Double value) {
            this.overage = value;
            return this;
        }

        /**
         * Sets the {@code overageAllowedWhenExhausted} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder overageAllowedWhenExhausted(Boolean value) {
            this.overageAllowedWhenExhausted = value;
            return this;
        }

        /**
         * Sets the {@code resetAtEpochMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder resetAtEpochMs(Double value) {
            this.resetAtEpochMs = value;
            return this;
        }

        /**
         * Sets the {@code resetEstimated} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder resetEstimated(Boolean value) {
            this.resetEstimated = value;
            return this;
        }

        /**
         * Sets the {@code tokenBasedBilling} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder tokenBasedBilling(Boolean value) {
            this.tokenBasedBilling = value;
            return this;
        }

        /**
         * Sets the {@code overageLimit} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder overageLimit(Double value) {
            this.overageLimit = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ProviderQuotaBudgetMetadata build() {
            if (unlimited == null) throw new IllegalStateException("Missing required property: unlimited");
            if (entitlement == null) throw new IllegalStateException("Missing required property: entitlement");
            if (consumed == null) throw new IllegalStateException("Missing required property: consumed");
            if (remainingPercentage == null) throw new IllegalStateException("Missing required property: remainingPercentage");
            if (usageAllowedWhenExhausted == null) throw new IllegalStateException("Missing required property: usageAllowedWhenExhausted");
            if (overage == null) throw new IllegalStateException("Missing required property: overage");
            if (overageAllowedWhenExhausted == null) throw new IllegalStateException("Missing required property: overageAllowedWhenExhausted");
            return new ProviderQuotaBudgetMetadata(unlimited, entitlement, consumed, remainingPercentage, usageAllowedWhenExhausted, overage, overageAllowedWhenExhausted, resetAtEpochMs, resetEstimated, tokenBasedBilling, overageLimit);
        }
    }
}
