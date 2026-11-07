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
 * Redacted validation and memory-tool settings for a session.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionSettingsValidationSnapshot(
    /** General validation timeout budget in seconds. */
    @JsonProperty("timeout") Double timeout,
    /** Dependabot validation timeout budget in seconds. */
    @JsonProperty("dependabotTimeout") Double dependabotTimeout,
    /** Whether CodeQL validation is enabled. */
    @JsonProperty("codeqlEnabled") Boolean codeqlEnabled,
    /** Whether code-review validation is enabled. */
    @JsonProperty("codeReviewEnabled") Boolean codeReviewEnabled,
    /** Model used for code-review validation. */
    @JsonProperty("codeReviewModel") String codeReviewModel,
    /** Whether advisory validation is enabled. */
    @JsonProperty("advisoryEnabled") Boolean advisoryEnabled,
    /** Whether secret-scanning validation is enabled. */
    @JsonProperty("secretScanningEnabled") Boolean secretScanningEnabled,
    /** Whether the memory-store tool is enabled. */
    @JsonProperty("memoryStoreEnabled") Boolean memoryStoreEnabled,
    /** Whether the memory-vote tool is enabled. */
    @JsonProperty("memoryVoteEnabled") Boolean memoryVoteEnabled
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SessionSettingsValidationSnapshot {}

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
        private Double timeout;
        private Double dependabotTimeout;
        private Boolean codeqlEnabled;
        private Boolean codeReviewEnabled;
        private String codeReviewModel;
        private Boolean advisoryEnabled;
        private Boolean secretScanningEnabled;
        private Boolean memoryStoreEnabled;
        private Boolean memoryVoteEnabled;

        /**
         * Sets the {@code timeout} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder timeout(Double value) {
            this.timeout = value;
            return this;
        }

        /**
         * Sets the {@code dependabotTimeout} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder dependabotTimeout(Double value) {
            this.dependabotTimeout = value;
            return this;
        }

        /**
         * Sets the {@code codeqlEnabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder codeqlEnabled(Boolean value) {
            this.codeqlEnabled = value;
            return this;
        }

        /**
         * Sets the {@code codeReviewEnabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder codeReviewEnabled(Boolean value) {
            this.codeReviewEnabled = value;
            return this;
        }

        /**
         * Sets the {@code codeReviewModel} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder codeReviewModel(String value) {
            this.codeReviewModel = value;
            return this;
        }

        /**
         * Sets the {@code advisoryEnabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder advisoryEnabled(Boolean value) {
            this.advisoryEnabled = value;
            return this;
        }

        /**
         * Sets the {@code secretScanningEnabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder secretScanningEnabled(Boolean value) {
            this.secretScanningEnabled = value;
            return this;
        }

        /**
         * Sets the {@code memoryStoreEnabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder memoryStoreEnabled(Boolean value) {
            this.memoryStoreEnabled = value;
            return this;
        }

        /**
         * Sets the {@code memoryVoteEnabled} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder memoryVoteEnabled(Boolean value) {
            this.memoryVoteEnabled = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionSettingsValidationSnapshot build() {
            return new SessionSettingsValidationSnapshot(timeout, dependabotTimeout, codeqlEnabled, codeReviewEnabled, codeReviewModel, advisoryEnabled, secretScanningEnabled, memoryStoreEnabled, memoryVoteEnabled);
        }
    }
}
