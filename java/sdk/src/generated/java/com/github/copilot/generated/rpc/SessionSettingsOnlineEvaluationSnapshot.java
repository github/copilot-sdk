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
 * Online-evaluation settings safe to expose across the SDK boundary.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record SessionSettingsOnlineEvaluationSnapshot(
    /** Whether online evaluation is disabled. */
    @JsonProperty("disableOnlineEvaluation") Boolean disableOnlineEvaluation,
    /** Whether online-evaluation output-file generation is enabled. */
    @JsonProperty("enableOnlineEvaluationOutputFile") Boolean enableOnlineEvaluationOutputFile
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    SessionSettingsOnlineEvaluationSnapshot {}

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
        private Boolean disableOnlineEvaluation;
        private Boolean enableOnlineEvaluationOutputFile;

        /**
         * Sets the {@code disableOnlineEvaluation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder disableOnlineEvaluation(Boolean value) {
            this.disableOnlineEvaluation = value;
            return this;
        }

        /**
         * Sets the {@code enableOnlineEvaluationOutputFile} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder enableOnlineEvaluationOutputFile(Boolean value) {
            this.enableOnlineEvaluationOutputFile = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public SessionSettingsOnlineEvaluationSnapshot build() {
            return new SessionSettingsOnlineEvaluationSnapshot(disableOnlineEvaluation, enableOnlineEvaluationOutputFile);
        }
    }
}
