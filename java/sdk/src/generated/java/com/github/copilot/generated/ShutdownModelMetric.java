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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Per-model shutdown metrics with request counts, token usage, nano-AI units, and token details.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ShutdownModelMetric(
    /** Request count and cost metrics */
    @JsonProperty("requests") ShutdownModelMetricRequests requests,
    /** Token usage breakdown */
    @JsonProperty("usage") ShutdownModelMetricUsage usage,
    /** Accumulated nano-AI units cost for this model */
    @CopilotExperimental
    @JsonProperty("totalNanoAiu") Double totalNanoAiu,
    /** Token count details per type */
    @JsonProperty("tokenDetails") Map<String, ShutdownModelMetricTokenDetail> tokenDetails
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ShutdownModelMetric {}

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
        private ShutdownModelMetricRequests requests;
        private ShutdownModelMetricUsage usage;
        private Double totalNanoAiu;
        private Map<String, ShutdownModelMetricTokenDetail> tokenDetails;

        /**
         * Sets the {@code requests} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder requests(ShutdownModelMetricRequests value) {
            this.requests = value;
            return this;
        }

        /**
         * Sets the {@code usage} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder usage(ShutdownModelMetricUsage value) {
            this.usage = value;
            return this;
        }

        /**
         * Sets the {@code totalNanoAiu} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder totalNanoAiu(Double value) {
            this.totalNanoAiu = value;
            return this;
        }

        /**
         * Sets the {@code tokenDetails} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder tokenDetails(Map<String, ShutdownModelMetricTokenDetail> value) {
            this.tokenDetails = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ShutdownModelMetric build() {
            if (requests == null) throw new IllegalStateException("Missing required property: requests");
            if (usage == null) throw new IllegalStateException("Missing required property: usage");
            return new ShutdownModelMetric(requests, usage, totalNanoAiu, tokenDetails);
        }
    }
}
