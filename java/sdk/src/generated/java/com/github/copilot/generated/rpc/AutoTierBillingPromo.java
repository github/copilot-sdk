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
 * Server-driven promotion for an Auto routing preference. It can be announced before it starts.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AutoTierBillingPromo(
    /** Stable identifier for the promotion campaign. */
    @JsonProperty("id") String id,
    /** Percentage discount (0-100) while the promotion is active. Absent or 0 means the promotion is an announcement only: do not present a discount. */
    @JsonProperty("discountPercent") Double discountPercent,
    /** UTC ISO 8601 timestamp marking when the promotion starts. The service can announce a promotion before it starts, so do not present the discount as active before this time. Absent means it has already started. */
    @JsonProperty("startsAt") String startsAt,
    /** UTC ISO 8601 timestamp marking when the promotion ends. Absent means open-ended. The runtime omits promotions that have already ended. */
    @JsonProperty("endsAt") String endsAt,
    /** Human-readable promotion message. Does not include the start or end timestamps. */
    @JsonProperty("message") String message,
    /** Whether the service asked hosts to give this promotion a prominent surface, such as a banner. Absent means the service expressed no preference, so hosts should apply their own default rather than read it as `false`. */
    @JsonProperty("showBanner") Boolean showBanner
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AutoTierBillingPromo {}

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
        private String id;
        private Double discountPercent;
        private String startsAt;
        private String endsAt;
        private String message;
        private Boolean showBanner;

        /**
         * Sets the {@code id} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder id(String value) {
            this.id = value;
            return this;
        }

        /**
         * Sets the {@code discountPercent} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder discountPercent(Double value) {
            this.discountPercent = value;
            return this;
        }

        /**
         * Sets the {@code startsAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder startsAt(String value) {
            this.startsAt = value;
            return this;
        }

        /**
         * Sets the {@code endsAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder endsAt(String value) {
            this.endsAt = value;
            return this;
        }

        /**
         * Sets the {@code message} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder message(String value) {
            this.message = value;
            return this;
        }

        /**
         * Sets the {@code showBanner} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder showBanner(Boolean value) {
            this.showBanner = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AutoTierBillingPromo build() {
            return new AutoTierBillingPromo(id, discountPercent, startsAt, endsAt, message, showBanner);
        }
    }
}
