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
 * Active server-driven promotion for a model, including its discount and optional expiry.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelBillingPromo(
    /** Stable identifier for the promotion campaign. */
    @JsonProperty("id") String id,
    /** Percentage discount (0-100) applied while the promotion is active. May be fractional. */
    @JsonProperty("discountPercent") Double discountPercent,
    /** UTC ISO 8601 timestamp marking when the promotion ends. Optional: an open-ended promotion omits this field. When present, the API only surfaces a promo whose expiry parses and is in the future, so consumers should treat a past value as expired. */
    @JsonProperty("endsAt") String endsAt,
    /** Human-readable promotion message. Does not include the expiry timestamp; consumers may format endsAt and append it when present. */
    @JsonProperty("message") String message,
    /** Whether the service asked hosts to give this promotion a prominent surface, such as a dedicated banner, in addition to listing it with the model. `true` requests that surface and `false` asks for the model list only. Absent means the service expressed no preference — for example a response that predates the field — so hosts should apply their own default rather than read it as `false`. */
    @JsonProperty("showBanner") Boolean showBanner
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ModelBillingPromo {}

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
        public ModelBillingPromo build() {
            return new ModelBillingPromo(id, discountPercent, endsAt, message, showBanner);
        }
    }
}
