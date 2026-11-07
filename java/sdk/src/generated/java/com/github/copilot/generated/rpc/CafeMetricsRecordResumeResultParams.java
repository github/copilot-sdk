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
import javax.annotation.processing.Generated;

/**
 * Terminal result of one resume attempt.
 *
 * @apiNote This method is experimental and may change in a future version.
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
record CafeMetricsRecordResumeResultParams(
    /** Last phase reached. */
    @JsonProperty("stage") CafeMetricsResumeStage stage,
    /** Terminal outcome. */
    @JsonProperty("outcome") CafeMetricsResumeOutcome outcome,
    /** Bounded result classification. */
    @JsonProperty("reason") CafeMetricsResumeReason reason,
    /** Whether an event-log recovery projection was used. */
    @JsonProperty("recovered") Boolean recovered
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    CafeMetricsRecordResumeResultParams {}

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
        private CafeMetricsResumeStage stage;
        private CafeMetricsResumeOutcome outcome;
        private CafeMetricsResumeReason reason;
        private Boolean recovered;

        /**
         * Sets the {@code stage} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder stage(CafeMetricsResumeStage value) {
            this.stage = value;
            return this;
        }

        /**
         * Sets the {@code outcome} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder outcome(CafeMetricsResumeOutcome value) {
            this.outcome = value;
            return this;
        }

        /**
         * Sets the {@code reason} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder reason(CafeMetricsResumeReason value) {
            this.reason = value;
            return this;
        }

        /**
         * Sets the {@code recovered} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder recovered(Boolean value) {
            this.recovered = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public CafeMetricsRecordResumeResultParams build() {
            if (stage == null) throw new IllegalStateException("Missing required property: stage");
            if (outcome == null) throw new IllegalStateException("Missing required property: outcome");
            if (reason == null) throw new IllegalStateException("Missing required property: reason");
            if (recovered == null) throw new IllegalStateException("Missing required property: recovered");
            return new CafeMetricsRecordResumeResultParams(stage, outcome, reason, recovered);
        }
    }
}
