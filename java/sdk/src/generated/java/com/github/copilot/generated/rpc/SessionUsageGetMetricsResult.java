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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Accumulated session usage metrics, including premium request cost, token counts, model breakdown, and code-change totals.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionUsageGetMetricsResult(
    /** Completeness of totalNanoAiu. Numeric totals remain the reported subtotal; zero is not a claim of free usage when unavailable. */
    @JsonProperty("aiCreditsStatus") AiCreditsStatus aiCreditsStatus,
    /** Total user-initiated premium request cost across all models (may be fractional due to multipliers) */
    @JsonProperty("totalPremiumRequestCost") Double totalPremiumRequestCost,
    /** Raw count of user-initiated API requests */
    @JsonProperty("totalUserRequests") Long totalUserRequests,
    /** Session-wide accumulated nano-AI units cost */
    @JsonProperty("totalNanoAiu") Double totalNanoAiu,
    /** Session-wide per-token-type accumulated token counts */
    @JsonProperty("tokenDetails") Map<String, UsageMetricsTokenDetail> tokenDetails,
    /** Total time spent in model API calls (milliseconds) */
    @JsonProperty("totalApiDurationMs") Long totalApiDurationMs,
    /** ISO 8601 timestamp when the session started */
    @JsonProperty("sessionStartTime") OffsetDateTime sessionStartTime,
    /** Aggregated code change metrics */
    @JsonProperty("codeChanges") UsageMetricsCodeChanges codeChanges,
    /** Per-model token and request metrics, keyed by model identifier */
    @JsonProperty("modelMetrics") Map<String, UsageMetricsModelMetric> modelMetrics,
    /** Provider-attributed model metrics, ordered by first observed provider and model.
Null attribution preserves historical usage whose provider or model was not recorded. */
    @JsonProperty("providerModelMetrics") List<UsageMetricsProviderModelMetric> providerModelMetrics,
    /** Per-agent usage metrics, keyed by agent instance identifier. The main conversation uses the stable key `main`. */
    @JsonProperty("agentMetrics") Map<String, UsageMetricsAgentMetric> agentMetrics,
    /** Currently active model identifier */
    @JsonProperty("currentModel") String currentModel,
    /** Input tokens from the most recent main-agent API call */
    @JsonProperty("lastCallInputTokens") Long lastCallInputTokens,
    /** Output tokens from the most recent main-agent API call */
    @JsonProperty("lastCallOutputTokens") Long lastCallOutputTokens
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionUsageGetMetricsResult {}

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
        private Double totalPremiumRequestCost;
        private Long totalUserRequests;
        private Double totalNanoAiu;
        private Map<String, UsageMetricsTokenDetail> tokenDetails;
        private Long totalApiDurationMs;
        private OffsetDateTime sessionStartTime;
        private UsageMetricsCodeChanges codeChanges;
        private Map<String, UsageMetricsModelMetric> modelMetrics;
        private List<UsageMetricsProviderModelMetric> providerModelMetrics;
        private Map<String, UsageMetricsAgentMetric> agentMetrics;
        private String currentModel;
        private Long lastCallInputTokens;
        private Long lastCallOutputTokens;

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
         * Sets the {@code totalPremiumRequestCost} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder totalPremiumRequestCost(Double value) {
            this.totalPremiumRequestCost = value;
            return this;
        }

        /**
         * Sets the {@code totalUserRequests} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder totalUserRequests(Long value) {
            this.totalUserRequests = value;
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
        public Builder tokenDetails(Map<String, UsageMetricsTokenDetail> value) {
            this.tokenDetails = value;
            return this;
        }

        /**
         * Sets the {@code totalApiDurationMs} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder totalApiDurationMs(Long value) {
            this.totalApiDurationMs = value;
            return this;
        }

        /**
         * Sets the {@code sessionStartTime} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sessionStartTime(OffsetDateTime value) {
            this.sessionStartTime = value;
            return this;
        }

        /**
         * Sets the {@code codeChanges} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder codeChanges(UsageMetricsCodeChanges value) {
            this.codeChanges = value;
            return this;
        }

        /**
         * Sets the {@code modelMetrics} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder modelMetrics(Map<String, UsageMetricsModelMetric> value) {
            this.modelMetrics = value;
            return this;
        }

        /**
         * Sets the {@code providerModelMetrics} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder providerModelMetrics(List<UsageMetricsProviderModelMetric> value) {
            this.providerModelMetrics = value;
            return this;
        }

        /**
         * Sets the {@code agentMetrics} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder agentMetrics(Map<String, UsageMetricsAgentMetric> value) {
            this.agentMetrics = value;
            return this;
        }

        /**
         * Sets the {@code currentModel} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder currentModel(String value) {
            this.currentModel = value;
            return this;
        }

        /**
         * Sets the {@code lastCallInputTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder lastCallInputTokens(Long value) {
            this.lastCallInputTokens = value;
            return this;
        }

        /**
         * Sets the {@code lastCallOutputTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder lastCallOutputTokens(Long value) {
            this.lastCallOutputTokens = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionUsageGetMetricsResult build() {
            if (totalPremiumRequestCost == null) throw new IllegalStateException("Missing required property: totalPremiumRequestCost");
            if (totalUserRequests == null) throw new IllegalStateException("Missing required property: totalUserRequests");
            if (totalApiDurationMs == null) throw new IllegalStateException("Missing required property: totalApiDurationMs");
            if (sessionStartTime == null) throw new IllegalStateException("Missing required property: sessionStartTime");
            if (codeChanges == null) throw new IllegalStateException("Missing required property: codeChanges");
            if (modelMetrics == null) throw new IllegalStateException("Missing required property: modelMetrics");
            if (lastCallInputTokens == null) throw new IllegalStateException("Missing required property: lastCallInputTokens");
            if (lastCallOutputTokens == null) throw new IllegalStateException("Missing required property: lastCallOutputTokens");
            return new SessionUsageGetMetricsResult(aiCreditsStatus, totalPremiumRequestCost, totalUserRequests, totalNanoAiu, tokenDetails, totalApiDurationMs, sessionStartTime, codeChanges, modelMetrics, providerModelMetrics, agentMetrics, currentModel, lastCallInputTokens, lastCallOutputTokens);
        }
    }
}
