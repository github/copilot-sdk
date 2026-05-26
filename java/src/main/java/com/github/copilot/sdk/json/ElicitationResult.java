/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.Map;

/**
 * Result returned from an elicitation dialog.
 *
 * @since 1.0.0
 */
public class ElicitationResult {

    private ElicitationResultAction action;
    private Map<String, Object> content;

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
}
