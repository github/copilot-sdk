/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Explainable AI-credit session-limit prediction.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionLimitPredictionDetails(
    /** Client population used for the prediction. */
    @JsonProperty("clientType") SessionLimitPredictionClientType clientType,
    /** Model identifier used for lookup. */
    @JsonProperty("modelId") String modelId,
    /** Baseline fallback level used to create the prediction. */
    @JsonProperty("source") SessionLimitPredictionSource source,
    /** Key matched at the source level, such as a model id, family id, or `global`. */
    @JsonProperty("sourceKey") String sourceKey,
    /** Resolved model family when known. */
    @JsonProperty("family") String family,
    /** Ordered usage tiers and their AI-credit caps. */
    @JsonProperty("tiers") List<SessionLimitPredictionTierOption> tiers,
    /** Baseline data provenance. */
    @JsonProperty("baselineData") SessionLimitPredictionBaselineData baselineData,
    /** Tier chosen as the recommended cap. */
    @JsonProperty("recommendedTier") SessionLimitPredictionTier recommendedTier,
    /** Recommended maximum AI credits for this session. */
    @JsonProperty("recommendedCap") Double recommendedCap
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public SessionLimitPredictionDetails {}

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
        private SessionLimitPredictionClientType clientType;
        private String modelId;
        private SessionLimitPredictionSource source;
        private String sourceKey;
        private String family;
        private List<SessionLimitPredictionTierOption> tiers;
        private SessionLimitPredictionBaselineData baselineData;
        private SessionLimitPredictionTier recommendedTier;
        private Double recommendedCap;

        /**
         * Sets the {@code clientType} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder clientType(SessionLimitPredictionClientType value) {
            this.clientType = value;
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
            return this;
        }

        /**
         * Sets the {@code source} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder source(SessionLimitPredictionSource value) {
            this.source = value;
            return this;
        }

        /**
         * Sets the {@code sourceKey} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder sourceKey(String value) {
            this.sourceKey = value;
            return this;
        }

        /**
         * Sets the {@code family} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder family(String value) {
            this.family = value;
            return this;
        }

        /**
         * Sets the {@code tiers} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder tiers(List<SessionLimitPredictionTierOption> value) {
            this.tiers = value;
            return this;
        }

        /**
         * Sets the {@code baselineData} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder baselineData(SessionLimitPredictionBaselineData value) {
            this.baselineData = value;
            return this;
        }

        /**
         * Sets the {@code recommendedTier} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder recommendedTier(SessionLimitPredictionTier value) {
            this.recommendedTier = value;
            return this;
        }

        /**
         * Sets the {@code recommendedCap} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder recommendedCap(Double value) {
            this.recommendedCap = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionLimitPredictionDetails build() {
            if (clientType == null) throw new IllegalStateException("Missing required property: clientType");
            if (modelId == null) throw new IllegalStateException("Missing required property: modelId");
            if (source == null) throw new IllegalStateException("Missing required property: source");
            if (sourceKey == null) throw new IllegalStateException("Missing required property: sourceKey");
            if (tiers == null) throw new IllegalStateException("Missing required property: tiers");
            if (baselineData == null) throw new IllegalStateException("Missing required property: baselineData");
            if (recommendedTier == null) throw new IllegalStateException("Missing required property: recommendedTier");
            if (recommendedCap == null) throw new IllegalStateException("Missing required property: recommendedCap");
            return new SessionLimitPredictionDetails(clientType, modelId, source, sourceKey, family, tiers, baselineData, recommendedTier, recommendedCap);
        }
    }
}
