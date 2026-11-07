/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * The Assisted Permissions judge returned a usable verdict.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class AssistedApprovalAssessmentCompleted extends AssistedApprovalAssessment {

    @JsonProperty("status")
    private final String status = "completed";

    @Override
    public String getStatus() { return status; }

    /** Judge verdict for this request. */
    @JsonProperty("recommendation")
    private AssistedApprovalCompletedRecommendation recommendation;

    /** Judge-authored human-readable rationale. A bare ALLOW or DENY verdict legitimately leaves this absent. */
    @JsonProperty("rationale")
    private String rationale;

    public AssistedApprovalCompletedRecommendation getRecommendation() { return recommendation; }
    public void setRecommendation(AssistedApprovalCompletedRecommendation recommendation) { this.recommendation = recommendation; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }


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
        private AssistedApprovalCompletedRecommendation recommendation;
        private String rationale;

        /**
         * Sets the {@code recommendation} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder recommendation(AssistedApprovalCompletedRecommendation value) {
            this.recommendation = value;
            return this;
        }

        /**
         * Sets the {@code rationale} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        @com.github.copilot.CopilotExperimental
        public Builder rationale(String value) {
            this.rationale = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public AssistedApprovalAssessmentCompleted build() {
            if (recommendation == null) throw new IllegalStateException("Missing required property: recommendation");
            AssistedApprovalAssessmentCompleted instance = new AssistedApprovalAssessmentCompleted();
            instance.setRecommendation(recommendation);
            instance.setRationale(rationale);
            return instance;
        }
    }
}
