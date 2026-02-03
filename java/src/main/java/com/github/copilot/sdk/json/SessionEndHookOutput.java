/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Output for a session-end hook.
 * <p>
 * Allows specifying cleanup actions and session summary.
 *
 * @since 1.0.7
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionEndHookOutput {

    @JsonProperty("suppressOutput")
    private Boolean suppressOutput;

    @JsonProperty("cleanupActions")
    private List<String> cleanupActions;

    @JsonProperty("sessionSummary")
    private String sessionSummary;

    /**
     * Gets whether output should be suppressed.
     *
     * @return {@code true} to suppress output, or {@code null}
     */
    public Boolean getSuppressOutput() {
        return suppressOutput;
    }

    /**
     * Sets whether to suppress the output.
     *
     * @param suppressOutput
     *            {@code true} to suppress output
     * @return this instance for method chaining
     */
    public SessionEndHookOutput setSuppressOutput(Boolean suppressOutput) {
        this.suppressOutput = suppressOutput;
        return this;
    }

    /**
     * Gets the list of cleanup actions.
     *
     * @return the cleanup actions, or {@code null}
     */
    public List<String> getCleanupActions() {
        return cleanupActions;
    }

    /**
     * Sets the cleanup actions to perform.
     *
     * @param cleanupActions
     *            the cleanup actions
     * @return this instance for method chaining
     */
    public SessionEndHookOutput setCleanupActions(List<String> cleanupActions) {
        this.cleanupActions = cleanupActions;
        return this;
    }

    /**
     * Gets the session summary.
     *
     * @return the session summary, or {@code null}
     */
    public String getSessionSummary() {
        return sessionSummary;
    }

    /**
     * Sets a summary of the session.
     *
     * @param sessionSummary
     *            the session summary
     * @return this instance for method chaining
     */
    public SessionEndHookOutput setSessionSummary(String sessionSummary) {
        this.sessionSummary = sessionSummary;
        return this;
    }
}
