/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * What the agent was doing when the user interrupted it.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum AgentInterruptedActivity {
    /** The {@code model_call} variant. */
    MODEL_CALL("model_call"),
    /** The {@code retry_backoff} variant. */
    RETRY_BACKOFF("retry_backoff"),
    /** The {@code tool_call} variant. */
    TOOL_CALL("tool_call"),
    /** The {@code background_agent} variant. */
    BACKGROUND_AGENT("background_agent");

    private final String value;
    AgentInterruptedActivity(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static AgentInterruptedActivity fromValue(String value) {
        for (AgentInterruptedActivity v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown AgentInterruptedActivity value: " + value);
    }
}
