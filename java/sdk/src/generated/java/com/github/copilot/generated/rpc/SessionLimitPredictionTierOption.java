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
 * Semantic usage tier and its AI-credit cap.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionLimitPredictionTierOption(
    /** Semantic usage tier. */
    @JsonProperty("tier") SessionLimitPredictionTier tier,
    /** AI-credit cap for this tier. */
    @JsonProperty("cap") Double cap
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionLimitPredictionTierOption {}

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
        private SessionLimitPredictionTier tier;
        private Double cap;

        /**
         * Sets the {@code tier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder tier(SessionLimitPredictionTier value) {
            this.tier = value;
            return this;
        }

        /**
         * Sets the {@code cap} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder cap(Double value) {
            this.cap = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionLimitPredictionTierOption build() {
            if (tier == null) throw new IllegalStateException("Missing required property: tier");
            if (cap == null) throw new IllegalStateException("Missing required property: cap");
            return new SessionLimitPredictionTierOption(tier, cap);
        }
    }
}
