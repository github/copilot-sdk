/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

/**
 * Context for a user input request invocation.
 *
 * @since 1.0.6
 */
public class UserInputInvocation {

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
    public UserInputInvocation setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }
}
