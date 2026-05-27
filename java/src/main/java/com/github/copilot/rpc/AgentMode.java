/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The UI mode the agent is in for a given turn.
 * <p>
 * Set on {@link MessageOptions#setAgentMode(AgentMode)} to send a message in a
 * specific mode; defaults to the session's current mode when unset.
 *
 * @see MessageOptions
 * @since 1.0.0
 */
public enum AgentMode {

    /** The agent is responding interactively to the user. */
    INTERACTIVE("interactive"),

    /** The agent is preparing a plan before making changes. */
    PLAN("plan"),

    /** The agent is working autonomously toward task completion. */
    AUTOPILOT("autopilot"),

    /** The agent is in shell-focused UI mode. */
    SHELL("shell");

    private final String value;

    AgentMode(String value) {
        this.value = value;
    }

    /**
     * Returns the JSON value for this agent mode.
     *
     * @return the string value used in JSON serialization
     */
    @JsonValue
    public String getValue() {
        return value;
    }
}
