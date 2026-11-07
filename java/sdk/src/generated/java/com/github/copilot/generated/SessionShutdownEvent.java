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
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

/**
 * Session event "session.shutdown". Session termination metrics including usage statistics, code changes, and shutdown reason
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class SessionShutdownEvent extends SessionEvent {

    @Override
    public String getType() { return "session.shutdown"; }

    @JsonProperty("data")
    private SessionShutdownEventData data;

    public SessionShutdownEventData getData() { return data; }
    public void setData(SessionShutdownEventData data) { this.data = data; }

    /** Data payload for {@link SessionShutdownEvent}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SessionShutdownEventData(
        /** Runtime tool-operation identities already included in the final accounting snapshot. */
        @JsonProperty("toolUsageRequestIds") List<String> toolUsageRequestIds,
        /** Complete accounting state captured atomically with receipt watermarks. */
        @JsonProperty("accountingSnapshot") UsageGetMetricsResult accountingSnapshot,
        /** Provider/model accounting preserved for replay without consulting current provider selection. */
        @JsonProperty("providerModelMetrics") List<UsageMetricsProviderModelMetric> providerModelMetrics,
        /** Per-source accounting sequence watermarks subsuming preceding usage receipts. */
        @JsonProperty("usageAccountingWatermarks") Map<String, Long> usageAccountingWatermarks,
        /** Whether the session ended normally ("routine") or due to a crash/fatal error ("error") */
        @JsonProperty("shutdownType") ShutdownType shutdownType,
        /** Error description when shutdownType is "error" */
        @JsonProperty("errorReason") String errorReason,
        /** Total number of premium API requests used during the session */
        @JsonProperty("totalPremiumRequests") Double totalPremiumRequests,
        /** Session-wide accumulated nano-AI units cost */
        @CopilotExperimental
        @JsonProperty("totalNanoAiu") Double totalNanoAiu,
        /** Session-wide per-token-type accumulated token counts */
        @JsonProperty("tokenDetails") Map<String, ShutdownTokenDetail> tokenDetails,
        /** Cumulative time spent in API calls during the session, in milliseconds */
        @JsonProperty("totalApiDurationMs") Long totalApiDurationMs,
        /** Unix timestamp (milliseconds) when the session started */
        @JsonProperty("sessionStartTime") Long sessionStartTime,
        /** On-disk byte size of the session's persisted events.jsonl file at shutdown time; omitted when the file does not exist or cannot be stat'd */
        @JsonProperty("eventsFileSizeBytes") Long eventsFileSizeBytes,
        /** Aggregate code change metrics for the session */
        @JsonProperty("codeChanges") ShutdownCodeChanges codeChanges,
        /** Per-model usage breakdown, keyed by model identifier */
        @JsonProperty("modelMetrics") Map<String, ShutdownModelMetric> modelMetrics,
        /** Per-agent usage breakdown, keyed by agent instance identifier. The main conversation uses the stable key `main`. */
        @JsonProperty("agentMetrics") Map<String, ShutdownAgentMetric> agentMetrics,
        /** Model that was selected at the time of shutdown */
        @JsonProperty("currentModel") String currentModel,
        /** Total tokens in context window at shutdown */
        @JsonProperty("currentTokens") Long currentTokens,
        /** System message token count at shutdown */
        @JsonProperty("systemTokens") Long systemTokens,
        /** Non-system message token count at shutdown */
        @JsonProperty("conversationTokens") Long conversationTokens,
        /** Tool definitions token count at shutdown */
        @JsonProperty("toolDefinitionsTokens") Long toolDefinitionsTokens
    ) {

        /**
         * Creates a record positionally.
         * @deprecated Use {@link #builder()} for named construction.
         */
        @Deprecated(since = "2.0.0", forRemoval = false)
        public SessionShutdownEventData {}

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
            private UsageGetMetricsResult accountingSnapshot;
            private List<UsageMetricsProviderModelMetric> providerModelMetrics;
            private Map<String, Long> usageAccountingWatermarks;
            private ShutdownType shutdownType;
            private String errorReason;
            private Double totalPremiumRequests;
            private Double totalNanoAiu;
            private Map<String, ShutdownTokenDetail> tokenDetails;
            private Long totalApiDurationMs;
            private Long sessionStartTime;
            private Long eventsFileSizeBytes;
            private ShutdownCodeChanges codeChanges;
            private Map<String, ShutdownModelMetric> modelMetrics;
            private Map<String, ShutdownAgentMetric> agentMetrics;
            private String currentModel;
            private Long currentTokens;
            private Long systemTokens;
            private Long conversationTokens;
            private Long toolDefinitionsTokens;

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
             * Sets the {@code shutdownType} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder shutdownType(ShutdownType value) {
                this.shutdownType = value;
                return this;
            }

            /**
             * Sets the {@code errorReason} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder errorReason(String value) {
                this.errorReason = value;
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
            public Builder tokenDetails(Map<String, ShutdownTokenDetail> value) {
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
            public Builder sessionStartTime(Long value) {
                this.sessionStartTime = value;
                return this;
            }

            /**
             * Sets the {@code eventsFileSizeBytes} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder eventsFileSizeBytes(Long value) {
                this.eventsFileSizeBytes = value;
                return this;
            }

            /**
             * Sets the {@code codeChanges} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder codeChanges(ShutdownCodeChanges value) {
                this.codeChanges = value;
                return this;
            }

            /**
             * Sets the {@code modelMetrics} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder modelMetrics(Map<String, ShutdownModelMetric> value) {
                this.modelMetrics = value;
                return this;
            }

            /**
             * Sets the {@code agentMetrics} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder agentMetrics(Map<String, ShutdownAgentMetric> value) {
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
             * Sets the {@code currentTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder currentTokens(Long value) {
                this.currentTokens = value;
                return this;
            }

            /**
             * Sets the {@code systemTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder systemTokens(Long value) {
                this.systemTokens = value;
                return this;
            }

            /**
             * Sets the {@code conversationTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder conversationTokens(Long value) {
                this.conversationTokens = value;
                return this;
            }

            /**
             * Sets the {@code toolDefinitionsTokens} property.
             * @param value the property value
             * @return this builder
             * @since 2.0.0
             */
            public Builder toolDefinitionsTokens(Long value) {
                this.toolDefinitionsTokens = value;
                return this;
            }

            /**
             * Builds a record after checking required property presence.
             * @return a new record
             * @throws IllegalStateException if a required property is missing
             * @since 2.0.0
             */
            public SessionShutdownEventData build() {
                if (shutdownType == null) throw new IllegalStateException("Missing required property: shutdownType");
                if (totalApiDurationMs == null) throw new IllegalStateException("Missing required property: totalApiDurationMs");
                if (sessionStartTime == null) throw new IllegalStateException("Missing required property: sessionStartTime");
                if (codeChanges == null) throw new IllegalStateException("Missing required property: codeChanges");
                if (modelMetrics == null) throw new IllegalStateException("Missing required property: modelMetrics");
                return new SessionShutdownEventData(toolUsageRequestIds, accountingSnapshot, providerModelMetrics, usageAccountingWatermarks, shutdownType, errorReason, totalPremiumRequests, totalNanoAiu, tokenDetails, totalApiDurationMs, sessionStartTime, eventsFileSizeBytes, codeChanges, modelMetrics, agentMetrics, currentModel, currentTokens, systemTokens, conversationTokens, toolDefinitionsTokens);
            }
        }
    }
}
