/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * Internal per-quota snapshot for assistant usage, including entitlement, consumed requests, overage, reset date, and remaining quota.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AssistantUsageQuotaSnapshot(
    /** Whether the user has an unlimited usage entitlement */
    @JsonProperty("isUnlimitedEntitlement") Boolean isUnlimitedEntitlement,
    /** Total requests allowed by the entitlement */
    @JsonProperty("entitlementRequests") Long entitlementRequests,
    /** Number of requests already consumed */
    @JsonProperty("usedRequests") Long usedRequests,
    /** Whether usage is still permitted after quota exhaustion */
    @JsonProperty("usageAllowedWithExhaustedQuota") Boolean usageAllowedWithExhaustedQuota,
    /** Number of additional usage requests made this period */
    @JsonProperty("overage") Double overage,
    /** Whether additional usage is allowed when quota is exhausted */
    @JsonProperty("overageAllowedWithExhaustedQuota") Boolean overageAllowedWithExhaustedQuota,
    /** Percentage of quota remaining (0 to 100) */
    @JsonProperty("remainingPercentage") Double remainingPercentage,
    /** Date when the quota resets */
    @JsonProperty("resetDate") OffsetDateTime resetDate,
    /** Whether the user currently has quota available for use */
    @JsonProperty("hasQuota") Boolean hasQuota,
    /** Whether this snapshot uses token-based billing (AI-credits allocation) */
    @JsonProperty("tokenBasedBilling") Boolean tokenBasedBilling,
    /** Pay-as-you-go additional-usage budget cap in AI credits (1 credit = $0.01); present only when CAPI emits a finite value */
    @JsonProperty("overageEntitlement") Double overageEntitlement
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AssistantUsageQuotaSnapshot {}

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
        private Long entitlementRequests;
        private Long usedRequests;
        private Boolean usageAllowedWithExhaustedQuota;
        private Double overage;
        private Boolean overageAllowedWithExhaustedQuota;
        private Double remainingPercentage;
        private OffsetDateTime resetDate;
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
        public Builder entitlementRequests(Long value) {
            this.entitlementRequests = value;
            return this;
        }

        /**
         * Sets the {@code usedRequests} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder usedRequests(Long value) {
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
         * Sets the {@code resetDate} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder resetDate(OffsetDateTime value) {
            this.resetDate = value;
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
        public AssistantUsageQuotaSnapshot build() {
            if (isUnlimitedEntitlement == null) throw new IllegalStateException("Missing required property: isUnlimitedEntitlement");
            if (entitlementRequests == null) throw new IllegalStateException("Missing required property: entitlementRequests");
            if (usedRequests == null) throw new IllegalStateException("Missing required property: usedRequests");
            if (usageAllowedWithExhaustedQuota == null) throw new IllegalStateException("Missing required property: usageAllowedWithExhaustedQuota");
            if (overage == null) throw new IllegalStateException("Missing required property: overage");
            if (overageAllowedWithExhaustedQuota == null) throw new IllegalStateException("Missing required property: overageAllowedWithExhaustedQuota");
            if (remainingPercentage == null) throw new IllegalStateException("Missing required property: remainingPercentage");
            return new AssistantUsageQuotaSnapshot(isUnlimitedEntitlement, entitlementRequests, usedRequests, usageAllowedWithExhaustedQuota, overage, overageAllowedWithExhaustedQuota, remainingPercentage, resetDate, hasQuota, tokenBasedBilling, overageEntitlement);
        }
    }
}
