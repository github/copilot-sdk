/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Session event "session.usage_checkpoint". Durable session usage checkpoint for reconstructing aggregate accounting on resume
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionUsageCheckpointEvent extends SessionEvent {

    @Override
    public String getType() { return "session.usage_checkpoint"; }

    @JsonProperty("data")
    private SessionUsageCheckpointEventData data;

    public SessionUsageCheckpointEventData getData() { return data; }
    public void setData(SessionUsageCheckpointEventData data) { this.data = data; }

    /** Data payload for {@link SessionUsageCheckpointEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionUsageCheckpointEventData(
        /** Runtime tool-operation identities already included in this checkpoint. Prevents duplicate charges when an operation is delivered again with a newer sequence. */
        @JsonProperty("toolUsageRequestIds") List<String> toolUsageRequestIds,
        /** Session-wide accumulated nano-AI units cost at checkpoint time */
        @JsonProperty("totalNanoAiu") Double totalNanoAiu,
        /** Complete accounting state captured atomically with the receipt watermarks. */
        @JsonProperty("accountingSnapshot") UsageGetMetricsResult accountingSnapshot,
        /** Provider/model accounting snapshot that subsumes preceding usage receipts. */
        @JsonProperty("providerModelMetrics") List<UsageMetricsProviderModelMetric> providerModelMetrics,
        /** Highest accounted sequence per source session; no per-call deduplication history is retained. */
        @JsonProperty("usageAccountingWatermarks") Map<String, Long> usageAccountingWatermarks,
        /** Total number of premium API requests used at checkpoint time */
        @JsonProperty("totalPremiumRequests") Double totalPremiumRequests,
        /** Internal per-model prompt-cache state used to restore expiration tracking on resume */
        @JsonProperty("modelCacheState") List<UsageCheckpointModelCacheState> modelCacheState,
        /** Internal per-conversation prompt-cache-break detector baselines restored on resume */
        @JsonProperty("promptCacheBreakState") List<Object> promptCacheBreakState
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionUsageCheckpointEventData {}

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
            private List<String> toolUsageRequestIds;
            private Double totalNanoAiu;
            private UsageGetMetricsResult accountingSnapshot;
            private List<UsageMetricsProviderModelMetric> providerModelMetrics;
            private Map<String, Long> usageAccountingWatermarks;
            private Double totalPremiumRequests;
            private List<UsageCheckpointModelCacheState> modelCacheState;
            private List<Object> promptCacheBreakState;

            /**
             * Sets the {@code toolUsageRequestIds} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolUsageRequestIds(List<String> value) {
                this.toolUsageRequestIds = value;
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
             * Sets the {@code accountingSnapshot} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder accountingSnapshot(UsageGetMetricsResult value) {
                this.accountingSnapshot = value;
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
             * Sets the {@code usageAccountingWatermarks} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder usageAccountingWatermarks(Map<String, Long> value) {
                this.usageAccountingWatermarks = value;
                return this;
            }

            /**
             * Sets the {@code totalPremiumRequests} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder totalPremiumRequests(Double value) {
                this.totalPremiumRequests = value;
                return this;
            }

            /**
             * Sets the {@code modelCacheState} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder modelCacheState(List<UsageCheckpointModelCacheState> value) {
                this.modelCacheState = value;
                return this;
            }

            /**
             * Sets the {@code promptCacheBreakState} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder promptCacheBreakState(List<Object> value) {
                this.promptCacheBreakState = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionUsageCheckpointEventData build() {
                if (totalNanoAiu == null) throw new IllegalStateException("Missing required property: totalNanoAiu");
                return new SessionUsageCheckpointEventData(toolUsageRequestIds, totalNanoAiu, accountingSnapshot, providerModelMetrics, usageAccountingWatermarks, totalPremiumRequests, modelCacheState, promptCacheBreakState);
            }
        }
    }
}
