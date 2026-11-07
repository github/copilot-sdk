/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import javax.annotation.processing.Generated;

/**
 * Quota usage snapshot for a Copilot quota type, including entitlement, used requests, overage, reset date, and remaining percentage.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountQuotaSnapshot(
    /** Whether the user has an unlimited usage entitlement */
    @JsonProperty("isUnlimitedEntitlement") Boolean isUnlimitedEntitlement,
    /** Number of requests included in the entitlement, or -1 for unlimited entitlements */
    @JsonProperty("entitlementRequests") Long entitlementRequests,
    /** Number of requests used so far this period */
    @JsonProperty("usedRequests") Long usedRequests,
    /** Whether usage is still permitted after quota exhaustion */
    @JsonProperty("usageAllowedWithExhaustedQuota") Boolean usageAllowedWithExhaustedQuota,
    /** Percentage of entitlement remaining */
    @JsonProperty("remainingPercentage") Double remainingPercentage,
    /** Number of additional usage requests made this period */
    @JsonProperty("overage") Double overage,
    /** Whether additional usage is allowed when quota is exhausted */
    @JsonProperty("overageAllowedWithExhaustedQuota") Boolean overageAllowedWithExhaustedQuota,
    /** Date when the quota resets (ISO 8601 string) */
    @JsonProperty("resetDate") OffsetDateTime resetDate
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AccountQuotaSnapshot {}

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
        private Double remainingPercentage;
        private Double overage;
        private Boolean overageAllowedWithExhaustedQuota;
        private OffsetDateTime resetDate;

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
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AccountQuotaSnapshot build() {
            if (isUnlimitedEntitlement == null) throw new IllegalStateException("Missing required property: isUnlimitedEntitlement");
            if (entitlementRequests == null) throw new IllegalStateException("Missing required property: entitlementRequests");
            if (usedRequests == null) throw new IllegalStateException("Missing required property: usedRequests");
            if (usageAllowedWithExhaustedQuota == null) throw new IllegalStateException("Missing required property: usageAllowedWithExhaustedQuota");
            if (remainingPercentage == null) throw new IllegalStateException("Missing required property: remainingPercentage");
            if (overage == null) throw new IllegalStateException("Missing required property: overage");
            if (overageAllowedWithExhaustedQuota == null) throw new IllegalStateException("Missing required property: overageAllowedWithExhaustedQuota");
            return new AccountQuotaSnapshot(isUnlimitedEntitlement, entitlementRequests, usedRequests, usageAllowedWithExhaustedQuota, remainingPercentage, overage, overageAllowedWithExhaustedQuota, resetDate);
        }
    }
}
