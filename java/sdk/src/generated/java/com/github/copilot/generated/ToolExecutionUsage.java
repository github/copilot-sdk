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
 * Authoritative usage reported by a separately metered runtime-owned tool operation. May be present on a failed tool when the service reported usage before local result processing failed. Missing token counts or billing mean unavailable, not zero.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolExecutionUsage(
    /** Runtime-generated identifier for this metered operation. Deduplicates repeated delivery or replay; not an upstream billing identifier. */
    @JsonProperty("requestId") String requestId,
    /** Requested model for the tool operation, independent of the conversation model. Not a provider-confirmed serving model. */
    @JsonProperty("model") String model,
    /** Provider responsible for the tool operation, captured independently of the conversation provider. */
    @JsonProperty("provider") ModelProviderRef provider,
    /** Runtime-owned accounting order, preserved across session checkpoints and replay. */
    @JsonProperty("accounting") UsageAccountingIdentity accounting,
    /** Total provider-reported input tokens across all modalities. */
    @JsonProperty("inputTokens") Long inputTokens,
    /** Total provider-reported output tokens across all modalities. */
    @JsonProperty("outputTokens") Long outputTokens,
    /** Provider-reported sum of input and output tokens. */
    @JsonProperty("totalTokens") Long totalTokens,
    /** Optional input modality breakdown; these counts are already included in inputTokens. */
    @JsonProperty("inputTokensDetails") ToolExecutionUsageTokenDetails inputTokensDetails,
    /** Optional output modality breakdown; these counts are already included in outputTokens. */
    @JsonProperty("outputTokensDetails") ToolExecutionUsageTokenDetails outputTokensDetails,
    /** Server-reported charge. Divide totalNanoAiu by 1,000,000,000 for AI credits; do not calculate customer cost from token prices. */
    @JsonProperty("copilotUsage") AssistantUsageCopilotUsage copilotUsage,
    /** Account quota snapshots, not per-operation cost deltas. */
    @JsonProperty("quotaSnapshots") Map<String, Object> quotaSnapshots
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ToolExecutionUsage {}

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
        private String requestId;
        private String model;
        private ModelProviderRef provider;
        private UsageAccountingIdentity accounting;
        private Long inputTokens;
        private Long outputTokens;
        private Long totalTokens;
        private ToolExecutionUsageTokenDetails inputTokensDetails;
        private ToolExecutionUsageTokenDetails outputTokensDetails;
        private AssistantUsageCopilotUsage copilotUsage;
        private Map<String, Object> quotaSnapshots;

        /**
         * Sets the {@code requestId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder requestId(String value) {
            this.requestId = value;
            return this;
        }

        /**
         * Sets the {@code model} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder model(String value) {
            this.model = value;
            return this;
        }

        /**
         * Sets the {@code provider} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder provider(ModelProviderRef value) {
            this.provider = value;
            return this;
        }

        /**
         * Sets the {@code accounting} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder accounting(UsageAccountingIdentity value) {
            this.accounting = value;
            return this;
        }

        /**
         * Sets the {@code inputTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder inputTokens(Long value) {
            this.inputTokens = value;
            return this;
        }

        /**
         * Sets the {@code outputTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder outputTokens(Long value) {
            this.outputTokens = value;
            return this;
        }

        /**
         * Sets the {@code totalTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder totalTokens(Long value) {
            this.totalTokens = value;
            return this;
        }

        /**
         * Sets the {@code inputTokensDetails} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder inputTokensDetails(ToolExecutionUsageTokenDetails value) {
            this.inputTokensDetails = value;
            return this;
        }

        /**
         * Sets the {@code outputTokensDetails} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder outputTokensDetails(ToolExecutionUsageTokenDetails value) {
            this.outputTokensDetails = value;
            return this;
        }

        /**
         * Sets the {@code copilotUsage} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder copilotUsage(AssistantUsageCopilotUsage value) {
            this.copilotUsage = value;
            return this;
        }

        /**
         * Sets the {@code quotaSnapshots} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder quotaSnapshots(Map<String, Object> value) {
            this.quotaSnapshots = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ToolExecutionUsage build() {
            if (requestId == null) throw new IllegalStateException("Missing required property: requestId");
            if (model == null) throw new IllegalStateException("Missing required property: model");
            return new ToolExecutionUsage(requestId, model, provider, accounting, inputTokens, outputTokens, totalTokens, inputTokensDetails, outputTokensDetails, copilotUsage, quotaSnapshots);
        }
    }
}
