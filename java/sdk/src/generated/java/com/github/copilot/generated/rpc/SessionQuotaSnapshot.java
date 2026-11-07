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
 * Session quota snapshot, preserving the incumbent numeric units and reset metadata.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionQuotaSnapshot(
    /** Whether the entitlement is unlimited. */
    @JsonProperty("isUnlimitedEntitlement") Boolean isUnlimitedEntitlement,
    /** Entitled quantity in this quota's units. */
    @JsonProperty("entitlementRequests") Double entitlementRequests,
    /** Consumed quantity in this quota's units. */
    @JsonProperty("usedRequests") Double usedRequests,
    /** Whether usage is allowed after quota exhaustion. */
    @JsonProperty("usageAllowedWithExhaustedQuota") Boolean usageAllowedWithExhaustedQuota,
    /** Additional usage in this quota's units. */
    @JsonProperty("overage") Double overage,
    /** Whether additional usage is allowed after quota exhaustion. */
    @JsonProperty("overageAllowedWithExhaustedQuota") Boolean overageAllowedWithExhaustedQuota,
    /** Percentage of the entitlement remaining. */
    @JsonProperty("remainingPercentage") Double remainingPercentage,
    /** Quota reset time in milliseconds since the Unix epoch, when known. */
    @JsonProperty("resetDateEpochMs") Double resetDateEpochMs,
    /** Whether the reset time is estimated. */
    @JsonProperty("resetDateEstimated") Boolean resetDateEstimated,
    /** Whether the provider reports available quota. */
    @JsonProperty("hasQuota") Boolean hasQuota,
    /** Whether this quota uses token-based billing. */
    @JsonProperty("tokenBasedBilling") Boolean tokenBasedBilling,
    /** Additional-usage budget cap, when provided. */
    @JsonProperty("overageEntitlement") Double overageEntitlement
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionQuotaSnapshot {}

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
        private Boolean isUnlimitedEntitlement;
        private Double entitlementRequests;
        private Double usedRequests;
        private Boolean usageAllowedWithExhaustedQuota;
        private Double overage;
        private Boolean overageAllowedWithExhaustedQuota;
        private Double remainingPercentage;
        private Double resetDateEpochMs;
        private Boolean resetDateEstimated;
        private Boolean hasQuota;
        private Boolean tokenBasedBilling;
        private Double overageEntitlement;

        /**
         * Sets the {@code isUnlimitedEntitlement} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder isUnlimitedEntitlement(Boolean value) {
            this.isUnlimitedEntitlement = value;
            return this;
        }

        /**
         * Sets the {@code entitlementRequests} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder entitlementRequests(Double value) {
            this.entitlementRequests = value;
            return this;
        }

        /**
         * Sets the {@code usedRequests} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder usedRequests(Double value) {
            this.usedRequests = value;
            return this;
        }

        /**
         * Sets the {@code usageAllowedWithExhaustedQuota} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder usageAllowedWithExhaustedQuota(Boolean value) {
            this.usageAllowedWithExhaustedQuota = value;
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
         * Sets the {@code overageAllowedWithExhaustedQuota} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder overageAllowedWithExhaustedQuota(Boolean value) {
            this.overageAllowedWithExhaustedQuota = value;
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
         * Sets the {@code resetDateEpochMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder resetDateEpochMs(Double value) {
            this.resetDateEpochMs = value;
            return this;
        }

        /**
         * Sets the {@code resetDateEstimated} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder resetDateEstimated(Boolean value) {
            this.resetDateEstimated = value;
            return this;
        }

        /**
         * Sets the {@code hasQuota} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder hasQuota(Boolean value) {
            this.hasQuota = value;
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
         * Sets the {@code overageEntitlement} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder overageEntitlement(Double value) {
            this.overageEntitlement = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionQuotaSnapshot build() {
            if (isUnlimitedEntitlement == null) throw new IllegalStateException("Missing required property: isUnlimitedEntitlement");
            if (entitlementRequests == null) throw new IllegalStateException("Missing required property: entitlementRequests");
            if (usedRequests == null) throw new IllegalStateException("Missing required property: usedRequests");
            if (usageAllowedWithExhaustedQuota == null) throw new IllegalStateException("Missing required property: usageAllowedWithExhaustedQuota");
            if (overage == null) throw new IllegalStateException("Missing required property: overage");
            if (overageAllowedWithExhaustedQuota == null) throw new IllegalStateException("Missing required property: overageAllowedWithExhaustedQuota");
            if (remainingPercentage == null) throw new IllegalStateException("Missing required property: remainingPercentage");
            return new SessionQuotaSnapshot(isUnlimitedEntitlement, entitlementRequests, usedRequests, usageAllowedWithExhaustedQuota, overage, overageAllowedWithExhaustedQuota, remainingPercentage, resetDateEpochMs, resetDateEstimated, hasQuota, tokenBasedBilling, overageEntitlement);
        }
    }
}
