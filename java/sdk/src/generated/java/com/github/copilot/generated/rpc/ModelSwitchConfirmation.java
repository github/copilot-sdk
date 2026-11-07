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

@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelSwitchConfirmation(
    /** Display name of the model that requires compaction confirmation. */
    @JsonProperty("targetModelDisplayName") String targetModelDisplayName,
    /** Current conversation token count before switching models. */
    @JsonProperty("currentTokens") Double currentTokens,
    /** Target model token limit used by the compaction preflight. */
    @JsonProperty("targetLimit") Double targetLimit,
    /** Effective prompt-token limit with the target model's long_context tier, present only when that tier can fit the conversation and managed policy permits it. Resubmit the switch with contextTier set to long_context to avoid compaction. */
    @JsonProperty("longContextLimit") Double longContextLimit
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ModelSwitchConfirmation {}

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
        private String targetModelDisplayName;
        private Double currentTokens;
        private Double targetLimit;
        private Double longContextLimit;

        /**
         * Sets the {@code targetModelDisplayName} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder targetModelDisplayName(String value) {
            this.targetModelDisplayName = value;
            return this;
        }

        /**
         * Sets the {@code currentTokens} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder currentTokens(Double value) {
            this.currentTokens = value;
            return this;
        }

        /**
         * Sets the {@code targetLimit} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder targetLimit(Double value) {
            this.targetLimit = value;
            return this;
        }

        /**
         * Sets the {@code longContextLimit} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder longContextLimit(Double value) {
            this.longContextLimit = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public ModelSwitchConfirmation build() {
            if (targetModelDisplayName == null) throw new IllegalStateException("Missing required property: targetModelDisplayName");
            if (currentTokens == null) throw new IllegalStateException("Missing required property: currentTokens");
            if (targetLimit == null) throw new IllegalStateException("Missing required property: targetLimit");
            return new ModelSwitchConfirmation(targetModelDisplayName, currentTokens, targetLimit, longContextLimit);
        }
    }

    /**
     * Retains the constructor published in Java SDK 1.0.28.
     * @deprecated Use named construction via {@link #builder()}.
     * @param targetModelDisplayName Display name of the model that requires compaction confirmation.
     * @param currentTokens Current conversation token count before switching models.
     * @param targetLimit Target model token limit used by the compaction preflight.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public ModelSwitchConfirmation(
        String targetModelDisplayName,
        Double currentTokens,
        Double targetLimit
    ) {
        this(targetModelDisplayName, currentTokens, targetLimit, null);
    }
}
