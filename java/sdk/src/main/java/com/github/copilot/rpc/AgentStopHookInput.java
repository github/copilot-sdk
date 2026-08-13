/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Input for an agent-stop hook.
 *
 * @since 1.0.9
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentStopHookInput {

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("cwd")
    private String cwd;

    @JsonProperty("stopReason")
    private String stopReason;

    @JsonProperty("transcriptPath")
    private String transcriptPath;

    @JsonProperty("stop_hook_active")
    private Boolean stopHookActive;

    /**
     * Gets the runtime session ID of the session that triggered the hook.
     *
     * @return the session ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets the runtime session ID of the session that triggered the hook.
     *
     * @param sessionId
     *            the session ID
     * @return this instance for method chaining
     */
    public AgentStopHookInput setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    /**
     * Gets the timestamp of the hook invocation.
     *
     * @return the timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp of the hook invocation.
     *
     * @param timestamp
     *            the timestamp in milliseconds
     * @return this instance for method chaining
     */
    public AgentStopHookInput setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    /**
     * Gets the current working directory.
     *
     * @return the working directory path
     */
    public String getCwd() {
        return cwd;
    }

    /**
     * Sets the current working directory.
     *
     * @param cwd
     *            the working directory path
     * @return this instance for method chaining
     */
    public AgentStopHookInput setCwd(String cwd) {
        this.cwd = cwd;
        return this;
    }

    /**
     * Gets the reason the agent stopped.
     *
     * @return the stop reason
     */
    public String getStopReason() {
        return stopReason;
    }

    /**
     * Sets the reason the agent stopped.
     *
     * @param stopReason
     *            the stop reason
     * @return this instance for method chaining
     */
    public AgentStopHookInput setStopReason(String stopReason) {
        this.stopReason = stopReason;
        return this;
    }

    /**
     * Gets the path to the on-disk session transcript.
     *
     * @return the transcript path
     */
    public String getTranscriptPath() {
        return transcriptPath;
    }

    /**
     * Sets the path to the on-disk session transcript.
     *
     * @param transcriptPath
     *            the transcript path
     * @return this instance for method chaining
     */
    public AgentStopHookInput setTranscriptPath(String transcriptPath) {
        this.transcriptPath = transcriptPath;
        return this;
    }

    /**
     * Gets whether this stop follows a previous block decision.
     *
     * @return {@code true} when the stop hook is already active
     */
    public Boolean getStopHookActive() {
        return stopHookActive;
    }

    /**
     * Sets whether this stop follows a previous block decision.
     *
     * @param stopHookActive
     *            whether the stop hook is already active
     * @return this instance for method chaining
     */
    public AgentStopHookInput setStopHookActive(Boolean stopHookActive) {
        this.stopHookActive = stopHookActive;
        return this;
    }
}
