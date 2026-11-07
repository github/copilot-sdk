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
 * Usage for one recorded provider and model, without merging identical model IDs across providers.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record UsageMetricsProviderModelMetric(
    /** Provider identity and product label captured when the call was dispatched; null when unknown. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("provider") ModelProviderRef provider,
    /** Model identity, or null for legacy aggregate-only usage. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("modelId") String modelId,
    /** Model display name captured at call time, when known. */
    @JsonProperty("modelDisplayName") String modelDisplayName,
    /** Request, token, and cost totals for this provider/model. */
    @JsonProperty("metrics") UsageMetricsModelMetric metrics
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public UsageMetricsProviderModelMetric {}

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
        private ModelProviderRef provider;
        private boolean $presence0;
        private String modelId;
        private boolean $presence1;
        private String modelDisplayName;
        private UsageMetricsModelMetric metrics;

        /**
         * Sets the {@code provider} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder provider(ModelProviderRef value) {
            this.provider = value;
            this.$presence0 = true;
            return this;
        }

        /**
         * Sets the {@code modelId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder modelId(String value) {
            this.modelId = value;
            this.$presence1 = true;
            return this;
        }

        /**
         * Sets the {@code modelDisplayName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder modelDisplayName(String value) {
            this.modelDisplayName = value;
            return this;
        }

        /**
         * Sets the {@code metrics} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder metrics(UsageMetricsModelMetric value) {
            this.metrics = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public UsageMetricsProviderModelMetric build() {
            if (!$presence0) throw new IllegalStateException("Missing required property: provider");
            if (!$presence1) throw new IllegalStateException("Missing required property: modelId");
            if (metrics == null) throw new IllegalStateException("Missing required property: metrics");
            return new UsageMetricsProviderModelMetric(provider, modelId, modelDisplayName, metrics);
        }
    }
}
