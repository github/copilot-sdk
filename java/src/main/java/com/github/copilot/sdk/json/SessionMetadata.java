/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Metadata about an existing Copilot session.
 * <p>
 * This class represents session information returned when listing available
 * sessions via {@link com.github.copilot.sdk.CopilotClient#listSessions()}. It
 * includes timing information, a summary of the conversation, and whether the
 * session is stored remotely.
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * var sessions = client.listSessions().get();
 * for (var meta : sessions) {
 * 	System.out.println("Session: " + meta.getSessionId());
 * 	System.out.println("  Started: " + meta.getStartTime());
 * 	System.out.println("  Summary: " + meta.getSummary());
 * }
 * }</pre>
 *
 * @see com.github.copilot.sdk.CopilotClient#listSessions()
 * @see com.github.copilot.sdk.CopilotClient#resumeSession(String,
 *      ResumeSessionConfig)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionMetadata {

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("startTime")
    private String startTime;

    @JsonProperty("modifiedTime")
    private String modifiedTime;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("isRemote")
    private boolean isRemote;

    /**
     * Gets the unique identifier for this session.
     *
     * @return the session ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets the session identifier.
     *
     * @param sessionId
     *            the session ID
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Gets the timestamp when the session was created.
     *
     * @return the start time as an ISO 8601 formatted string
     */
    public String getStartTime() {
        return startTime;
    }

    /**
     * Sets the session start time.
     *
     * @param startTime
     *            the start time as an ISO 8601 formatted string
     */
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    /**
     * Gets the timestamp when the session was last modified.
     *
     * @return the modified time as an ISO 8601 formatted string
     */
    public String getModifiedTime() {
        return modifiedTime;
    }

    /**
     * Sets the session modified time.
     *
     * @param modifiedTime
     *            the modified time as an ISO 8601 formatted string
     */
    public void setModifiedTime(String modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    /**
     * Gets a brief summary of the session's conversation.
     * <p>
     * This is typically an AI-generated summary of the session content.
     *
     * @return the session summary, or {@code null} if not available
     */
    public String getSummary() {
        return summary;
    }

    /**
     * Sets the session summary.
     *
     * @param summary
     *            the session summary
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /**
     * Returns whether this session is stored remotely.
     *
     * @return {@code true} if the session is stored on the server, {@code false} if
     *         it's stored locally
     */
    public boolean isRemote() {
        return isRemote;
    }

    /**
     * Sets whether this session is stored remotely.
     *
     * @param remote
     *            {@code true} if stored remotely
     */
    public void setRemote(boolean remote) {
        isRemote = remote;
    }
}
