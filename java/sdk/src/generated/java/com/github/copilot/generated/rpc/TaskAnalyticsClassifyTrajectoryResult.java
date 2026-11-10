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
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Categorical retrospective measurements of the supplied trajectory, not verification that its claims are true. Model-derived classifications can be inaccurate or manipulated by adversarial content. Treat them as untrusted measurements; independently validate results used for consequential decisions and never use them as authorization or security decisions.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskAnalyticsClassifyTrajectoryResult(
    /** Terminal classifier outcomes. Declined, failed, and abandoned outcomes are retained. No restricted rationale, raw judge output, or transcript text is returned. */
    @JsonProperty("classifications") List<TaskAnalyticsTrajectoryClassification> classifications,
    /** SHA-256 identity of the input archive and archive-adapter revision. Required when passing this result back for an exact retry; not an authentication or provenance guarantee. */
    @JsonProperty("trajectoryFingerprint") String trajectoryFingerprint,
    /** Fresh interaction ID for this analysis attempt, never an archived interaction ID. Reused outcomes retain their original per-row interaction ID. */
    @JsonProperty("interactionId") String interactionId,
    /** Local validation results. No inference runs when valid is false or validateOnly was requested. */
    @JsonProperty("validation") TaskAnalyticsTrajectoryValidation validation
) {

    /**
     * Creates a record positionally.
     * @deprecated Use {@link #builder()} for named construction.
     */
    @Deprecated(since = "2.0.0", forRemoval = false)
    public TaskAnalyticsClassifyTrajectoryResult {}

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
        private List<TaskAnalyticsTrajectoryClassification> classifications;
        private String trajectoryFingerprint;
        private String interactionId;
        private TaskAnalyticsTrajectoryValidation validation;

        /**
         * Sets the {@code classifications} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder classifications(List<TaskAnalyticsTrajectoryClassification> value) {
            this.classifications = value;
            return this;
        }

        /**
         * Sets the {@code trajectoryFingerprint} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder trajectoryFingerprint(String value) {
            this.trajectoryFingerprint = value;
            return this;
        }

        /**
         * Sets the {@code interactionId} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder interactionId(String value) {
            this.interactionId = value;
            return this;
        }

        /**
         * Sets the {@code validation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder validation(TaskAnalyticsTrajectoryValidation value) {
            this.validation = value;
            return this;
        }

        /**
         * Builds a record after checking required property presence.
         * @return a new record
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public TaskAnalyticsClassifyTrajectoryResult build() {
            if (classifications == null) throw new IllegalStateException("Missing required property: classifications");
            return new TaskAnalyticsClassifyTrajectoryResult(classifications, trajectoryFingerprint, interactionId, validation);
        }
    }
}
