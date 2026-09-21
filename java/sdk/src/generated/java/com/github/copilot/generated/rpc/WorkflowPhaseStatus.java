/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import javax.annotation.processing.Generated;

/**
 * Derived lifecycle state of a workflow phase.
 *
 * @apiNote This type is experimental and may change in a future version.
 *
 * @since 1.0.0
 */
@CopilotExperimental
@javax.annotation.processing.Generated("copilot-sdk-codegen")
public enum WorkflowPhaseStatus {
    /** The {@code pending} variant. */
    PENDING("pending"),
    /** The {@code active} variant. */
    ACTIVE("active"),
    /** The {@code completed} variant. */
    COMPLETED("completed"),
    /** The {@code skipped} variant. */
    SKIPPED("skipped");

    private final String value;
    WorkflowPhaseStatus(String value) { this.value = value; }
    @com.fasterxml.jackson.annotation.JsonValue
    public String getValue() { return value; }
    @com.fasterxml.jackson.annotation.JsonCreator
    public static WorkflowPhaseStatus fromValue(String value) {
        for (WorkflowPhaseStatus v : values()) {
            if (v.value.equals(value)) return v;
        }
        throw new IllegalArgumentException("Unknown WorkflowPhaseStatus value: " + value);
    }
}
