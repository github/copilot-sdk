/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.copilot.generated.rpc.PermissionDecisionContext;

/**
 * Response to a user input request.
 *
 * @since 1.0.6
 */
public class UserInputResponse {

    @JsonProperty("answer")
    private String answer;

    @JsonProperty("wasFreeform")
    private boolean wasFreeform;

    @JsonProperty("decisionContext")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private PermissionDecisionContext decisionContext;

    /**
     * Gets the user's answer.
     *
     * @return the answer text
     */
    public String getAnswer() {
        return answer;
    }

    /**
     * Sets the user's answer.
     *
     * @param answer
     *            the answer text
     * @return this instance for method chaining
     */
    public UserInputResponse setAnswer(String answer) {
        this.answer = answer;
        return this;
    }

    /**
     * Returns whether the answer was freeform (not from the provided choices).
     *
     * @return {@code true} if the answer was freeform
     */
    public boolean isWasFreeform() {
        return wasFreeform;
    }

    /**
     * Sets whether the answer was freeform.
     *
     * @param wasFreeform
     *            {@code true} if the answer was freeform
     * @return this instance for method chaining
     */
    public UserInputResponse setWasFreeform(boolean wasFreeform) {
        this.wasFreeform = wasFreeform;
        return this;
    }

    /**
     * Gets the label describing who answered and how.
     *
     * @return the decision context, or {@code null} if the answer is unlabeled
     * @since 2.0.0
     */
    public PermissionDecisionContext getDecisionContext() {
        return decisionContext;
    }

    /**
     * Labels this answer with who answered and how.
     * <p>
     * When a person answered interactively, set a context with
     * {@link com.github.copilot.generated.rpc.PermissionDecisionSource#HUMAN_RESPONSE}
     * and
     * {@link com.github.copilot.generated.rpc.PermissionResponseCapability#INTERACTIVE}
     * so the runtime can count the answer as the person's own authority for
     * Assisted Permissions. The runtime decides which answers carry authority and
     * accepts the label only from the session's owning connection; other answers
     * still settle, without it. Leave automated answers unlabeled. The SDK sends
     * the context as {@code decisionContext} beside {@code answer} and omits it
     * when {@code null}.
     *
     * @param decisionContext
     *            the decision context, or {@code null} to send none
     * @return this instance for method chaining
     * @since 2.0.0
     */
    public UserInputResponse setDecisionContext(PermissionDecisionContext decisionContext) {
        this.decisionContext = decisionContext;
        return this;
    }
}
