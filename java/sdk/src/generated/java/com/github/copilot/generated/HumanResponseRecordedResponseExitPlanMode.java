/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.annotation.processing.Generated;

/**
 * Variant {@code exit_plan_mode} of {@link HumanResponseRecordedResponse}.
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public final class HumanResponseRecordedResponseExitPlanMode extends HumanResponseRecordedResponse {

    @JsonProperty("responseKind")
    private final String responseKind = "exit_plan_mode";

    @Override
    public String getResponseKind() { return responseKind; }

    /** Exact plan summary displayed to the user. */
    @JsonProperty("summary")
    private String summary;

    /** Exact full plan content available from the review UI. */
    @JsonProperty("planContent")
    private String planContent;

    /** Actions offered by the plan review UI. */
    @JsonProperty("actions")
    private List<ExitPlanModeAction> actions;

    /** Action the plan review UI recommended. */
    @JsonProperty("recommendedAction")
    private ExitPlanModeAction recommendedAction;

    /** Whether the user approved the reviewed plan. */
    @JsonProperty("approved")
    private Boolean approved;

    /** Action selected by the user, when applicable. */
    @JsonProperty("selectedAction")
    private ExitPlanModeAction selectedAction;

    /** Whether the selected response requested edit auto-approval. */
    @JsonProperty("autoApproveEdits")
    private Boolean autoApproveEdits;

    /** Exact feedback submitted with the plan decision, when present. */
    @JsonProperty("feedback")
    private String feedback;

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getPlanContent() { return planContent; }
    public void setPlanContent(String planContent) { this.planContent = planContent; }

    public List<ExitPlanModeAction> getActions() { return actions; }
    public void setActions(List<ExitPlanModeAction> actions) { this.actions = actions; }

    public ExitPlanModeAction getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(ExitPlanModeAction recommendedAction) { this.recommendedAction = recommendedAction; }

    public Boolean getApproved() { return approved; }
    public void setApproved(Boolean approved) { this.approved = approved; }

    public ExitPlanModeAction getSelectedAction() { return selectedAction; }
    public void setSelectedAction(ExitPlanModeAction selectedAction) { this.selectedAction = selectedAction; }

    public Boolean getAutoApproveEdits() { return autoApproveEdits; }
    public void setAutoApproveEdits(Boolean autoApproveEdits) { this.autoApproveEdits = autoApproveEdits; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }


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
        private String summary;
        private String planContent;
        private List<ExitPlanModeAction> actions;
        private ExitPlanModeAction recommendedAction;
        private Boolean approved;
        private ExitPlanModeAction selectedAction;
        private Boolean autoApproveEdits;
        private String feedback;

        /**
         * Sets the {@code summary} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder summary(String value) {
            this.summary = value;
            return this;
        }

        /**
         * Sets the {@code planContent} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder planContent(String value) {
            this.planContent = value;
            return this;
        }

        /**
         * Sets the {@code actions} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder actions(List<ExitPlanModeAction> value) {
            this.actions = value;
            return this;
        }

        /**
         * Sets the {@code recommendedAction} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder recommendedAction(ExitPlanModeAction value) {
            this.recommendedAction = value;
            return this;
        }

        /**
         * Sets the {@code approved} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder approved(Boolean value) {
            this.approved = value;
            return this;
        }

        /**
         * Sets the {@code selectedAction} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder selectedAction(ExitPlanModeAction value) {
            this.selectedAction = value;
            return this;
        }

        /**
         * Sets the {@code autoApproveEdits} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder autoApproveEdits(Boolean value) {
            this.autoApproveEdits = value;
            return this;
        }

        /**
         * Sets the {@code feedback} property.
         * @param value the property value
         * @return this builder
         * @since 2.0.0
         */
        public Builder feedback(String value) {
            this.feedback = value;
            return this;
        }

        /**
         * Builds a variant after checking required property presence.
         * @return a new variant
         * @throws IllegalStateException if a required property is missing
         * @since 2.0.0
         */
        public HumanResponseRecordedResponseExitPlanMode build() {
            if (summary == null) throw new IllegalStateException("Missing required property: summary");
            if (planContent == null) throw new IllegalStateException("Missing required property: planContent");
            if (actions == null) throw new IllegalStateException("Missing required property: actions");
            if (recommendedAction == null) throw new IllegalStateException("Missing required property: recommendedAction");
            if (approved == null) throw new IllegalStateException("Missing required property: approved");
            HumanResponseRecordedResponseExitPlanMode instance = new HumanResponseRecordedResponseExitPlanMode();
            instance.setSummary(summary);
            instance.setPlanContent(planContent);
            instance.setActions(actions);
            instance.setRecommendedAction(recommendedAction);
            instance.setApproved(approved);
            instance.setSelectedAction(selectedAction);
            instance.setAutoApproveEdits(autoApproveEdits);
            instance.setFeedback(feedback);
            return instance;
        }
    }
}
