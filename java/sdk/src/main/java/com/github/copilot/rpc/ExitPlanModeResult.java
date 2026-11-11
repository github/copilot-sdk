/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.generated.rpc.PermissionDecisionContext;

/**
 * Response to an exit-plan-mode request.
 *
 * @since 1.0.8
 */
public class ExitPlanModeResult {

    @JsonProperty("approved")
    private boolean approved = true;

    @JsonProperty("selectedAction")
    private String selectedAction;

    @JsonProperty("feedback")
    private String feedback;

    @JsonProperty("decisionContext")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private PermissionDecisionContext decisionContext;

    /**
     * Returns whether the user approved exiting plan mode.
     *
     * @return {@code true} if approved
     */
    public boolean isApproved() {
        return approved;
    }

    /**
     * Sets whether the user approved exiting plan mode.
     *
     * @param approved
     *            {@code true} if approved
     * @return this instance for method chaining
     */
    public ExitPlanModeResult setApproved(boolean approved) {
        this.approved = approved;
        return this;
    }

    /**
     * Gets the selected action, if the user chose one.
     *
     * @return the selected action, or {@code null}
     */
    public String getSelectedAction() {
        return selectedAction;
    }

    /**
     * Sets the selected action.
     *
     * @param selectedAction
     *            the selected action
     * @return this instance for method chaining
     */
    public ExitPlanModeResult setSelectedAction(String selectedAction) {
        this.selectedAction = selectedAction;
        return this;
    }

    /**
     * Gets optional feedback provided by the user.
     *
     * @return the feedback, or {@code null}
     */
    public String getFeedback() {
        return feedback;
    }

    /**
     * Sets feedback from the user.
     *
     * @param feedback
     *            the feedback text
     * @return this instance for method chaining
     */
    public ExitPlanModeResult setFeedback(String feedback) {
        this.feedback = feedback;
        return this;
    }

    /**
     * Gets the label describing who answered and how.
     *
     * @return the decision context, or {@code null} if the review is unlabeled
     * @since 2.0.0
     */
    public PermissionDecisionContext getDecisionContext() {
        return decisionContext;
    }

    /**
     * Labels this plan review with who answered and how.
     * <p>
     * When a person reviewed the plan interactively, set a context with
     * {@link com.github.copilot.generated.rpc.PermissionDecisionSource#HUMAN_RESPONSE}
     * and
     * {@link com.github.copilot.generated.rpc.PermissionResponseCapability#INTERACTIVE}
     * so the runtime can count the review as the person's own for Assisted
     * Permissions. The runtime decides which answers carry authority and accepts
     * the label only from the session's owning connection; other answers still
     * settle, without it. Leave automated answers unlabeled. The SDK sends the
     * context as {@code decisionContext} beside {@code approved} and omits it when
     * {@code null}.
     *
     * @param decisionContext
     *            the decision context, or {@code null} to send none
     * @return this instance for method chaining
     * @since 2.0.0
     */
    public ExitPlanModeResult setDecisionContext(PermissionDecisionContext decisionContext) {
        this.decisionContext = decisionContext;
        return this;
    }
}
