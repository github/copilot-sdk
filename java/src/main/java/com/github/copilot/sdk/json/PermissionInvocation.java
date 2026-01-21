/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

/**
 * Context information for a permission request invocation.
 * <p>
 * This object provides context about the session where the permission request
 * originated.
 *
 * @see PermissionHandler
 */
public final class PermissionInvocation {

    private String sessionId;

    /**
     * Gets the session ID where the permission was requested.
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
     * @return this invocation for method chaining
     */
    public PermissionInvocation setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }
}
