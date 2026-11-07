/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * An authoritative monthly usage reading for the observation's provider, account, and service.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProviderMonthlyUsage(
    /** Availability of monthly consumption. Missing or unavailable usage must never be interpreted as zero. */
    @JsonProperty("state") ProviderMonthlyUsageState state,
    /** Unit of consumedQuantity; independent of the parent observation's balance unit. */
    @JsonProperty("unit") ProviderQuotaUnit unit,
    /** Identity scope of the reading within its provider and service. */
    @JsonProperty("scope") ProviderMonthlyUsageScope scope,
    /** Nonnegative finite consumption reported by the service, preserving zero and fractions. Present only when state is available; never computed from tokens or balance differences. */
    @JsonProperty("consumedQuantity") Double consumedQuantity,
    /** Service read time in UTC RFC 3339, not a ledger reconciliation watermark or confirmation of the latest inference charge. */
    @JsonProperty("queriedAt") String queriedAt,
    /** Start of the service-reported monthly billing cycle, in UTC RFC 3339. */
    @JsonProperty("cycleStart") String cycleStart,
    /** End of the service-reported monthly billing cycle, in UTC RFC 3339. */
    @JsonProperty("resetOn") String resetOn
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ProviderMonthlyUsage {}

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
        private ProviderMonthlyUsageState state;
        private ProviderQuotaUnit unit;
        private ProviderMonthlyUsageScope scope;
        private Double consumedQuantity;
        private String queriedAt;
        private String cycleStart;
        private String resetOn;

        /**
         * Sets the {@code state} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder state(ProviderMonthlyUsageState value) {
            this.state = value;
            return this;
        }

        /**
         * Sets the {@code unit} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder unit(ProviderQuotaUnit value) {
            this.unit = value;
            return this;
        }

        /**
         * Sets the {@code scope} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder scope(ProviderMonthlyUsageScope value) {
            this.scope = value;
            return this;
        }

        /**
         * Sets the {@code consumedQuantity} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder consumedQuantity(Double value) {
            this.consumedQuantity = value;
            return this;
        }

        /**
         * Sets the {@code queriedAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder queriedAt(String value) {
            this.queriedAt = value;
            return this;
        }

        /**
         * Sets the {@code cycleStart} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder cycleStart(String value) {
            this.cycleStart = value;
            return this;
        }

        /**
         * Sets the {@code resetOn} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder resetOn(String value) {
            this.resetOn = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ProviderMonthlyUsage build() {
            if (state == null) throw new IllegalStateException("Missing required property: state");
            if (unit == null) throw new IllegalStateException("Missing required property: unit");
            if (scope == null) throw new IllegalStateException("Missing required property: scope");
            return new ProviderMonthlyUsage(state, unit, scope, consumedQuantity, queriedAt, cycleStart, resetOn);
        }
    }
}
