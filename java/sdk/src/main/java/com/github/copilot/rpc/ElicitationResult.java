/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import java.util.Map;

import com.github.copilot.generated.rpc.PermissionDecisionContext;

/**
 * Result returned from an elicitation dialog.
 *
 * @since 1.0.0
 */
public class ElicitationResult {

    private ElicitationResultAction action;
    private Map<String, Object> content;
    private PermissionDecisionContext decisionContext;

    /**
     * Gets the user action taken on the elicitation dialog.
     * <p>
     * {@link ElicitationResultAction#ACCEPT} means the user submitted the form,
     * {@link ElicitationResultAction#DECLINE} means the user rejected the request,
     * and {@link ElicitationResultAction#CANCEL} means the user dismissed the
     * dialog.
     *
     * @return the user action
     */
    public ElicitationResultAction getAction() {
        return action;
    }

    /**
     * Sets the user action taken on the elicitation dialog.
     *
     * @param action
     *            the user action
     * @return this instance for method chaining
     */
    public ElicitationResult setAction(ElicitationResultAction action) {
        this.action = action;
        return this;
    }

    /**
     * Gets the form values submitted by the user.
     * <p>
     * Only present when {@link #getAction()} is
     * {@link ElicitationResultAction#ACCEPT}.
     *
     * @return the submitted form values, or {@code null} if the user did not accept
     */
    public Map<String, Object> getContent() {
        return content;
    }

    /**
     * Sets the form values submitted by the user.
     *
     * @param content
     *            the submitted form values
     * @return this instance for method chaining
     */
    public ElicitationResult setContent(Map<String, Object> content) {
        this.content = content;
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
     * Labels an {@link ElicitationHandler} answer with who answered and how.
     * <p>
     * When a person answered interactively, including when they declined or
     * dismissed the dialog, set a context with
     * {@link com.github.copilot.generated.rpc.PermissionDecisionSource#HUMAN_RESPONSE}
     * and
     * {@link com.github.copilot.generated.rpc.PermissionResponseCapability#INTERACTIVE}
     * so the runtime can count the answer as the person's own authority for
     * Assisted Permissions. The runtime decides which answers carry authority and
     * accepts the label only from the session's owning connection; other answers
     * still settle, without it. Leave automated answers unlabeled. The SDK sends
     * the context as {@code decisionContext} beside, never inside, the elicitation
     * {@code result}, and omits it when {@code null}.
     *
     * @param decisionContext
     *            the decision context, or {@code null} to send none
     * @return this instance for method chaining
     * @since 2.0.0
     */
    public ElicitationResult setDecisionContext(PermissionDecisionContext decisionContext) {
        this.decisionContext = decisionContext;
        return this;
    }
}
