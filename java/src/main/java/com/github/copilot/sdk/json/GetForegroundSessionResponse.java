/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from session.getForeground RPC call.
 * <p>
 * This is only available when connecting to a server running in TUI+server mode
 * (--ui-server).
 *
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetForegroundSessionResponse {

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("workspacePath")
    private String workspacePath;

    /**
     * Gets the session ID currently displayed in the TUI.
     *
     * @return the session ID, or null if no foreground session
     */
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Gets the workspace path of the foreground session.
     *
     * @return the workspace path, or null
     */
    public String getWorkspacePath() {
        return workspacePath;
    }

    public void setWorkspacePath(String workspacePath) {
        this.workspacePath = workspacePath;
    }
}
