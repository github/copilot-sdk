/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

/**
 * UI-specific capability flags for a session.
 *
 * @since 1.0.0
 */
public class SessionUiCapabilities {

    private Boolean elicitation;

    /**
     * Returns whether the host supports interactive elicitation dialogs.
     *
     * @return {@code true} if elicitation is supported, {@code false} or
     *         {@code null} otherwise
     */
    public Boolean getElicitation() {
        return elicitation;
    }

    /**
     * Sets whether the host supports interactive elicitation dialogs.
     *
     * @param elicitation
     *            {@code true} if elicitation is supported
     * @return this instance for method chaining
     */
    public SessionUiCapabilities setElicitation(Boolean elicitation) {
        this.elicitation = elicitation;
        return this;
    }
}
