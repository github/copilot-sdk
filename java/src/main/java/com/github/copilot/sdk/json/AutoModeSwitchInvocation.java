/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

/**
 * Context for an auto-mode-switch request invocation.
 *
 * @since 1.0.8
 */
public class AutoModeSwitchInvocation {

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
    public AutoModeSwitchInvocation setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }
}
