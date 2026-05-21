/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

/**
 * Context for an exit-plan-mode request invocation.
 *
 * @since 1.0.8
 */
public class ExitPlanModeInvocation {

    private String sessionId;

    /**
     * Gets the session ID.
     *
     * @return the session ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets the session ID.
     *
     * @param sessionId
     *            the session ID
     * @return this instance for method chaining
     */
    public ExitPlanModeInvocation setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }
}
