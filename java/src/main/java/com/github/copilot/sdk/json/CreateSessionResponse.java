package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class CreateSessionResponse {
    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("workspacePath")
    private String workspacePath;

    public String getSessionId() {
        return sessionId;
    }
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Gets the workspace path when infinite sessions are enabled.
     *
     * @return the workspace path, or {@code null} if infinite sessions are disabled
     */
    public String getWorkspacePath() {
        return workspacePath;
    }
    public void setWorkspacePath(String workspacePath) {
        this.workspacePath = workspacePath;
    }
}
