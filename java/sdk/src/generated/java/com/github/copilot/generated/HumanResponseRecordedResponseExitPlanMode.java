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
}
