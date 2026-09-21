/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Action the runtime selected for a durable workflow pause checkpoint.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum WorkflowPauseCheckpointAction {
    /** The {@code continue} variant. */
    CONTINUE("continue"),
    /** The {@code pause} variant. */
    PAUSE("pause");

    private final String value;
    WorkflowPauseCheckpointAction(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static WorkflowPauseCheckpointAction fromValue(String value) {
        for (WorkflowPauseCheckpointAction v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown WorkflowPauseCheckpointAction value: " + value);
    }
}
