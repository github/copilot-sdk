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
 * Baseline data provenance for a prediction.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionLimitPredictionBaselineData(
    /** Start of the baseline data slice. */
    @JsonProperty("windowStart") String windowStart,
    /** End of the baseline data slice. */
    @JsonProperty("windowEnd") String windowEnd
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionLimitPredictionBaselineData {}

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
        private String windowStart;
        private String windowEnd;

        /**
         * Sets the {@code windowStart} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder windowStart(String value) {
            this.windowStart = value;
            return this;
        }

        /**
         * Sets the {@code windowEnd} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder windowEnd(String value) {
            this.windowEnd = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionLimitPredictionBaselineData build() {
            if (windowStart == null) throw new IllegalStateException("Missing required property: windowStart");
            if (windowEnd == null) throw new IllegalStateException("Missing required property: windowEnd");
            return new SessionLimitPredictionBaselineData(windowStart, windowEnd);
        }
    }
}
