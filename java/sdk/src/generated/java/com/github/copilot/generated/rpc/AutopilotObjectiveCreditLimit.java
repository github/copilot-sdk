/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Current per-window credit limit and consumption for an autopilot objective.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AutopilotObjectiveCreditLimit(
    /** Configured AI-credit cap, when one is set. */
    @JsonProperty("credits") Double credits,
    /** Window consumption in fractional AI credits, for display. */
    @JsonProperty("creditsUsed") Double creditsUsed,
    /** Exact window consumption in non-negative integer nano-AIU, encoded as a decimal string. */
    @JsonProperty("creditsUsedNanoAiu") String creditsUsedNanoAiu
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public AutopilotObjectiveCreditLimit {}

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
        private Double credits;
        private Double creditsUsed;
        private String creditsUsedNanoAiu;

        /**
         * Sets the {@code credits} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder credits(Double value) {
            this.credits = value;
            return this;
        }

        /**
         * Sets the {@code creditsUsed} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder creditsUsed(Double value) {
            this.creditsUsed = value;
            return this;
        }

        /**
         * Sets the {@code creditsUsedNanoAiu} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder creditsUsedNanoAiu(String value) {
            this.creditsUsedNanoAiu = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AutopilotObjectiveCreditLimit build() {
            if (creditsUsed == null) throw new IllegalStateException("Missing required property: creditsUsed");
            if (creditsUsedNanoAiu == null) throw new IllegalStateException("Missing required property: creditsUsedNanoAiu");
            return new AutopilotObjectiveCreditLimit(credits, creditsUsed, creditsUsedNanoAiu);
        }
    }
}
