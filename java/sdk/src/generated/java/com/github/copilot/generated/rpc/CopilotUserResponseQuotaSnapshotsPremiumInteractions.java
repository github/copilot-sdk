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
 * Premium-interactions quota snapshot from the raw Copilot user-response passthrough, with entitlement, overage, remaining quota, reset, and billing fields.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CopilotUserResponseQuotaSnapshotsPremiumInteractions(
    /** Number of requests/units included in the entitlement for this period; `-1` denotes an unlimited entitlement. */
    @JsonProperty("entitlement") Double entitlement,
    /** Count of additional pay-per-request usage consumed this period beyond the entitlement. */
    @JsonProperty("overage_count") Double overageCount,
    /** Whether usage may continue at pay-per-request rates once the entitlement is exhausted. */
    @JsonProperty("overage_permitted") Boolean overagePermitted,
    /** Percentage of the entitlement remaining at the snapshot timestamp. */
    @JsonProperty("percent_remaining") Double percentRemaining,
    /** Identifier of the quota bucket this snapshot describes. */
    @JsonProperty("quota_id") String quotaId,
    /** Amount of quota remaining at the snapshot timestamp. */
    @JsonProperty("quota_remaining") Double quotaRemaining,
    /** Remaining entitlement/quota amount at the snapshot timestamp. */
    @JsonProperty("remaining") Double remaining,
    /** Whether the entitlement for this category is unlimited. */
    @JsonProperty("unlimited") Boolean unlimited,
    /** UTC timestamp when this snapshot was captured. */
    @JsonProperty("timestamp_utc") String timestampUtc,
    /** Whether the user currently has quota available; when `false` and not unlimited, further requests are blocked until the quota resets. */
    @JsonProperty("has_quota") Boolean hasQuota,
    /** Unix epoch time, in seconds, when this quota next resets. */
    @JsonProperty("quota_reset_at") Double quotaResetAt,
    /** Whether this category uses usage-based (token/AI-credit) billing rather than a fixed premium-request count. */
    @JsonProperty("token_based_billing") Boolean tokenBasedBilling
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public CopilotUserResponseQuotaSnapshotsPremiumInteractions {}

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
        private Double entitlement;
        private Double overageCount;
        private Boolean overagePermitted;
        private Double percentRemaining;
        private String quotaId;
        private Double quotaRemaining;
        private Double remaining;
        private Boolean unlimited;
        private String timestampUtc;
        private Boolean hasQuota;
        private Double quotaResetAt;
        private Boolean tokenBasedBilling;

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
         * Sets the {@code overage_count} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder overageCount(Double value) {
            this.overageCount = value;
            return this;
        }

        /**
         * Sets the {@code overage_permitted} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder overagePermitted(Boolean value) {
            this.overagePermitted = value;
            return this;
        }

        /**
         * Sets the {@code percent_remaining} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder percentRemaining(Double value) {
            this.percentRemaining = value;
            return this;
        }

        /**
         * Sets the {@code quota_id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder quotaId(String value) {
            this.quotaId = value;
            return this;
        }

        /**
         * Sets the {@code quota_remaining} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder quotaRemaining(Double value) {
            this.quotaRemaining = value;
            return this;
        }

        /**
         * Sets the {@code remaining} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder remaining(Double value) {
            this.remaining = value;
            return this;
        }

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
         * Sets the {@code timestamp_utc} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder timestampUtc(String value) {
            this.timestampUtc = value;
            return this;
        }

        /**
         * Sets the {@code has_quota} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder hasQuota(Boolean value) {
            this.hasQuota = value;
            return this;
        }

        /**
         * Sets the {@code quota_reset_at} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder quotaResetAt(Double value) {
            this.quotaResetAt = value;
            return this;
        }

        /**
         * Sets the {@code token_based_billing} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder tokenBasedBilling(Boolean value) {
            this.tokenBasedBilling = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CopilotUserResponseQuotaSnapshotsPremiumInteractions build() {
            return new CopilotUserResponseQuotaSnapshotsPremiumInteractions(entitlement, overageCount, overagePermitted, percentRemaining, quotaId, quotaRemaining, remaining, unlimited, timestampUtc, hasQuota, quotaResetAt, tokenBasedBilling);
        }
    }
}
