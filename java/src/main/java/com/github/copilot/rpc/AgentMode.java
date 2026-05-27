/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The UI mode the agent is in for a given turn.
 * <p>
 * Set on {@link MessageOptions#setAgentMode(AgentMode)} to send a message in a
 * specific mode; defaults to the session's current mode when unset.
 *
 * @see MessageOptions
 * @since 1.1.0
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

    /**
     * Deserializes a JSON string value into the corresponding {@code AgentMode}
     * enum constant.
     *
     * @param value
     *            the JSON string value
     * @return the matching {@code AgentMode}, or {@code null} if value is
     *         {@code null}
     * @throws IllegalArgumentException
     *             if the value does not match any known agent mode
     */
    @JsonCreator
    public static AgentMode fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (AgentMode mode : values()) {
            if (mode.value.equals(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown AgentMode: " + value);
    }
}
