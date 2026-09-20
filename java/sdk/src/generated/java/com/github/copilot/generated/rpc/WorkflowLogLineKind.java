/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import javax.annotation.processing.Generated;

/**
 * Kind of workflow progress line.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum WorkflowLogLineKind {
    /** The {@code log} variant. */
    LOG("log"),
    /** The {@code phase} variant. */
    PHASE("phase");

    private final String value;
    WorkflowLogLineKind(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static WorkflowLogLineKind fromValue(String value) {
        for (WorkflowLogLineKind v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown WorkflowLogLineKind value: " + value);
    }
}
