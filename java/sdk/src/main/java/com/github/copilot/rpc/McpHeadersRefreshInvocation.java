/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

/**
 * Context for a managed MCP dynamic-header refresh invocation.
 *
 * @since 1.0.0
 */
public class McpHeadersRefreshInvocation {

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
    public McpHeadersRefreshInvocation setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }
}
