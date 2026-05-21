/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonProperty;

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
}
