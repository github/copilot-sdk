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
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Per-model usage metrics, including request counts/costs, token usage, nano-AI units, and per-token-type details.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsageMetricsModelMetric(
    /** Completeness of this model's reported AI-credit subtotal. Explicitly reported zero is complete, not unavailable. */
    @JsonProperty("aiCreditsStatus") AiCreditsStatus aiCreditsStatus,
    /** Request count and cost metrics for this model */
    @JsonProperty("requests") UsageMetricsModelMetricRequests requests,
    /** Token usage metrics for this model */
    @JsonProperty("usage") UsageMetricsModelMetricUsage usage,
    /** Latest known prompt-cache expiration for this model. A timestamp in the past indicates that the observed cache has expired. */
    @JsonProperty("cacheExpiresAt") OffsetDateTime cacheExpiresAt,
    /** Accumulated nano-AI units cost for this model */
    @JsonProperty("totalNanoAiu") Double totalNanoAiu,
    /** Token count details per type */
    @JsonProperty("tokenDetails") Map<String, UsageMetricsModelMetricTokenDetail> tokenDetails
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public UsageMetricsModelMetric {}

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
        private AiCreditsStatus aiCreditsStatus;
        private UsageMetricsModelMetricRequests requests;
        private UsageMetricsModelMetricUsage usage;
        private OffsetDateTime cacheExpiresAt;
        private Double totalNanoAiu;
        private Map<String, UsageMetricsModelMetricTokenDetail> tokenDetails;

        /**
         * Sets the {@code aiCreditsStatus} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder aiCreditsStatus(AiCreditsStatus value) {
            this.aiCreditsStatus = value;
            return this;
        }

        /**
         * Sets the {@code requests} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder requests(UsageMetricsModelMetricRequests value) {
            this.requests = value;
            return this;
        }

        /**
         * Sets the {@code usage} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder usage(UsageMetricsModelMetricUsage value) {
            this.usage = value;
            return this;
        }

        /**
         * Sets the {@code cacheExpiresAt} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder cacheExpiresAt(OffsetDateTime value) {
            this.cacheExpiresAt = value;
            return this;
        }

        /**
         * Sets the {@code totalNanoAiu} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
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
        public Builder tokenDetails(Map<String, UsageMetricsModelMetricTokenDetail> value) {
            this.tokenDetails = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public UsageMetricsModelMetric build() {
            if (requests == null) throw new IllegalStateException("Missing required property: requests");
            if (usage == null) throw new IllegalStateException("Missing required property: usage");
            return new UsageMetricsModelMetric(aiCreditsStatus, requests, usage, cacheExpiresAt, totalNanoAiu, tokenDetails);
        }
    }
}
