/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

package com.github.copilot.generated;

import javax.annotation.processing.Generated;

/**
 * Where the model input for a task-tool sub-agent came from.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum SubagentTaskModelSource {
    /** The {@code task_argument} variant. */
    TASK_ARGUMENT("task_argument"),
    /** The {@code subagent_configuration} variant. */
    SUBAGENT_CONFIGURATION("subagent_configuration"),
    /** The {@code custom_agent_definition} variant. */
    CUSTOM_AGENT_DEFINITION("custom_agent_definition"),
    /** The {@code unset} variant. */
    UNSET("unset");

    private final String value;
    SubagentTaskModelSource(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static SubagentTaskModelSource fromValue(String value) {
        for (SubagentTaskModelSource v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown SubagentTaskModelSource value: " + value);
    }
}
