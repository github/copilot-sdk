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
 * Variant {@code failed} of {@link CafeMetricsResolveAdmissionParams}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class CafeMetricsResolveAdmissionParamsFailed extends CafeMetricsResolveAdmissionParams {

    @JsonProperty("kind")
    private final String kind = "failed";

    @Override
    public String getKind() { return kind; }

    /** Finite non-negative lookup duration in milliseconds. */
    @JsonProperty("lookupDurationMs")
    private Double lookupDurationMs;

    public Double getLookupDurationMs() { return lookupDurationMs; }
    public void setLookupDurationMs(Double lookupDurationMs) { this.lookupDurationMs = lookupDurationMs; }


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
        private Double lookupDurationMs;

        /**
         * Sets the {@code lookupDurationMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder lookupDurationMs(Double value) {
            this.lookupDurationMs = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CafeMetricsResolveAdmissionParamsFailed build() {
            if (lookupDurationMs == null) throw new IllegalStateException("Missing required property: lookupDurationMs");
            CafeMetricsResolveAdmissionParamsFailed instance = new CafeMetricsResolveAdmissionParamsFailed();
            instance.setLookupDurationMs(lookupDurationMs);
            return instance;
        }
    }
}
