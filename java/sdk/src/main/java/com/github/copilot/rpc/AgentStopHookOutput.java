/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Output for an agent-stop hook.
 *
 * @since 1.0.9
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentStopHookOutput {

    @JsonProperty("decision")
    private String decision;

    @JsonProperty("reason")
    private String reason;

    /**
     * Gets the stop decision.
     *
     * @return {@code "block"} to keep the agent running, or {@code null}
     */
    public String getDecision() {
        return decision;
    }

    /**
     * Sets the stop decision.
     *
     * @param decision
     *            {@code "block"} to keep the agent running
     * @return this instance for method chaining
     */
    public AgentStopHookOutput setDecision(String decision) {
        this.decision = decision;
        return this;
    }

    /**
     * Gets the follow-up instruction supplied when the stop is blocked.
     *
     * @return the follow-up instruction
     */
    public String getReason() {
        return reason;
    }

    /**
     * Sets the follow-up instruction supplied when the stop is blocked.
     *
     * @param reason
     *            the follow-up instruction
     * @return this instance for method chaining
     */
    public AgentStopHookOutput setReason(String reason) {
        this.reason = reason;
        return this;
    }
}
