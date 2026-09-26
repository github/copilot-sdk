/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Supported observed occurrences. Chronological parentId is not a causal reference.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum WorkerEventType {
    /** The {@code tool.execution_start} variant. */
    TOOL_EXECUTION_START("tool.execution_start"),
    /** The {@code user.message} variant. */
    USER_MESSAGE("user.message"),
    /** The {@code subagent.completed} variant. */
    SUBAGENT_COMPLETED("subagent.completed"),
    /** The {@code system.notification} variant. */
    SYSTEM_NOTIFICATION("system.notification"),
    /** The {@code assistant.turn_start} variant. */
    ASSISTANT_TURN_START("assistant.turn_start");

    private final String value;
    WorkerEventType(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static WorkerEventType fromValue(String value) {
        for (WorkerEventType v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown WorkerEventType value: " + value);
    }
}
